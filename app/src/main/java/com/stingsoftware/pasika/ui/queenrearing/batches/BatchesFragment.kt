package com.stingsoftware.pasika.ui.queenrearing.batches

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.databinding.FragmentBatchesListBinding
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragment
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragmentDirections
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import com.stingsoftware.pasika.ui.queenrearing.SearchableFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BatchesFragment : Fragment(), SearchableFragment {

    private var _binding: FragmentBatchesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QueenRearingViewModel by viewModels({ requireParentFragment() })
    private lateinit var batchesAdapter: GraftingBatchAdapter
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchesListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeBatches()
        setupOnBackPressed()
    }

    private fun setupOnBackPressed() {
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (batchesAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(false)
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)
    }

    private fun setMultiSelectMode(enabled: Boolean) {
        batchesAdapter.setMultiSelectMode(enabled)
        backPressedCallback?.isEnabled = enabled
        activity?.invalidateOptionsMenu()
    }

    private fun setupRecyclerView() {
        batchesAdapter = GraftingBatchAdapter(
            onItemClick = { batch ->
                if (batchesAdapter.isMultiSelectMode()) {
                    batchesAdapter.toggleSelection(batch)
                } else {
                    val action = QueenRearingFragmentDirections.actionQueenRearingFragmentToBatchDetailFragment(batch.id)
                    findNavController().navigate(action)
                }
            },
            onEditClick = { batch ->
                val action = QueenRearingFragmentDirections.actionQueenRearingFragmentToAddEditGraftingBatchFragment(batch.id)
                findNavController().navigate(action)
            },
            onLongClick = { batch ->
                if (!batchesAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(true)
                    batchesAdapter.toggleSelection(batch)
                }
            },
            onSelectionChange = { count ->
                if (count == 0 && batchesAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(false)
                } else {
                    activity?.invalidateOptionsMenu()
                }
            }
        )

        binding.recyclerViewBatches.apply {
            adapter = batchesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeBatches() {
        viewModel.graftingBatches.observe(viewLifecycleOwner) { batches ->
            if (batches.isNullOrEmpty()) {
                binding.recyclerViewBatches.visibility = View.GONE
                binding.emptyState.root.visibility = View.VISIBLE
                binding.emptyState.textViewEmptyMessage.text =
                    getString(R.string.no_batches_created)
            } else {
                binding.recyclerViewBatches.visibility = View.VISIBLE
                binding.emptyState.root.visibility = View.GONE
                batchesAdapter.submitList(batches)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (batchesAdapter.isMultiSelectMode()) {
            inflater.inflate(R.menu.menu_batches, menu)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val isMultiSelect = batchesAdapter.isMultiSelectMode()
        val parentMenu = (parentFragment as? QueenRearingFragment)?.view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.menu
        parentMenu?.findItem(R.id.action_search)?.isVisible = !isMultiSelect

        if (isMultiSelect) {
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.selected_count_format, batchesAdapter.getSelectedItems().size)
        } else {
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.queen_rearing)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (batchesAdapter.isMultiSelectMode()) {
                setMultiSelectMode(false)
                return true
            }
            return false
        }

        if (!batchesAdapter.isMultiSelectMode()) {
            return super.onOptionsItemSelected(item)
        }

        val selectedBatches = batchesAdapter.getSelectedItems()
        return when (item.itemId) {
            R.id.action_delete_selected -> {
                showDeleteConfirmationDialog(selectedBatches)
                true
            }
            R.id.action_select_all -> {
                batchesAdapter.selectAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog(batches: List<GraftingBatch>) {
        val quantity = batches.size
        val title = resources.getQuantityString(
            R.plurals.dialog_title_delete_selected_batches,
            quantity,
            quantity
        )
        val message = resources.getQuantityString(
            R.plurals.dialog_message_delete_selected_batches,
            quantity,
            quantity
        )

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                viewModel.deleteGraftingBatches(batches)
                setMultiSelectMode(false)
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    override fun search(query: String?) {
        viewModel.setSearchQuery(query)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
