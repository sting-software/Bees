package com.stingsoftware.pasika.ui.queenrearing.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: ApiaryRepository
) : ViewModel() {

    val stats = repository.getQueenRearingStats().asLiveData()
    val batches = repository.getAllGraftingBatches().asLiveData()
    val allCells = repository.getAllQueenCells().asLiveData()
}