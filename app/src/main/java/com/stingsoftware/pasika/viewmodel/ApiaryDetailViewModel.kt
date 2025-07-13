package com.stingsoftware.pasika.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.*
import com.google.gson.Gson
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ApiaryDetailViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val apiaryId: Long = savedStateHandle.get<Long>("apiaryId")!!

    // LiveData for export operation status
    private val _exportStatus = MutableLiveData<Boolean?>()
    val exportStatus: LiveData<Boolean?> = _exportStatus

    // LiveData to hold all apiaries for the move dialog
    val allApiaries: LiveData<List<Apiary>> = repository.allApiaries.asLiveData()

    // LiveData for move operation status
    private val _moveStatus = MutableLiveData<Boolean?>()
    val moveStatus: LiveData<Boolean?> = _moveStatus

    private val _hiveSearchQuery = MutableStateFlow<String?>(null)

    val apiary: LiveData<Apiary?> = liveData {
        emit(repository.getApiaryById(apiaryId))
    }

    val allHivesForApiary: LiveData<List<Hive>> = repository.getHivesForApiary(apiaryId).asLiveData()

    val filteredHivesForApiary: MediatorLiveData<List<Hive>> = MediatorLiveData()

    init {
        var latestHives: List<Hive>? = null
        var latestSearchQuery: String? = null

        filteredHivesForApiary.addSource(allHivesForApiary) { hives ->
            latestHives = hives
            filteredHivesForApiary.value = filterHives(latestHives, latestSearchQuery)
        }

        filteredHivesForApiary.addSource(_hiveSearchQuery.asLiveData()) { query ->
            latestSearchQuery = query
            filteredHivesForApiary.value = filterHives(latestHives, latestSearchQuery)
        }
    }

    fun exportApiaryData(context: Context) {
        viewModelScope.launch {
            val exportData = repository.getApiaryExportData(apiaryId)
            if (exportData == null) {
                _exportStatus.postValue(false)
                return@launch
            }

            val gson = Gson()
            val jsonString = gson.toJson(exportData)
            val success = saveJsonToFile(context, "Apiary_${exportData.apiary.name}", jsonString)
            _exportStatus.postValue(success)
        }
    }

    fun onExportStatusHandled() {
        _exportStatus.value = null
    }

    private fun saveJsonToFile(context: Context, fileNamePrefix: String, jsonString: String): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val fileName = "${fileNamePrefix}_$timestamp.json"

        return try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Pasika")
                }
            }
            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: throw IOException("Failed to create new MediaStore entry for JSON.")

            contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
                ?: throw IOException("Failed to get output stream.")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun moveHives(hiveIds: List<Long>, destinationApiaryId: Long) {
        viewModelScope.launch {
            try {
                repository.moveHives(hiveIds, apiaryId, destinationApiaryId)
                _moveStatus.postValue(true)
            } catch (e: Exception) {
                _moveStatus.postValue(false)
            }
        }
    }

    fun onMoveStatusHandled() {
        _moveStatus.value = null
    }

    fun setHiveSearchQuery(query: String?) {
        _hiveSearchQuery.value = query
    }

    private fun filterHives(hives: List<Hive>?, query: String?): List<Hive> {
        if (hives.isNullOrEmpty()) {
            return emptyList()
        }
        return if (query.isNullOrBlank()) {
            hives
        } else {
            hives.filter { hive ->
                hive.hiveNumber?.contains(query, ignoreCase = true) == true ||
                        hive.notes?.contains(query, ignoreCase = true) == true
            }
        }
    }

    fun deleteHive(hive: Hive) = viewModelScope.launch {
        repository.deleteHive(hive)
        repository.updateApiaryHiveCount(hive.apiaryId)
    }
}