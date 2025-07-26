package com.stingsoftware.pasika.ui.queenrearing.batches

import androidx.lifecycle.*
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

    init {
        if (batchId != null && batchId != -1L) {
            viewModelScope.launch {
                _batch.value = repository.getGraftingBatchById(batchId).first()
            }
        }
    }

    fun saveBatch(batch: GraftingBatch, cellCount: Int) {
        viewModelScope.launch {
            if (batch.id == 0L) {
                repository.insertGraftingBatchAndTasks(batch, cellCount)
            } else {
                repository.updateGraftingBatch(batch)
            }
        }
    }
}
