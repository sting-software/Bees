package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(private val repository: ApiaryRepository) : ViewModel() {

    val totalApiariesCount = repository.totalApiariesCount.asLiveData()

    val totalHivesCount = repository.totalHivesCount.asLiveData()
}