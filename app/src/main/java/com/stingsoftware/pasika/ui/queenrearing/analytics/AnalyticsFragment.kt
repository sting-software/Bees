package com.stingsoftware.pasika.ui.queenrearing.analytics

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.data.QueenCellStatus
import com.stingsoftware.pasika.data.QueenRearingAnalytics
import com.stingsoftware.pasika.databinding.FragmentAnalyticsBinding
import dagger.hilt.android.AndroidEntryPoint

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

        // Observe the main analytics data
        viewModel.analytics.observe(viewLifecycleOwner) { analytics ->
            if (analytics != null && analytics.totalGrafted > 0) {
                binding.overallSuccessRateText.text = getString(R.string.overall_success_rate_format, analytics.overallSuccessRate)
                setupFunnelChart(analytics)
                setupStatusPieChart(analytics)
            } else {
                clearCharts()
            }
        }

        // Observe batches and cells for the batch-specific charts
        viewModel.batches.observe(viewLifecycleOwner) { batches ->
            viewModel.allCells.observe(viewLifecycleOwner) { cells ->
                if (batches.isNotEmpty() && cells.isNotEmpty()) {
                    setupBarChart(batches, cells)
                } else {
                    clearBatchCharts()
                }
            }
        }
    }

    private fun setupFunnelChart(analytics: QueenRearingAnalytics) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        val stages = listOf(
            getString(R.string.queen_cell_status_grafted) to analytics.totalGrafted.toFloat(),
            getString(R.string.queen_cell_status_accepted) to analytics.acceptance.successCount.toFloat(),
            getString(R.string.queen_cell_status_capped) to analytics.capping.successCount.toFloat(),
            getString(R.string.queen_cell_status_emerged) to analytics.emergence.successCount.toFloat(),
            getString(R.string.queen_cell_status_laying) to analytics.mating.successCount.toFloat()
        )

        stages.forEachIndexed { index, (label, count) ->
            entries.add(BarEntry(index.toFloat(), count))
            labels.add(label)
        }

        val dataSet = BarDataSet(entries, getString(R.string.cells_at_stage))
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryVariant),
            ContextCompat.getColor(requireContext(), R.color.colorPrimary),
            ContextCompat.getColor(requireContext(), R.color.colorSecondary),
            ContextCompat.getColor(requireContext(), R.color.colorSecondaryVariant),
            Color.rgb(255, 193, 7)
        )

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        val textColor = getThemeTextColor()
        dataSet.valueTextColor = textColor
        barData.setValueFormatter(object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        })

        binding.funnelChart.apply {
            setNoDataText(getString(R.string.chart_no_data))
            data = barData
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(true)
            setFitBars(true)
            // Add extra space at the bottom of the chart to prevent label clipping
            setExtraOffsets(0f, 0f, 0f, 30f)

            val xAxis = this.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.textColor = textColor
            xAxis.labelRotationAngle = -30f

            val leftAxis = this.axisLeft
            leftAxis.textColor = textColor
            leftAxis.axisMinimum = 0f
            leftAxis.setDrawGridLines(true)

            axisRight.isEnabled = false

            invalidate()
        }
    }

    private fun setupStatusPieChart(analytics: QueenRearingAnalytics) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        val statusColorMap = mapOf(
            QueenCellStatus.GRAFTED to ContextCompat.getColor(requireContext(), R.color.teal_200),
            QueenCellStatus.ACCEPTED to ContextCompat.getColor(requireContext(), R.color.colorPrimaryVariant),
            QueenCellStatus.CAPPED to ContextCompat.getColor(requireContext(), R.color.colorPrimary),
            QueenCellStatus.EMERGED to ContextCompat.getColor(requireContext(), R.color.colorSecondary),
            QueenCellStatus.MATING to ContextCompat.getColor(requireContext(), R.color.colorSecondaryVariant),
            QueenCellStatus.LAYING to ContextCompat.getColor(requireContext(), R.color.purple_200),
            QueenCellStatus.SOLD to ContextCompat.getColor(requireContext(), R.color.purple_500),
            QueenCellStatus.FAILED to ContextCompat.getColor(requireContext(), R.color.colorError)
        )

        analytics.cellStatusDistribution.forEach { (status, count) ->
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), status.getLabel(requireContext())))
                colors.add(statusColorMap[status] ?: Color.GRAY)
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        dataSet.sliceSpace = 2f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(binding.statusPieChart))

        binding.statusPieChart.apply {
            setNoDataText(getString(R.string.chart_no_data))
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            legend.isEnabled = false
            invalidate()
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
            val accepted = batchCells.count { it.status >= QueenCellStatus.ACCEPTED && it.status != QueenCellStatus.FAILED }
            val emerged = batchCells.count { it.status >= QueenCellStatus.EMERGED && it.status != QueenCellStatus.FAILED }
            val layingOrSold = batchCells.count { it.status == QueenCellStatus.LAYING || it.status == QueenCellStatus.SOLD }

            val acceptanceRate = if (totalGrafted > 0) (accepted.toFloat() / totalGrafted.toFloat()) * 100 else 0f
            val emergenceRate = if (accepted > 0) (emerged.toFloat() / accepted.toFloat()) * 100 else 0f
            val matingSuccessRate = if (emerged > 0) (layingOrSold.toFloat() / emerged.toFloat()) * 100 else 0f

            val xValue = index.toFloat()
            acceptanceEntries.add(BarEntry(xValue, acceptanceRate))
            emergenceEntries.add(BarEntry(xValue, emergenceRate))
            matingSuccessEntries.add(BarEntry(xValue, matingSuccessRate))
        }

        val acceptanceDataSet = BarDataSet(acceptanceEntries, getString(R.string.chart_acceptance)).apply { color = ContextCompat.getColor(requireContext(), R.color.colorPrimary) }
        val emergenceDataSet = BarDataSet(emergenceEntries, getString(R.string.chart_emergence)).apply { color = ContextCompat.getColor(requireContext(), R.color.colorSecondary) }
        val matingSuccessDataSet = BarDataSet(matingSuccessEntries, getString(R.string.chart_mating_success)).apply { color = ContextCompat.getColor(requireContext(), R.color.colorPrimaryVariant) }

        val dataSets = mutableListOf<IBarDataSet>(acceptanceDataSet, emergenceDataSet, matingSuccessDataSet)
        val barData = BarData(dataSets)
        barData.setValueFormatter(PercentFormatter())

        val textColor = getThemeTextColor()
        barData.setValueTextColor(textColor)

        binding.batchChart.apply {
            setNoDataText(getString(R.string.chart_no_data))
            data = barData
            description.isEnabled = false
            setDrawValueAboveBar(false)

            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.TOP
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.textColor = textColor
            xAxis.setCenterAxisLabels(true)
            xAxis.setDrawGridLines(false)

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

    private fun clearCharts() {
        binding.funnelChart.clear()
        binding.statusPieChart.clear()
        binding.overallSuccessRateText.text = ""
        binding.funnelChart.invalidate()
        binding.statusPieChart.invalidate()
    }

    private fun clearBatchCharts() {
        binding.batchChart.clear()
        binding.batchChart.invalidate()
    }

    private fun getThemeTextColor(): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
