package com.stingsoftware.pasika.ui.queenrearing.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: ApiaryRepository
) : ViewModel() {

    /**
     * NEW: Exposes a single LiveData object containing all calculated analytics.
     * The fragment will observe this to update all charts.
     */
    val analytics = repository.getQueenRearingAnalytics().asLiveData()

    /**
     * These are still needed for charts that require batch-specific data,
     * like the "Performance by Batch" chart.
     */
    val batches = repository.getAllGraftingBatches().asLiveData()
    val allCells = repository.getAllQueenCells().asLiveData()
}