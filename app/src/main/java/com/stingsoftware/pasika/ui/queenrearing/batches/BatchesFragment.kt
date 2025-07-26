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
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragmentDirections
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BatchesFragment : Fragment() {

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
                    batchesAdapter.setMultiSelectMode(false)
                    activity?.invalidateOptionsMenu()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)
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
                    batchesAdapter.setMultiSelectMode(true)
                    activity?.invalidateOptionsMenu()
                    batchesAdapter.toggleSelection(batch)
                }
            },
            onSelectionChange = { count ->
                if (count == 0 && batchesAdapter.isMultiSelectMode()) {
                    batchesAdapter.setMultiSelectMode(false)
                }
                backPressedCallback?.isEnabled = batchesAdapter.isMultiSelectMode()
                activity?.invalidateOptionsMenu()
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
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.selected_count_format, batchesAdapter.getSelectedItems().size)
        } else {
            menu.clear()
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.queen_rearing)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
            android.R.id.home -> {
                if (batchesAdapter.isMultiSelectMode()) {
                    batchesAdapter.setMultiSelectMode(false)
                    activity?.invalidateOptionsMenu()
                    return true
                }
                false
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
                batchesAdapter.setMultiSelectMode(false)
                activity?.invalidateOptionsMenu()
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (batchesAdapter.isMultiSelectMode()) {
            batchesAdapter.setMultiSelectMode(false)
            activity?.invalidateOptionsMenu()
        }
        _binding = null
    }
}