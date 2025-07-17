package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BulkEditHiveViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Retrieve hiveIds safely from navigation arguments via SavedStateHandle
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
        // Existing params
        hiveType: String?, frameType: String?, framesTotal: Int?, breed: String?,
        lastInspectionDate: Long?, notes: String?, autoNumber: Boolean, startingHiveNumber: Int?,
        // New params to add
        material: String?, queenTagColor: String?, queenNumber: String?, queenYear: String?,
        queenLine: String?, queenCells: Int?, isolationFromDate: Long?, isolationToDate: Long?,
        defensivenessRating: Int?, framesEggs: Int?, framesOpenBrood: Int?, framesCappedBrood: Int?,
        framesFeed: Int?, givenBuiltCombs: Int?, givenFoundation: Int?, givenBrood: Int?,
        givenBeesKg: Double?, givenHoneyKg: Double?, givenSugarKg: Double?, treatment: String?
    ) = viewModelScope.launch {
        _selectedHives.value?.let { hives ->
            val sortedHives = hives.sortedBy { it.id }

            for ((index, hive) in sortedHives.withIndex()) {
                val updatedHive = hive.copy(
                    // Existing copy logic
                    hiveType = hiveType ?: hive.hiveType,
                    frameType = frameType ?: hive.frameType,
                    framesTotal = framesTotal ?: hive.framesTotal,
                    breed = breed ?: hive.breed,
                    lastInspectionDate = lastInspectionDate ?: hive.lastInspectionDate,
                    notes = notes ?: hive.notes,
                    hiveNumber = if (autoNumber && startingHiveNumber != null) {
                        (startingHiveNumber + index).toString()
                    } else {
                        hive.hiveNumber
                    },
                    // New copy logic for all other attributes
                    material = material ?: hive.material,
                    queenTagColor = queenTagColor ?: hive.queenTagColor,
                    queenNumber = queenNumber ?: hive.queenNumber,
                    queenYear = queenYear ?: hive.queenYear,
                    queenLine = queenLine ?: hive.queenLine,
                    queenCells = queenCells ?: hive.queenCells,
                    isolationFromDate = isolationFromDate ?: hive.isolationFromDate,
                    isolationToDate = isolationToDate ?: hive.isolationToDate,
                    defensivenessRating = defensivenessRating ?: hive.defensivenessRating,
                    framesEggs = framesEggs ?: hive.framesEggs,
                    framesOpenBrood = framesOpenBrood ?: hive.framesOpenBrood,
                    framesCappedBrood = framesCappedBrood ?: hive.framesCappedBrood,
                    framesFeed = framesFeed ?: hive.framesFeed,
                    givenBuiltCombs = givenBuiltCombs ?: hive.givenBuiltCombs,
                    givenFoundation = givenFoundation ?: hive.givenFoundation,
                    givenBrood = givenBrood ?: hive.givenBrood,
                    givenBeesKg = givenBeesKg ?: hive.givenBeesKg,
                    givenHoneyKg = givenHoneyKg ?: hive.givenHoneyKg,
                    givenSugarKg = givenSugarKg ?: hive.givenSugarKg,
                    treatment = treatment ?: hive.treatment
                )
                repository.updateHive(updatedHive)
            }
        }
    }
}