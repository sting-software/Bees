package com.stingsoftware.pasika.ui.queenrearing.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stingsoftware.pasika.R
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val cellAdapter = QueenCellAdapter { cell ->
            showStatusUpdateDialog(cell)
        }

        binding.recyclerViewQueenCells.apply {
            adapter = cellAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // First, observe the batch details
        viewModel.getBatch(args.batchId).observe(viewLifecycleOwner) { batch ->
            batch?.let {
                binding.batchName.text = it.name
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                binding.graftingDate.text =
                    getString(R.string.grafted_on, sdf.format(Date(it.graftingDate)))
                // Once we have the batch, trigger the fetch for the mother hive
                viewModel.fetchMotherHive(it.motherHiveId)
            }
        }

        // Second, observe the motherHive LiveData property that will be updated by the fetch
        viewModel.motherHive.observe(viewLifecycleOwner) { hive ->
            hive?.let {
                binding.motherColony.text =
                    getString(R.string.mother, it.hiveNumber ?: "ID: ${it.id}")
            }
        }

        viewModel.getQueenCells(args.batchId).observe(viewLifecycleOwner) { cells ->
            cellAdapter.submitList(cells)
            val acceptedCount = cells.count { it.status != QueenCellStatus.GRAFTED && it.status != QueenCellStatus.FAILED }
            binding.cellStats.text = getString(R.string.accepted, acceptedCount, cells.size)
        }
    }

    private fun showStatusUpdateDialog(cell: QueenCell) {
        val statuses = QueenCellStatus.entries.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.update_cell_status, cell.id))
            .setItems(statuses) { dialog, which ->
                val newStatus = QueenCellStatus.entries[which]
                val updatedCell = cell.copy(status = newStatus)
                viewModel.updateQueenCell(updatedCell)
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
