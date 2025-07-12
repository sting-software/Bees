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

    // --- NEW: LiveData to hold all apiaries for the move dialog ---
    val allApiaries: LiveData<List<Apiary>> = repository.allApiaries.asLiveData()

    // --- NEW: LiveData for move operation status ---
    private val _moveStatus = MutableLiveData<Boolean?>()
    val moveStatus: LiveData<Boolean?> = _moveStatus

    // ... (existing properties and functions)
    val apiary: LiveData<Apiary?> = liveData { emit(repository.getApiaryById(apiaryId)) }
    val allHivesForApiary: LiveData<List<Hive>> = repository.getHivesForApiary(apiaryId).asLiveData()
    val filteredHivesForApiary: MediatorLiveData<List<Hive>> = MediatorLiveData()
    private val _hiveSearchQuery = MutableStateFlow<String?>(null)

    init {
        // MediatorLiveData setup
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

    // --- NEW: Function to trigger the move operation ---
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

    // ... (existing functions: setHiveSearchQuery, filterHives, deleteHive)
}