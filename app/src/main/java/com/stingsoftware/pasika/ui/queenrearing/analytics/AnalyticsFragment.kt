package com.stingsoftware.pasika.ui.queenrearing.analytics

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
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
        val acceptanceEntries = ArrayList<BarEntry>()
        val emergenceEntries = ArrayList<BarEntry>()
        val matingSuccessEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        batches.forEachIndexed { index, batch ->
            val batchCells = allCells.filter { it.batchId == batch.id }
            labels.add(batch.name)

            val totalGrafted = batch.cellsGrafted
            val accepted =
                batchCells.count { it.status >= QueenCellStatus.ACCEPTED && it.status != QueenCellStatus.FAILED }
            val emerged =
                batchCells.count { it.status >= QueenCellStatus.EMERGED && it.status != QueenCellStatus.FAILED }
            val layingOrSold =
                batchCells.count { it.status == QueenCellStatus.LAYING || it.status == QueenCellStatus.SOLD }

            val acceptanceRate =
                if (totalGrafted > 0) (accepted.toFloat() / totalGrafted.toFloat()) * 100 else 0f
            val emergenceRate =
                if (accepted > 0) (emerged.toFloat() / accepted.toFloat()) * 100 else 0f
            val matingSuccessRate =
                if (emerged > 0) (layingOrSold.toFloat() / emerged.toFloat()) * 100 else 0f

            val xValue = index.toFloat()
            acceptanceEntries.add(BarEntry(xValue, acceptanceRate))
            emergenceEntries.add(BarEntry(xValue, emergenceRate))
            matingSuccessEntries.add(BarEntry(xValue, matingSuccessRate))
        }

        val acceptanceDataSet =
            BarDataSet(acceptanceEntries, getString(R.string.chart_acceptance)).apply {
                color = Color.parseColor("#FFC107")
            }
        val emergenceDataSet =
            BarDataSet(emergenceEntries, getString(R.string.chart_emergence)).apply {
                color = Color.parseColor("#4CAF50")
            }
        val matingSuccessDataSet = BarDataSet(
            matingSuccessEntries,
            getString(R.string.chart_mating_success)
        ).apply { color = Color.parseColor("#2196F3") }

        val barData = BarData(acceptanceDataSet, emergenceDataSet, matingSuccessDataSet)
        barData.setValueFormatter(PercentFormatter())


        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isNightMode) Color.WHITE else Color.BLACK

        acceptanceDataSet.valueTextColor = textColor
        emergenceDataSet.valueTextColor = textColor
        matingSuccessDataSet.valueTextColor = textColor

        binding.batchChart.apply {
            data = barData
            description.isEnabled = false

            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.TOP
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.textColor = textColor
            xAxis.setCenterAxisLabels(true)

            axisLeft.textColor = textColor
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisLeft.valueFormatter = PercentFormatter()

            axisRight.isEnabled = false

            legend.textColor = textColor

            val groupSpace = 0.1f
            val barSpace = 0.05f
            val barWidth = 0.25f
            barData.barWidth = barWidth
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 0f + barData.getGroupWidth(groupSpace, barSpace) * batches.size
            groupBars(0f, groupSpace, barSpace)

            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
