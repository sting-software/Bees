package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BulkEditHiveViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hiveIds: LongArray = savedStateHandle.get<LongArray>("hiveIds") ?: longArrayOf()

    private val _selectedHives = MutableLiveData<List<Hive>>()
    val selectedHives: LiveData<List<Hive>>
        get() = _selectedHives

    init {
        fetchSelectedHives()
    }

    private fun fetchSelectedHives() {
        viewModelScope.launch {
            val hives = mutableListOf<Hive>()
            for (id in hiveIds) {
                repository.getHiveById(id)?.let { hive ->
                    hives.add(hive)
                }
            }
            _selectedHives.postValue(hives)
        }
    }

    fun updateSelectedHives(
        material: String?,
        hiveType: String?,
        frameType: String?,
        breed: String?,
        role: HiveRole?,
        queenTagColor: String?,
        queenNumber: String?,
        queenYear: String?,
        queenLine: String?,
        isolationFromDate: Long?,
        isolationToDate: Long?,
        notes: String?,
        autoNumber: Boolean,
        startingHiveNumber: Int?
    ) = viewModelScope.launch {
        _selectedHives.value?.let { hives ->
            val sortedHives = hives.sortedBy { it.id }

            for ((index, hive) in sortedHives.withIndex()) {
                val updatedHive = hive.copy(
                    material = material ?: hive.material,
                    hiveType = hiveType ?: hive.hiveType,
                    frameType = frameType ?: hive.frameType,
                    breed = breed ?: hive.breed,
                    role = role ?: hive.role,
                    queenTagColor = queenTagColor ?: hive.queenTagColor,
                    queenNumber = queenNumber ?: hive.queenNumber,
                    queenYear = queenYear ?: hive.queenYear,
                    queenLine = queenLine ?: hive.queenLine,
                    isolationFromDate = isolationFromDate ?: hive.isolationFromDate,
                    isolationToDate = isolationToDate ?: hive.isolationToDate,
                    notes = notes ?: hive.notes,
                    hiveNumber = if (autoNumber && startingHiveNumber != null) {
                        (startingHiveNumber + index).toString()
                    } else {
                        hive.hiveNumber
                    }
                )
                repository.updateHive(updatedHive)
            }
        }
    }
}
