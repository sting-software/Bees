package com.stingsoftware.pasika.ui.queenrearing.batches

import androidx.lifecycle.*
import com.stingsoftware.pasika.data.CustomTask
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditGraftingBatchViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val motherColonies = repository.getHivesByRole(HiveRole.MOTHER).asLiveData()

    private val batchId = savedStateHandle.get<Long>("batchId")
    private val _batch = MutableLiveData<GraftingBatch?>()
    val batch: LiveData<GraftingBatch?> = _batch

    private val _customTasks = MutableLiveData<List<CustomTask>>(emptyList())
    val customTasks: LiveData<List<CustomTask>> = _customTasks

    init {
        if (batchId != null && batchId != -1L) {
            viewModelScope.launch {
                _batch.value = repository.getGraftingBatchById(batchId).first()
            }
        }
    }

    fun addCustomTask(task: CustomTask) {
        val currentTasks = _customTasks.value ?: emptyList()
        _customTasks.value = currentTasks + task
    }

    fun removeCustomTask(task: CustomTask) {
        val currentTasks = _customTasks.value ?: emptyList()
        _customTasks.value = currentTasks - task
    }

    fun saveBatch(batch: GraftingBatch, customTasks: List<CustomTask>) {
        viewModelScope.launch {
            if (batch.id == 0L) {
                repository.insertGraftingBatchAndTasks(batch, customTasks)
            } else {
                repository.updateGraftingBatch(batch)
                // Note: Updating custom tasks for an existing batch is not implemented here.
            }
        }
    }
}
