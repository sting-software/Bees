package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    private val savedStateHandle: SavedStateHandle // Use SavedStateHandle for process death restoration
) : ViewModel() {

    // Use SavedStateHandle to save and restore the search query
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