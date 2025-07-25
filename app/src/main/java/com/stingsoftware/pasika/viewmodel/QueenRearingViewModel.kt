package com.stingsoftware.pasika.ui.queenrearing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QueenRearingViewModel @Inject constructor(
    private val repository: ApiaryRepository
) : ViewModel() {

    /**
     * A flow of all grafting batches, converted to LiveData so it can be observed by the UI.
     */
    val graftingBatches = repository.getAllGraftingBatches().asLiveData()

    /**
     * A flow of all tasks specifically related to queen rearing, ordered by due date.
     */
    val queenRearingTasks = repository.getQueenRearingTasks().asLiveData()

    /**
     * Fetches a flow of hives designated as MOTHER colonies.
     */
    fun getMotherColonies() = repository.getHivesByRole(HiveRole.MOTHER).asLiveData()

    /**
     * Fetches a flow of hives designated as STARTER colonies.
     */
    fun getStarterColonies() = repository.getHivesByRole(HiveRole.STARTER).asLiveData()

    /**
     * Fetches a flow of hives designated as FINISHER colonies.
     */
    fun getFinisherColonies() = repository.getHivesByRole(HiveRole.FINISHER).asLiveData()

    /**
     * Fetches a flow of hives designated as NUCLEUS colonies.
     */
    fun getNucleusColonies() = repository.getHivesByRole(HiveRole.NUCLEUS).asLiveData()
}
