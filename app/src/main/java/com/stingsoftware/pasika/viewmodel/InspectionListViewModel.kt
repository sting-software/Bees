package com.stingsoftware.pasika.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.stingsoftware.pasika.data.Hive
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

    // LiveData to hold the current hive's details
    private val _hive = MutableLiveData<Hive?>()
    val hive: LiveData<Hive?> = _hive

    init {
        // Fetch the hive details when the ViewModel is created
        viewModelScope.launch {
            _hive.value = repository.getHiveById(hiveId)
        }
    }

    val inspections: LiveData<List<Inspection>> = searchQuery.flatMapLatest { query: String ->
        if (query.isBlank()) {
            repository.getInspectionsForHive(hiveId)
        } else {
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
        val currentInspections = inspections.value
        if (currentInspections.isNullOrEmpty()) {
            _exportStatus.postValue(false)
            return@launch
        }

        val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.stingsoftware.pasika.util.CsvExporter.exportInspections(
                context,
                hiveNumber,
                currentInspections
            )
        }
        _exportStatus.postValue(success)
    }

    fun exportInspectionsToPdf(context: Context, hiveNumber: String) = viewModelScope.launch {
        val currentInspections = inspections.value
        if (currentInspections.isNullOrEmpty()) {
            _exportStatus.postValue(false)
            return@launch
        }

        val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.stingsoftware.pasika.util.PdfExporter.exportInspections(
                context,
                hiveNumber,
                currentInspections
            )
        }
        _exportStatus.postValue(success)
    }

    fun onExportStatusHandled() {
        _exportStatus.value = null
    }
}
