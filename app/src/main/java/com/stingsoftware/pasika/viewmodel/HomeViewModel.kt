package com.stingsoftware.pasika.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stingsoftware.pasika.data.*
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _importStatus = MutableLiveData<Boolean?>()
    val importStatus: LiveData<Boolean?> = _importStatus

    private val _searchQuery = savedStateHandle.getLiveData<String?>("searchQuery", null)

    val allApiaries: LiveData<List<Apiary>> = repository.allApiaries.asLiveData()

    val filteredApiaries: LiveData<List<Apiary>> = MediatorLiveData<List<Apiary>>().apply {
        var latestApiaries: List<Apiary>? = null
        var latestSearchQuery: String? = null

        addSource(allApiaries) { apiaries ->
            latestApiaries = apiaries
            value = filterApiaries(latestApiaries, latestSearchQuery)
        }

        addSource(_searchQuery) { query ->
            latestSearchQuery = query
            value = filterApiaries(latestApiaries, latestSearchQuery)
        }
    }

    fun saveApiaryOrder(reorderedApiaries: List<Apiary>) {
        viewModelScope.launch {
            val updatedApiaries = reorderedApiaries.mapIndexed { index, apiary ->
                apiary.copy(displayOrder = index)
            }
            repository.updateApiaries(updatedApiaries)
        }
    }

    fun importApiaryFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = readTextFromUri(context, uri)
                val legacyDataType = object : TypeToken<LegacyApiaryExportData>() {}.type
                val legacyExportData: LegacyApiaryExportData =
                    Gson().fromJson(jsonString, legacyDataType)

                // Convert the legacy data structure to the new one
                val newExportData = mapLegacyDataToNewData(legacyExportData)

                repository.importApiaryData(newExportData)
                _importStatus.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _importStatus.postValue(false)
            }
        }
    }

    private fun mapLegacyDataToNewData(legacyData: LegacyApiaryExportData): ApiaryExportData {
        val newApiary = Apiary(
            id = 0,
            name = legacyData.apiary.name,
            location = legacyData.apiary.location,
            numberOfHives = legacyData.apiary.numberOfHives,
            type = try {
                // Make the enum parsing case-insensitive for robustness
                ApiaryType.valueOf(legacyData.apiary.type.uppercase(Locale.ROOT))
            } catch (e: Exception) {
                ApiaryType.STATIONARY // Default value if the type is invalid or missing
            },
            notes = legacyData.apiary.notes,
            displayOrder = 0
        )

        val newHivesWithInspections = legacyData.hivesWithInspections.map { legacyHiveData ->
            val newHive = Hive(
                id = 0,
                apiaryId = 0,
                hiveNumber = legacyHiveData.hive.hiveNumber,
                hiveType = legacyHiveData.hive.hiveType,
                frameType = legacyHiveData.hive.frameType,
                material = legacyHiveData.hive.material,
                breed = legacyHiveData.hive.breed,
                notes = legacyHiveData.hive.notes,
                role = legacyHiveData.hive.role ?: HiveRole.PRODUCTION, // Provide default if null
                queenTagColor = legacyHiveData.hive.queenTagColor,
                queenNumber = legacyHiveData.hive.queenNumber,
                queenYear = legacyHiveData.hive.queenYear,
                queenLine = legacyHiveData.hive.queenLine,
                isolationFromDate = legacyHiveData.hive.isolationFromDate,
                isolationToDate = legacyHiveData.hive.isolationToDate
            )

            val newInspections = legacyHiveData.inspections.map { legacyInspection ->
                Inspection(
                    id = 0,
                    hiveId = 0,
                    inspectionDate = legacyInspection.inspectionDate,
                    queenCellsPresent = legacyInspection.queenCellsPresent,
                    queenCellsCount = legacyInspection.queenCellsCount,
                    framesEggsCount = legacyInspection.framesEggsCount,
                    framesOpenBroodCount = legacyInspection.framesOpenBroodCount,
                    framesCappedBroodCount = legacyInspection.framesCappedBroodCount,
                    framesHoneyCount = legacyInspection.framesHoneyCount
                        ?: legacyInspection.honeyStoresEstimateFrames,
                    framesPollenCount = legacyInspection.framesPollenCount
                        ?: legacyInspection.pollenStoresEstimateFrames,
                    pestsDiseasesObserved = legacyInspection.pestsDiseasesObserved,
                    treatment = legacyInspection.treatmentApplied,
                    defensivenessRating = legacyInspection.temperamentRating,
                    managementActionsTaken = legacyInspection.managementActionsTaken,
                    notes = legacyInspection.notes
                )
            }.toMutableList()

            // If the old hive has state data (e.g., from last inspection), create a new inspection for it
            if (legacyHiveData.inspections.isEmpty() && legacyHiveData.hive.lastInspectionDate != null) {
                val stateInspection = Inspection(
                    id = 0,
                    hiveId = 0,
                    inspectionDate = legacyHiveData.hive.lastInspectionDate!!,
                    defensivenessRating = legacyHiveData.hive.defensivenessRating,
                    treatment = legacyHiveData.hive.treatment,
                    givenBuiltCombs = legacyHiveData.hive.givenBuiltCombs,
                    givenFoundation = legacyHiveData.hive.givenFoundation,
                    givenBrood = legacyHiveData.hive.givenBrood,
                    givenBeesKg = legacyHiveData.hive.givenBeesKg,
                    givenHoneyKg = legacyHiveData.hive.givenHoneyKg,
                    givenSugarKg = legacyHiveData.hive.givenSugarKg,
                    framesEggsCount = legacyHiveData.hive.framesEggs,
                    framesOpenBroodCount = legacyHiveData.hive.framesOpenBrood,
                    framesCappedBroodCount = legacyHiveData.hive.framesCappedBrood,
                    framesHoneyCount = legacyHiveData.hive.framesFeed,
                    notes = "Imported from hive state"
                )
                newInspections.add(stateInspection)
            }

            HiveExportData(hive = newHive, inspections = newInspections)
        }

        return ApiaryExportData(apiary = newApiary, hivesWithInspections = newHivesWithInspections)
    }


    fun onImportStatusHandled() {
        _importStatus.value = null
    }

    private fun readTextFromUri(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
    }

    private fun filterApiaries(apiaries: List<Apiary>?, query: String?): List<Apiary> {
        if (apiaries.isNullOrEmpty()) {
            return emptyList()
        }
        return if (query.isNullOrBlank()) {
            apiaries
        } else {
            apiaries.filter { apiary ->
                apiary.name.contains(query, ignoreCase = true) ||
                        apiary.location.contains(query, ignoreCase = true)
            }
        }
    }

    fun deleteApiary(apiary: Apiary) = viewModelScope.launch {
        repository.deleteApiary(apiary)
    }
}
