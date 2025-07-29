package com.stingsoftware.pasika.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.repository.ApiaryRepository
import com.stingsoftware.pasika.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditApiaryViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

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
        try {
            if (isNewApiary) {
                val newApiaryId = repository.insertApiary(apiary)

                if (autoNumberHives && startingHiveNumber != null && endingHiveNumber != null) {
                    for (i in startingHiveNumber..endingHiveNumber) {
                        repository.insertHive(
                            createEmptyHive(
                                newApiaryId,
                                i.toString()
                            )
                        )
                    }
                } else if (!autoNumberHives && apiary.numberOfHives > 0) {
                    repeat(apiary.numberOfHives) {
                        repository.insertHive(
                            createEmptyHive(
                                newApiaryId,
                                null
                            )
                        )
                    }
                }
                repository.updateApiaryHiveCount(newApiaryId)
                _navigateToDetail.postValue(newApiaryId)
            } else {
                repository.updateApiary(apiary)
                repository.updateApiaryHiveCount(apiary.id)
            }
            _saveStatus.postValue(Resource.Success(apiary))
        } catch (e: Exception) {
            _saveStatus.postValue(
                Resource.Error(
                    e.message ?: context.getString(R.string.error_unknown)
                )
            )
        }
    }

    private fun createEmptyHive(
        apiaryId: Long,
        hiveNumber: String?
    ): Hive {
        return Hive(
            apiaryId = apiaryId,
            hiveNumber = hiveNumber,
            hiveType = null,
            hiveTypeOther = null,
            frameType = null,
            frameTypeOther = null,
            material = null,
            materialOther = null,
            breed = null,
            breedOther = null,
            notes = null,
            queenTagColor = null,
            queenTagColorOther = null,
            queenNumber = null,
            queenYear = null,
            queenLine = null,
            isolationFromDate = null,
            isolationToDate = null
        )
    }
}
