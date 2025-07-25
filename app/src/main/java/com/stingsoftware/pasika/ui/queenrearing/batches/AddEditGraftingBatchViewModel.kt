package com.stingsoftware.pasika.ui.queenrearing.batches

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AddEditGraftingBatchViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val motherColonies = repository.getHivesByRole(HiveRole.MOTHER).asLiveData()

    fun saveBatch(batch: GraftingBatch, cellCount: Int) {
        viewModelScope.launch {
            val batchId = repository.insertGraftingBatch(batch)
            val cells = (1..cellCount).map {
                QueenCell(batchId = batchId)
            }
            repository.insertQueenCells(cells)

            createAndSaveTasksForBatch(batch, batchId)
        }
    }

    private suspend fun createAndSaveTasksForBatch(batch: GraftingBatch, batchId: Long) {
        val calendar = Calendar.getInstance().apply { timeInMillis = batch.graftingDate }

        // Task: Check cell acceptance (+1 day)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val checkAcceptanceTask = Task(
            title = context.getString(R.string.check_acceptance_for, batch.name),
            description = context.getString(R.string.check_how_many_cells_were_accepted_in_the_starter_colony),
            dueDate = calendar.timeInMillis,
            graftingBatchId = batchId
        )

        // Task: Move cells to finisher (+4 days from acceptance check, so +5 total)
        calendar.add(Calendar.DAY_OF_YEAR, 4)
        val moveToFinisherTask = Task(
            title = context.getString(R.string.move_cells_for_to_finisher, batch.name),
            description = context.getString(R.string.check_for_capped_cells_and_move_the_cell_bar_to_a_finisher_colony),
            dueDate = calendar.timeInMillis,
            graftingBatchId = batchId
        )

        // Task: Queen emergence (+6 days from move, so +11 total)
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val emergenceTask = Task(
            title = context.getString(R.string.queens_emerge_for, batch.name),
            description = context.getString(R.string.queens_are_expected_to_emerge_prepare_mating_nucs),
            dueDate = calendar.timeInMillis,
            graftingBatchId = batchId
        )

        // Task: Check for laying queen (+14 days from emergence, so +25 total)
        calendar.add(Calendar.DAY_OF_YEAR, 14)
        val checkLayingTask = Task(
            title = context.getString(R.string.check_for_laying_queens_from, batch.name),
            description = context.getString(R.string.check_the_mating_nucs_for_laying_queens),
            dueDate = calendar.timeInMillis,
            graftingBatchId = batchId
        )

        repository.insertTask(checkAcceptanceTask)
        repository.insertTask(moveToFinisherTask)
        repository.insertTask(emergenceTask)
        repository.insertTask(checkLayingTask)
    }
}
