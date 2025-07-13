package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.stingsoftware.pasika.data.StatsByBreed
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

enum class StatsType {
    OVERALL,
    BY_BREED,
    BY_HIVE_TYPE
}

@HiltViewModel
class StatsViewModel @Inject constructor(private val repository: ApiaryRepository) : ViewModel() {

    val totalApiariesCount = repository.totalApiariesCount.asLiveData()
    val totalHivesCount = repository.totalHivesCount.asLiveData()

    val selectedStatsType = MutableStateFlow(StatsType.OVERALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val groupedStats = selectedStatsType.flatMapLatest { type ->
        when (type) {
            StatsType.BY_BREED -> repository.getStatsByBreed()
            // Add other cases here as you implement them
            // StatsType.BY_HIVE_TYPE -> repository.getStatsByHiveType()
            else -> flowOf(emptyList<StatsByBreed>()) // Return empty flow for OVERALL
        }
    }.asLiveData()

    fun setStatsType(type: StatsType) {
        selectedStatsType.value = type
    }
}