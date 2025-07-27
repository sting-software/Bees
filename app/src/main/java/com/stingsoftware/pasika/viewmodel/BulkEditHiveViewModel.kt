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

    /**
     * Updates all selected hives with the provided values.
     * Any parameter that is null will not be updated, preserving the original value in the hive.
     */
    fun updateSelectedHives(
        // Hive Characteristics
        material: String?,
        hiveType: String?,
        frameType: String?,
        breed: String?,
        role: HiveRole?,

        // Frame Counts
        framesEggs: Int?,
        framesOpenBrood: Int?,
        framesCappedBrood: Int?,
        framesFeed: Int?,
        framesTotal: Int?,

        // Queen Bee
        queenTagColor: String?,
        queenNumber: String?,
        queenYear: String?,
        queenLine: String?,
        isolationFromDate: Long?,
        isolationToDate: Long?,

        // Other attributes
        defensivenessRating: Int?,

        // Given/Taken
        givenBuiltCombs: Int?,
        givenFoundation: Int?,
        givenBrood: Int?,
        givenBeesKg: Double?,
        givenHoneyKg: Double?,
        givenSugarKg: Double?,

        // Treatment
        treatment: String?,

        // General
        lastInspectionDate: Long?,
        notes: String?,

        // Numbering
        autoNumber: Boolean,
        startingHiveNumber: Int?
    ) = viewModelScope.launch {
        _selectedHives.value?.let { hives ->
            // Sort hives by their current ID to ensure consistent re-numbering
            val sortedHives = hives.sortedBy { it.id }

            for ((index, hive) in sortedHives.withIndex()) {
                val updatedHive = hive.copy(
                    // Hive Characteristics
                    material = material ?: hive.material,
                    hiveType = hiveType ?: hive.hiveType,
                    frameType = frameType ?: hive.frameType,
                    breed = breed ?: hive.breed,
                    role = role ?: hive.role,

                    // Frame Counts
                    framesEggs = framesEggs ?: hive.framesEggs,
                    framesOpenBrood = framesOpenBrood ?: hive.framesOpenBrood,
                    framesCappedBrood = framesCappedBrood ?: hive.framesCappedBrood,
                    framesFeed = framesFeed ?: hive.framesFeed,
                    framesTotal = framesTotal ?: hive.framesTotal,

                    // Queen Bee
                    queenTagColor = queenTagColor ?: hive.queenTagColor,
                    queenNumber = queenNumber ?: hive.queenNumber,
                    queenYear = queenYear ?: hive.queenYear,
                    queenLine = queenLine ?: hive.queenLine,
                    isolationFromDate = isolationFromDate ?: hive.isolationFromDate,
                    isolationToDate = isolationToDate ?: hive.isolationToDate,

                    // Other attributes
                    defensivenessRating = defensivenessRating ?: hive.defensivenessRating,

                    // Given/Taken
                    givenBuiltCombs = givenBuiltCombs ?: hive.givenBuiltCombs,
                    givenFoundation = givenFoundation ?: hive.givenFoundation,
                    givenBrood = givenBrood ?: hive.givenBrood,
                    givenBeesKg = givenBeesKg ?: hive.givenBeesKg,
                    givenHoneyKg = givenHoneyKg ?: hive.givenHoneyKg,
                    givenSugarKg = givenSugarKg ?: hive.givenSugarKg,

                    // Treatment
                    treatment = treatment ?: hive.treatment,

                    // General
                    lastInspectionDate = lastInspectionDate ?: hive.lastInspectionDate,
                    notes = notes ?: hive.notes,

                    // Numbering - only applied if autoNumber is checked
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
