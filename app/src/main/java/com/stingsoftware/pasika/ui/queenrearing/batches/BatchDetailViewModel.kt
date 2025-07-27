package com.stingsoftware.pasika.ui.queenrearing.batches

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatchDetailViewModel @Inject constructor(
    private val repository: ApiaryRepository
) : ViewModel() {
    private val _motherHive = MutableLiveData<Hive?>()
    val motherHive: LiveData<Hive?> = _motherHive
    fun getBatch(batchId: Long) = repository.getGraftingBatchById(batchId).asLiveData()
    fun getQueenCells(batchId: Long) = repository.getQueenCellsForBatch(batchId).asLiveData()
    /**
     * Fetches the mother hive using a coroutine because the repository function is a suspend function.
     * The result is posted to the _motherHive LiveData.
     */
    fun fetchMotherHive(hiveId: Long) {
        viewModelScope.launch {
            _motherHive.value = repository.getHiveById(hiveId)
        }
    }
    fun updateQueenCell(cell: QueenCell) {
        viewModelScope.launch {
            repository.updateQueenCell(cell)
        }
    }
    fun updateQueenCells(cells: List<QueenCell>) {
        viewModelScope.launch {
            repository.updateQueenCells(cells)
        }
    }

    fun deleteQueenCells(cells: List<QueenCell>) {
        viewModelScope.launch {
            repository.deleteQueenCells(cells)
        }
    }
}
