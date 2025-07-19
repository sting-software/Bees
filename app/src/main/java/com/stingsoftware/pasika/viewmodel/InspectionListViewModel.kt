package com.stingsoftware.pasika.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspectionListViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hiveId = savedStateHandle.get<Long>("hiveId")!!
    private val searchQuery = MutableStateFlow("")

    private val _exportStatus = MutableLiveData<Boolean?>()
    val exportStatus: LiveData<Boolean?> = _exportStatus

    val inspections: LiveData<List<Inspection>> = searchQuery.flatMapLatest { query: String ->
        if (query.isBlank()) {
            repository.getInspectionsForHive(hiveId)
        } else {
            // This call will now resolve correctly
            repository.searchInspections(hiveId, query)
        }
    }.asLiveData()
    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun deleteInspection(inspection: Inspection) = viewModelScope.launch {
        repository.deleteInspection(inspection)
    }

    fun exportInspectionsToCsv(context: Context, hiveNumber: String) = viewModelScope.launch {
        // Placeholder for your export logic
        _exportStatus.value = true
    }

    fun exportInspectionsToPdf(context: Context, hiveNumber: String) = viewModelScope.launch {
        // Placeholder for your export logic
        _exportStatus.value = true
    }

    fun onExportStatusHandled() {
        _exportStatus.value = null
    }
}