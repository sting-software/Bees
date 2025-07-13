package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiaryDetailViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val apiaryId: Long = savedStateHandle.get<Long>("apiaryId")!!

    val allApiaries: LiveData<List<Apiary>> = repository.allApiaries.asLiveData()

    private val _moveStatus = MutableLiveData<Boolean?>()
    val moveStatus: LiveData<Boolean?> = _moveStatus

    private val _hiveSearchQuery = MutableStateFlow<String?>(null)
    val hiveSearchQuery = _hiveSearchQuery.asStateFlow()

    val apiary: LiveData<Apiary?> = liveData {
        emit(repository.getApiaryById(apiaryId))
    }

    val allHivesForApiary: LiveData<List<Hive>> = repository.getHivesForApiary(apiaryId).asLiveData()

    val filteredHivesForApiary: MediatorLiveData<List<Hive>> = MediatorLiveData()

    init {
        var latestHives: List<Hive>? = null
        var latestSearchQuery: String? = null

        filteredHivesForApiary.addSource(allHivesForApiary) { hives ->
            latestHives = hives
            filteredHivesForApiary.value = filterHives(latestHives, latestSearchQuery)
        }

        filteredHivesForApiary.addSource(_hiveSearchQuery.asLiveData()) { query ->
            latestSearchQuery = query
            filteredHivesForApiary.value = filterHives(latestHives, latestSearchQuery)
        }
    }

    fun moveHives(hiveIds: List<Long>, destinationApiaryId: Long) {
        viewModelScope.launch {
            try {
                repository.moveHives(hiveIds, apiaryId, destinationApiaryId)
                _moveStatus.postValue(true)
            } catch (e: Exception) {
                _moveStatus.postValue(false)
            }
        }
    }

    fun onMoveStatusHandled() {
        _moveStatus.value = null
    }

    fun setHiveSearchQuery(query: String?) {
        _hiveSearchQuery.value = query
    }

    private fun filterHives(hives: List<Hive>?, query: String?): List<Hive> {
        if (hives.isNullOrEmpty()) {
            return emptyList()
        }
        return if (query.isNullOrBlank()) {
            hives
        } else {
            hives.filter { hive ->
                hive.hiveNumber?.contains(query, ignoreCase = true) == true ||
                        hive.notes?.contains(query, ignoreCase = true) == true
            }
        }
    }

    // --- The restored function ---
    fun deleteHive(hive: Hive) = viewModelScope.launch {
        repository.deleteHive(hive)
        repository.updateApiaryHiveCount(hive.apiaryId)
    }
}