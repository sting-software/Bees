package com.stingsoftware.pasika.ui.queenrearing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueenRearingViewModel @Inject constructor(
    private val repository: ApiaryRepository
) : ViewModel() {

    val graftingBatches = repository.getAllGraftingBatches().asLiveData()
    val queenRearingTasks = repository.getQueenRearingTasks().asLiveData()

    fun getMotherColonies() = repository.getHivesByRole(HiveRole.MOTHER).asLiveData()
    fun getStarterColonies() = repository.getHivesByRole(HiveRole.STARTER).asLiveData()
    fun getFinisherColonies() = repository.getHivesByRole(HiveRole.FINISHER).asLiveData()
    fun getNucleusColonies() = repository.getHivesByRole(HiveRole.NUCLEUS).asLiveData()

    fun deleteGraftingBatches(batches: List<GraftingBatch>) {
        viewModelScope.launch {
            repository.deleteGraftingBatchesAndTasks(batches)
        }
    }
}
