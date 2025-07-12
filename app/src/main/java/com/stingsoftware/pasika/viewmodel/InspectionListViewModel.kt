package com.stingsoftware.pasika.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.repository.ApiaryRepository
import com.stingsoftware.pasika.util.CsvExporter
import com.stingsoftware.pasika.util.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspectionListViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hiveId: Long = savedStateHandle.get<Long>("hiveId")!!

    private val _exportStatus = MutableLiveData<Boolean?>()
    val exportStatus: LiveData<Boolean?> = _exportStatus

    val inspectionsForHive: LiveData<List<Inspection>> =
        repository.getInspectionsForHive(hiveId).asLiveData()

    fun deleteInspection(inspection: Inspection) = viewModelScope.launch {
        repository.deleteInspection(inspection)
    }

    fun exportInspectionsToCsv(context: Context, hiveNumber: String) {
        viewModelScope.launch {
            val inspections = inspectionsForHive.value
            val success = if (!inspections.isNullOrEmpty()) {
                CsvExporter.exportInspections(context, hiveNumber, inspections)
            } else false
            _exportStatus.postValue(success)
        }
    }

    // New function for PDF export
    fun exportInspectionsToPdf(context: Context, hiveNumber: String) {
        viewModelScope.launch {
            val inspections = inspectionsForHive.value
            val success = if (!inspections.isNullOrEmpty()) {
                PdfExporter.exportInspections(context, hiveNumber, inspections)
            } else false
            _exportStatus.postValue(success)
        }
    }

    fun onExportStatusHandled() {
        _exportStatus.value = null
    }
}