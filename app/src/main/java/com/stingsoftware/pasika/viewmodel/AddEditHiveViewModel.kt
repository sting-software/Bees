package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditHiveViewModel @Inject constructor(private val repository: ApiaryRepository) : ViewModel() {

    private val _saveCompleted = MutableLiveData<Boolean?>()
    val saveCompleted: LiveData<Boolean?> = _saveCompleted

    fun getHive(hiveId: Long): LiveData<Hive?> = liveData {
        emit(repository.getHiveById(hiveId))
    }

    fun saveOrUpdateHive(hive: Hive) = viewModelScope.launch {
        try {
            if (hive.id == 0L) {
                repository.insertHive(hive)
            } else {
                repository.updateHive(hive)
            }
            repository.updateApiaryHiveCount(hive.apiaryId)
            _saveCompleted.value = true
        } catch (e: Exception) {
            _saveCompleted.value = false
            e.printStackTrace()
        }
    }

    fun saveOrUpdateHives(
        apiaryId: Long,
        hiveType: String?,
        hiveTypeOther: String? = null,
        frameType: String?,
        frameTypeOther: String? = null,
        material: String?,
        materialOther: String? = null,
        breed: String?,
        breedOther: String? = null,
        notes: String?,
        quantity: Int,
        autoNumber: Boolean,
        startingHiveNumber: Int?,
        endingHiveNumber: Int?,
        queenTagColor: String?,
        queenTagColorOther: String?,
        queenNumber: String?,
        queenYear: String?,
        queenLine: String?,
        isolationFromDate: Long?,
        isolationToDate: Long?,
        role: HiveRole
    ) = viewModelScope.launch {
        try {
            val hivesToInsert = mutableListOf<Hive>()
            val baseHive = Hive(
                apiaryId = apiaryId, hiveNumber = null, hiveType = hiveType,
                hiveTypeOther = hiveTypeOther, frameType = frameType, frameTypeOther = frameTypeOther,
                material = material, materialOther = materialOther, breed = breed, breedOther = breedOther,
                notes = notes, queenTagColor = queenTagColor,
                queenTagColorOther = queenTagColorOther, queenNumber = queenNumber, queenYear = queenYear,
                queenLine = queenLine, isolationFromDate = isolationFromDate,
                isolationToDate = isolationToDate,
                role = role
            )

            if (autoNumber && startingHiveNumber != null && endingHiveNumber != null) {
                for (i in startingHiveNumber..endingHiveNumber) {
                    hivesToInsert.add(baseHive.copy(hiveNumber = i.toString()))
                }
            } else {
                for (i in 1..quantity) {
                    hivesToInsert.add(baseHive.copy())
                }
            }

            if (hivesToInsert.isNotEmpty()) {
                repository.insertHives(hivesToInsert)
            }

            repository.updateApiaryHiveCount(apiaryId)
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
