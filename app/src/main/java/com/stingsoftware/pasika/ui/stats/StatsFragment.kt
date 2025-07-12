package com.stingsoftware.pasika.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.stingsoftware.pasika.databinding.FragmentStatsBinding
import com.stingsoftware.pasika.viewmodel.StatsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class StatsFragment : Fragment() {

    // ViewModel is now injected by Hilt
    private val statsViewModel: StatsViewModel by viewModels()

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statsViewModel.totalApiariesCount.observe(viewLifecycleOwner) { count ->
            binding.textViewTotalApiariesValue.text = count.toString()
            updateEmptyStateVisibility(count)
        }

        statsViewModel.totalHivesCount.observe(viewLifecycleOwner) { totalHives ->
            binding.textViewTotalHivesValue.text = (totalHives ?: 0).toString()
            updateAverageHives()
        }
    }

    private fun updateEmptyStateVisibility(apiaryCount: Int) {
        if (apiaryCount == 0) {
            binding.textViewStatsEmptyState.visibility = View.VISIBLE
            binding.textViewTotalApiariesValue.text = "0"
            binding.textViewTotalHivesValue.text = "0"
            binding.textViewAvgHivesValue.text = "0.0"
        } else {
            binding.textViewStatsEmptyState.visibility = View.GONE
        }
        updateAverageHives()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}