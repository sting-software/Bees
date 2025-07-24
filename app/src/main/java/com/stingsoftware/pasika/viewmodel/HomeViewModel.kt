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
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.ApiaryExportData
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
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

    /**
     * NEW: Saves the final order of apiaries after a drag-and-drop operation is complete.
     */
    fun saveApiaryOrder(reorderedApiaries: List<Apiary>) {
        viewModelScope.launch {
            // Re-assign the displayOrder to every item based on its new position
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
                val type = object : TypeToken<ApiaryExportData>() {}.type
                val exportData: ApiaryExportData = Gson().fromJson(jsonString, type)

                repository.importApiaryData(exportData)
                _importStatus.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _importStatus.postValue(false)
            }
        }
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
