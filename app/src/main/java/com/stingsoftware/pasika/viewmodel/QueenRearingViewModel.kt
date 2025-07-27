package com.stingsoftware.pasika.ui.queenrearing

import androidx.lifecycle.*
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QueenRearingViewModel @Inject constructor(
    private val repository: ApiaryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val graftingBatches: LiveData<List<GraftingBatch>> = searchQuery.flatMapLatest { query ->
        // This would need to be updated if you want to search batches
        repository.getAllGraftingBatches()
    }.asLiveData()

    val queenRearingTasks: LiveData<List<Task>> = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getQueenRearingTasks()
        } else {
            repository.searchQueenRearingTasks(query)
        }
    }.asLiveData()

    fun setSearchQuery(query: String?) {
        searchQuery.value = query ?: ""
    }

    fun deleteGraftingBatches(batches: List<GraftingBatch>) = viewModelScope.launch {
        repository.deleteGraftingBatchesAndTasks(batches)
    }

    fun getMotherColonies(): LiveData<List<Hive>> = repository.getHivesByRole(HiveRole.MOTHER).asLiveData()
    fun getStarterColonies(): LiveData<List<Hive>> = repository.getHivesByRole(HiveRole.STARTER).asLiveData()
    fun getFinisherColonies(): LiveData<List<Hive>> = repository.getHivesByRole(HiveRole.FINISHER).asLiveData()
    fun getNucleusColonies(): LiveData<List<Hive>> = repository.getHivesByRole(HiveRole.NUCLEUS).asLiveData()

    val anyBatchUsesStarter: LiveData<Boolean> = repository.anyBatchUsesStarter().asLiveData()
}
