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
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InspectionListViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
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
        // 1. Get the current list of inspections from the LiveData
        val currentInspections = inspections.value
        if (currentInspections.isNullOrEmpty()) {
            _exportStatus.postValue(false) // Nothing to export
            return@launch
        }

        // 2. Run the file export on a background thread to keep the UI responsive
        val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // 3. Call your actual CsvExporter utility
            com.stingsoftware.pasika.util.CsvExporter.exportInspections(
                context,
                hiveNumber,
                currentInspections
            )
        }

        // 4. Post the true/false result back to the UI thread
        _exportStatus.postValue(success)
    }

    fun exportInspectionsToPdf(context: Context, hiveNumber: String) = viewModelScope.launch {
        // 1. Get the current list of inspections from the LiveData
        val currentInspections = inspections.value
        if (currentInspections.isNullOrEmpty()) {
            _exportStatus.postValue(false) // Nothing to export
            return@launch
        }

        // 2. Run the file export on a background thread to keep the UI responsive
        val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // 3. Call your actual PdfExporter utility
            com.stingsoftware.pasika.util.PdfExporter.exportInspections(
                context,
                hiveNumber,
                currentInspections
            )
        }

        // 4. Post the true/false result back to the UI thread
        _exportStatus.postValue(success)
    }

    fun onExportStatusHandled() {
        _exportStatus.value = null
    }
}