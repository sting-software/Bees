package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditInspectionViewModel @Inject constructor(private val repository: ApiaryRepository) : ViewModel() {

    private val _saveCompleted = MutableLiveData<Boolean?>()
    val saveCompleted: LiveData<Boolean?> = _saveCompleted

    fun getInspection(inspectionId: Long): LiveData<Inspection?> = liveData {
        emit(repository.getInspectionById(inspectionId))
    }

    fun saveInspection(inspection: Inspection) = viewModelScope.launch {
        try {
            repository.insertInspection(inspection)
            _saveCompleted.value = true
        } catch (e: Exception) {
            _saveCompleted.value = false
            e.printStackTrace()
        }
    }

    fun updateInspection(inspection: Inspection) = viewModelScope.launch {
        try {
            repository.updateInspection(inspection)
            _saveCompleted.value = true
        } catch (e: Exception) {
            _saveCompleted.value = false
            e.printStackTrace()
        }
    }

    fun resetSaveCompleted() {
        _saveCompleted.value = null
    }
}
