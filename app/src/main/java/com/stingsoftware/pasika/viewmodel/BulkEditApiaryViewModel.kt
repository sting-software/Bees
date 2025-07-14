package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.ApiaryType
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BulkEditApiaryViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val apiaryIds: LongArray = savedStateHandle.get<LongArray>("apiaryIds") ?: longArrayOf()

    private val _selectedApiaries = MutableLiveData<List<Apiary>>()
    val selectedApiaries: LiveData<List<Apiary>> = _selectedApiaries

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    init {
        fetchSelectedApiaries()
    }

    private fun fetchSelectedApiaries() {
        viewModelScope.launch {
            // FIX: Convert LongArray to a List before calling mapNotNull
            _selectedApiaries.value = apiaryIds.asList().mapNotNull { id ->
                repository.getApiaryById(id)
            }
        }
    }

    fun updateApiaries(location: String?, type: ApiaryType?, notes: String?) {
        viewModelScope.launch {
            try {
                _selectedApiaries.value?.forEach { apiary ->
                    val updatedApiary = apiary.copy(
                        location = location ?: apiary.location,
                        type = type ?: apiary.type,
                        notes = notes ?: apiary.notes
                    )
                    repository.updateApiary(updatedApiary)
                }
                _updateStatus.value = true
            } catch (_: Exception) {
                _updateStatus.value = false
            }
        }
    }
}