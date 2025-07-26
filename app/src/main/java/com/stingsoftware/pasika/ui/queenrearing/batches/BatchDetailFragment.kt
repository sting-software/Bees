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

        val cellAdapter = QueenCellAdapter { cell, cellNumber ->
            showStatusUpdateDialog(cell, cellNumber)
        }

        binding.recyclerViewQueenCells.apply {
            adapter = cellAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val batchLiveData = viewModel.getBatch(args.batchId)
        val cellsLiveData = viewModel.getQueenCells(args.batchId)

        batchLiveData.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                binding.batchName.text = it.name
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                binding.graftingDate.text =
                    getString(R.string.grafted_on, sdf.format(Date(it.graftingDate)))
                viewModel.fetchMotherHive(it.motherHiveId)

                cellsLiveData.value?.let { cells ->
                    updateCellStats(cells, it)
                }
            }
        }

        viewModel.motherHive.observe(viewLifecycleOwner) { hive ->
            hive?.let {
                binding.motherColony.text =
                    getString(R.string.mother, it.hiveNumber ?: "ID: ${it.id}")
            }
        }

        cellsLiveData.observe(viewLifecycleOwner) { cells ->
            cellAdapter.submitList(cells)
            batchLiveData.value?.let { batch ->
                updateCellStats(cells, batch)
            }
        }
    }

    private fun updateCellStats(cells: List<QueenCell>, batch: GraftingBatch) {
        val acceptedCount = cells.count { it.status != QueenCellStatus.GRAFTED && it.status != QueenCellStatus.FAILED }
        binding.cellStats.text = getString(R.string.accepted, acceptedCount, batch.cellsGrafted)
    }

    private fun showStatusUpdateDialog(cell: QueenCell, cellNumber: Int) {
        val statuses = QueenCellStatus.entries.map { it.getLabel(requireContext()) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.update_cell_status, cellNumber))
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
