package com.stingsoftware.pasika.ui.queenrearing.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.data.QueenCellStatus
import com.stingsoftware.pasika.databinding.FragmentAnalyticsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.batchChart.setNoDataText(getString(R.string.chart_no_data))

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.acceptanceRateText.text =
                String.format(
                    Locale.US,
                    getString(R.string.acceptance_rate_1f), stats.acceptanceRate
                )
            binding.emergenceRateText.text =
                String.format(Locale.US, getString(R.string.emergence_rate_1f), stats.emergenceRate)
            binding.matingSuccessText.text =
                String.format(
                    Locale.US,
                    getString(R.string.mating_success_1f), stats.matingSuccessRate
                )
        }

        viewModel.batches.observe(viewLifecycleOwner) { batches ->
            viewModel.allCells.observe(viewLifecycleOwner) { cells ->
                if (batches.isNotEmpty() && cells.isNotEmpty()) {
                    setupBarChart(batches, cells)
                } else {
                    binding.batchChart.clear()
                    binding.batchChart.invalidate()
                }
            }
        }
    }

    private fun setupBarChart(batches: List<GraftingBatch>, allCells: List<QueenCell>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        batches.forEachIndexed { index, batch ->
            val batchCells = allCells.filter { it.batchId == batch.id }
            val total = batch.cellsGrafted
            val accepted =
                batchCells.count { it.status >= QueenCellStatus.ACCEPTED && it.status != QueenCellStatus.FAILED }
            val acceptanceRate = if (total > 0) (accepted.toFloat() / total.toFloat()) * 100 else 0f
            entries.add(BarEntry(index.toFloat(), acceptanceRate))
            labels.add(batch.name)
        }

        val dataSet = BarDataSet(entries, getString(R.string.acceptance_rate))
        dataSet.color = Color.parseColor("#FFC107")

        binding.batchChart.apply {
            data = barData
            description.isEnabled = false
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
