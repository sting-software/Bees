package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.ApiaryType
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.repository.ApiaryRepository
import com.stingsoftware.pasika.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditApiaryViewModel @Inject constructor(private val repository: ApiaryRepository) : ViewModel() {

    private val _saveStatus = MutableLiveData<Resource<Apiary>>()
    val saveStatus: LiveData<Resource<Apiary>> = _saveStatus

    private val _navigateToDetail = MutableLiveData<Long?>()
    val navigateToDetail: LiveData<Long?> = _navigateToDetail

    fun navigationCompleted() {
        _navigateToDetail.value = null
    }

    fun getApiary(apiaryId: Long): LiveData<Apiary?> = liveData {
        emit(repository.getApiaryById(apiaryId))
    }

    fun saveOrUpdateApiary(
        apiary: Apiary,
        isNewApiary: Boolean,
        autoNumberHives: Boolean,
        startingHiveNumber: Int?,
        endingHiveNumber: Int?
    ) = viewModelScope.launch {
        _saveStatus.postValue(Resource.Loading())

        if (apiary.name.isBlank()) {
            _saveStatus.postValue(Resource.Error("Apiary Name cannot be empty."))
            return@launch
        }
        if (apiary.location.isBlank()) {
            _saveStatus.postValue(Resource.Error("Location cannot be empty."))
            return@launch
        }

        try {
            if (isNewApiary) {
                val newApiaryId = repository.insertApiary(apiary)

                if (autoNumberHives && startingHiveNumber != null && endingHiveNumber != null) {
                    // Auto-numbering logic
                    for (i in startingHiveNumber..endingHiveNumber) {
                        repository.insertHive(createEmptyHive(newApiaryId, i.toString()))
                    }
                } else if (!autoNumberHives && apiary.numberOfHives > 0) {
                    // Manual quantity logic
                    for (i in 0 until apiary.numberOfHives) {
                        repository.insertHive(createEmptyHive(newApiaryId, null))
                    }
                }
                repository.updateApiaryHiveCount(newApiaryId)
                _navigateToDetail.postValue(newApiaryId)
            } else {
                // Update existing apiary
                repository.updateApiary(apiary)
                repository.updateApiaryHiveCount(apiary.id)
            }
            _saveStatus.postValue(Resource.Success(apiary))
        } catch (e: Exception) {
            _saveStatus.postValue(Resource.Error(e.message ?: "An unknown error occurred."))
        }
    }

    private fun createEmptyHive(apiaryId: Long, hiveNumber: String?): Hive {
        return Hive(
            apiaryId = apiaryId,
            hiveNumber = hiveNumber,
            hiveType = null, hiveTypeOther = null, frameType = null, frameTypeOther = null,
            material = null, materialOther = null, breed = null, breedOther = null,
            lastInspectionDate = null, notes = null, queenTagColor = null, queenTagColorOther = null,
            queenNumber = null, queenYear = null, queenLine = null, queenCells = null,
            isolationFromDate = null, isolationToDate = null, defensivenessRating = null,
            framesTotal = null, framesEggs = null, framesOpenBrood = null,
            framesCappedBrood = null, framesFeed = null, givenBuiltCombs = null,
            givenFoundation = null, givenBrood = null, givenBeesKg = null,
            givenHoneyKg = null, givenSugarKg = null, treatment = null
        )
    }
}