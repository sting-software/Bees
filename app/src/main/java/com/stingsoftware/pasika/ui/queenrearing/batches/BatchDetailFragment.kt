package com.stingsoftware.pasika.ui.queenrearing.batches

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.data.QueenCellStatus
import com.stingsoftware.pasika.databinding.FragmentBatchDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BatchDetailFragment : Fragment() {

    private var _binding: FragmentBatchDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BatchDetailViewModel by viewModels()
    private val args: BatchDetailFragmentArgs by navArgs()
    private lateinit var cellAdapter: QueenCellAdapter
    private var isMultiSelectMode = false

    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeData()
        setupMenu()
        setupBackPressHandler()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = getString(R.string.batch_details)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        cellAdapter = QueenCellAdapter(
            onItemClick = { cell, cellNumber ->
                if (isMultiSelectMode) {
                    cellAdapter.toggleSelection(cell)
                } else {
                    showStatusUpdateDialog(listOf(cell), cellNumber)
                }
            },
            onLongClick = { cell ->
                if (!isMultiSelectMode) {
                    isMultiSelectMode = true
                    backPressedCallback.isEnabled = true
                    activity?.invalidateOptionsMenu()
                    cellAdapter.setMultiSelectMode(true)
                    cellAdapter.toggleSelection(cell)
                }
            },
            onSelectionChange = { count ->
                if (isMultiSelectMode) {
                    if (count == 0) {
                        finishMultiSelectMode()
                    } else {
                        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.selected_count_format, count)
                    }
                }
            }
        )

        binding.recyclerViewQueenCells.apply {
            adapter = cellAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        val batchLiveData = viewModel.getBatch(args.batchId)
        val cellsLiveData = viewModel.getQueenCells(args.batchId)

        batchLiveData.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                if (!isMultiSelectMode) {
                    binding.batchName.text = it.name
                }
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                binding.graftingDate.text =
                    getString(R.string.grafted_on, sdf.format(Date(it.graftingDate)))
                viewModel.fetchMotherHive(it.motherHiveId)
                cellsLiveData.value?.let { cells -> updateCellStats(cells, it) }
            }
        }

        viewModel.motherHive.observe(viewLifecycleOwner) { hive ->
            hive?.let {
                val motherIdentifier = it.hiveNumber ?: "ID: ${it.id}"
                binding.motherColony.text = getString(R.string.mother, motherIdentifier)
            }
        }

        cellsLiveData.observe(viewLifecycleOwner) { cells ->
            cellAdapter.submitList(cells)
            batchLiveData.value?.let { batch -> updateCellStats(cells, batch) }
        }
    }

    private fun updateCellStats(cells: List<QueenCell>, batch: GraftingBatch) {
        val acceptedCount = cells.count { it.status != QueenCellStatus.GRAFTED && it.status != QueenCellStatus.FAILED }
        binding.cellStats.text = getString(R.string.accepted, acceptedCount, batch.cellsGrafted)
    }

    private fun showStatusUpdateDialog(cellsToUpdate: List<QueenCell>, cellNumber: Int) {
        val title = if (cellsToUpdate.size == 1) {
            getString(R.string.update_cell_status, cellNumber)
        } else {
            val count = cellsToUpdate.size
            resources.getQuantityString(R.plurals.update_status_for_cells_title, count, count)
        }

        val statuses = QueenCellStatus.values().map { it.getLabel(requireContext()) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(statuses) { dialog, which ->
                val newStatus = QueenCellStatus.values()[which]
                val updatedCells = cellsToUpdate.map { it.copy(status = newStatus) }
                viewModel.updateQueenCells(updatedCells)
                dialog.dismiss()
                finishMultiSelectMode()
            }
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        val selectedItems = cellAdapter.getSelectedItems()
        val count = selectedItems.size
        if (count == 0) return

        val message = resources.getQuantityString(R.plurals.dialog_message_delete_selected_cells, count, count)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteQueenCells(selectedItems)
                finishMultiSelectMode()
            }
            .show()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if (isMultiSelectMode) {
                    menuInflater.inflate(R.menu.menu_cell, menu)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == android.R.id.home) {
                    if (isMultiSelectMode) {
                        finishMultiSelectMode()
                    } else {
                        findNavController().popBackStack()
                    }
                    return true
                }

                if (isMultiSelectMode) {
                    return when (menuItem.itemId) {
                        R.id.action_delete -> {
                            showDeleteConfirmationDialog()
                            true
                        }
                        R.id.action_change_status -> {
                            showStatusUpdateDialog(cellAdapter.getSelectedItems(), -1)
                            true
                        }
                        R.id.action_select_all -> {
                            cellAdapter.selectAll()
                            true
                        }
                        else -> false
                    }
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupBackPressHandler() {
        backPressedCallback = object : OnBackPressedCallback(false) { // Initially disabled
            override fun handleOnBackPressed() {
                finishMultiSelectMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun finishMultiSelectMode() {
        if (!isMultiSelectMode) return
        isMultiSelectMode = false
        backPressedCallback.isEnabled = false
        cellAdapter.clearSelections()
        cellAdapter.setMultiSelectMode(false)
        setupToolbar()
        activity?.invalidateOptionsMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
