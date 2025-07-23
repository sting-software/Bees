package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Hive
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
        lastInspectionDate: Long?,
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
        queenCells: Int?,
        isolationFromDate: Long?,
        isolationToDate: Long?,
        defensivenessRating: Int?,
        framesTotal: Int?,
        framesEggs: Int?,
        framesOpenBrood: Int?,
        framesCappedBrood: Int?,
        framesFeed: Int?,
        givenBuiltCombs: Int?,
        givenFoundation: Int?,
        givenBrood: Int?,
        givenBeesKg: Double?,
        givenHoneyKg: Double?,
        givenSugarKg: Double?,
        treatment: String?
    ) = viewModelScope.launch {
        try {
            if (autoNumber && startingHiveNumber != null && endingHiveNumber != null) {
                for (i in startingHiveNumber..endingHiveNumber) {
                    val hive = Hive(
                        apiaryId = apiaryId, hiveNumber = i.toString(), hiveType = hiveType,
                        hiveTypeOther = hiveTypeOther, frameType = frameType, frameTypeOther = frameTypeOther,
                        material = material, materialOther = materialOther, breed = breed, breedOther = breedOther,
                        lastInspectionDate = lastInspectionDate, notes = notes, queenTagColor = queenTagColor,
                        queenTagColorOther = queenTagColorOther, queenNumber = queenNumber, queenYear = queenYear,
                        queenLine = queenLine, queenCells = queenCells, isolationFromDate = isolationFromDate,
                        isolationToDate = isolationToDate, defensivenessRating = defensivenessRating,
                        framesTotal = framesTotal, framesEggs = framesEggs, framesOpenBrood = framesOpenBrood,
                        framesCappedBrood = framesCappedBrood, framesFeed = framesFeed, givenBuiltCombs = givenBuiltCombs,
                        givenFoundation = givenFoundation, givenBrood = givenBrood, givenBeesKg = givenBeesKg,
                        givenHoneyKg = givenHoneyKg, givenSugarKg = givenSugarKg, treatment = treatment
                    )
                    repository.insertHive(hive)
                }
            } else {
                for (i in 1..quantity) {
                    val hive = Hive(
                        apiaryId = apiaryId, hiveNumber = null, hiveType = hiveType,
                        hiveTypeOther = hiveTypeOther, frameType = frameType, frameTypeOther = frameTypeOther,
                        material = material, materialOther = materialOther, breed = breed, breedOther = breedOther,
                        lastInspectionDate = lastInspectionDate, notes = notes, queenTagColor = queenTagColor,
                        queenTagColorOther = queenTagColorOther, queenNumber = queenNumber, queenYear = queenYear,
                        queenLine = queenLine, queenCells = queenCells, isolationFromDate = isolationFromDate,
                        isolationToDate = isolationToDate, defensivenessRating = defensivenessRating,
                        framesTotal = framesTotal, framesEggs = framesEggs, framesOpenBrood = framesOpenBrood,
                        framesCappedBrood = framesCappedBrood, framesFeed = framesFeed, givenBuiltCombs = givenBuiltCombs,
                        givenFoundation = givenFoundation, givenBrood = givenBrood, givenBeesKg = givenBeesKg,
                        givenHoneyKg = givenHoneyKg, givenSugarKg = givenSugarKg, treatment = treatment
                    )
                    repository.insertHive(hive)
                }
            }
            repository.updateApiaryHiveCount(apiaryId)
            _saveCompleted.value = true
        } catch (e: Exception) {
            _saveCompleted.value = false
            e.printStackTrace()
        }
    }

    fun updateApiaryLastInspectionDate(apiaryId: Long, newDateMillis: Long) = viewModelScope.launch {
        val apiary = repository.getApiaryById(apiaryId)
        apiary?.let {
            repository.updateApiary(it.copy(lastInspectionDate = newDateMillis))
        }
    }

    fun resetSaveCompleted() {
        _saveCompleted.value = null
    }

    fun updateInspectionDateForApiary(apiaryId: Long, newDateMillis: Long) = viewModelScope.launch {
        repository.updateInspectionDateForApiary(apiaryId, newDateMillis)
        // Optionally use LiveData to signal completion if needed
    }
}