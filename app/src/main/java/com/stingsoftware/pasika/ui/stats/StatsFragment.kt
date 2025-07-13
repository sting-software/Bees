package com.stingsoftware.pasika.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.StatsByBreed
import com.stingsoftware.pasika.databinding.FragmentStatsBinding
import com.stingsoftware.pasika.viewmodel.StatsType
import com.stingsoftware.pasika.viewmodel.StatsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class StatsFragment : Fragment() {

    private val statsViewModel: StatsViewModel by viewModels()
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = "Statistics"
        setupDropdown()
        setupObservers()
        setupChart()
    }

    private fun setupDropdown() {
        val statTypes = StatsType.entries.map { it.name.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, statTypes)
        binding.autoCompleteTextViewStatsType.setAdapter(adapter)
        binding.autoCompleteTextViewStatsType.setText(statTypes[0], false)

        binding.autoCompleteTextViewStatsType.setOnItemClickListener { _, _, position, _ ->
            statsViewModel.setStatsType(StatsType.entries[position])
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                statsViewModel.selectedStatsType.collect { type ->
                    // FIX: Reference the correct view ID for the overall stats card
                    if (type == StatsType.OVERALL) {
                        binding.cardOverallStats.visibility = View.VISIBLE
                        binding.statsChart.visibility = View.GONE
                        binding.emptyStateContainer.visibility = View.GONE
                    } else {
                        binding.cardOverallStats.visibility = View.GONE
                    }
                }
            }
        }

        statsViewModel.totalApiariesCount.observe(viewLifecycleOwner) { count ->
            binding.textViewTotalApiariesValue.text = count.toString()
        }
        statsViewModel.totalHivesCount.observe(viewLifecycleOwner) { totalHives ->
            binding.textViewTotalHivesValue.text = (totalHives ?: 0).toString()
            updateAverageHives()
        }

        statsViewModel.groupedStats.observe(viewLifecycleOwner) { stats ->
            if (statsViewModel.selectedStatsType.value != StatsType.OVERALL) {
                if (stats.isNullOrEmpty()) {
                    binding.statsChart.visibility = View.GONE
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    binding.emptyStateContainer.findViewById<TextView>(R.id.text_view_empty_message).text = getString(R.string.stats_no_data)
                } else {
                    binding.statsChart.visibility = View.VISIBLE
                    binding.emptyStateContainer.visibility = View.GONE
                    updateChart(stats)
                }
            }
        }
    }

    private fun updateAverageHives() {
        val totalApiaries = statsViewModel.totalApiariesCount.value ?: 0
        val totalHives = statsViewModel.totalHivesCount.value ?: 0

        val average = if (totalApiaries > 0) {
            totalHives.toFloat() / totalApiaries
        } else {
            0f
        }
        binding.textViewAvgHivesValue.text = String.format(Locale.getDefault(), "%.1f", average)
    }

    private fun setupChart() {
        binding.statsChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(true)
            setFitBars(true)
            isDoubleTapToZoomEnabled = false
            setPinchZoom(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.app_text_secondary)
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = ContextCompat.getColor(requireContext(), R.color.app_text_secondary)
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
        }
    }

    private fun updateChart(stats: List<*>) {
        if (stats.firstOrNull() !is StatsByBreed) {
            binding.statsChart.clear()
            binding.statsChart.invalidate()
            return
        }

        val breedStats = stats as List<StatsByBreed>
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        breedStats.forEachIndexed { index, stat ->
            entries.add(BarEntry(index.toFloat(), stat.hiveCount.toFloat()))
            labels.add(stat.breed ?: "Unknown")
        }

        val dataSet = BarDataSet(entries, "Hives per Breed")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.app_text_primary)
        dataSet.valueTextSize = 12f

        binding.statsChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.statsChart.xAxis.labelCount = labels.size
        binding.statsChart.data = BarData(dataSet)
        binding.statsChart.animateY(1000)
        binding.statsChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
