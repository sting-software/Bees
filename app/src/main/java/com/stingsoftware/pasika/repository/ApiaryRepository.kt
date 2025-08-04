package com.stingsoftware.pasika.repository

import android.content.Context
import androidx.room.Transaction
import androidx.room.withTransaction
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class ApiaryRepository @Inject constructor(
    private val db: AppDatabase,
    private val apiaryDao: ApiaryDao,
    private val hiveDao: HiveDao,
    private val inspectionDao: InspectionDao,
    private val taskDao: TaskDao,
    private val queenRearingDao: QueenRearingDao,
    @param:ApplicationContext private val context: Context
) {
    // --- Queen Rearing Methods ---
    fun getAllGraftingBatches(): Flow<List<GraftingBatch>> = queenRearingDao.getAllGraftingBatches()

    suspend fun updateGraftingBatch(batch: GraftingBatch) =
        queenRearingDao.updateGraftingBatch(batch)


    fun getQueenCellsForBatch(batchId: Long): Flow<List<QueenCell>> =
        queenRearingDao.getQueenCellsForBatch(batchId)

    fun getHivesByRole(role: HiveRole): Flow<List<Hive>> = hiveDao.getHivesByRole(role)
    fun getGraftingBatchById(batchId: Long): Flow<GraftingBatch> =
        queenRearingDao.getGraftingBatchById(batchId)

    suspend fun updateQueenCell(cell: QueenCell) = queenRearingDao.updateQueenCell(cell)
    suspend fun updateQueenCells(cells: List<QueenCell>) = queenRearingDao.updateQueenCells(cells)

    suspend fun deleteQueenCells(cells: List<QueenCell>) = queenRearingDao.deleteQueenCells(cells)

    fun getQueenRearingTasks(): Flow<List<Task>> = taskDao.getQueenRearingTasks()
    fun searchQueenRearingTasks(query: String): Flow<List<Task>> = taskDao.searchQueenRearingTasks(query)

    fun getAllQueenCells(): Flow<List<QueenCell>> = queenRearingDao.getAllQueenCells()

    fun anyBatchUsesStarter(): Flow<Boolean> = queenRearingDao.anyBatchUsesStarter()

    suspend fun insertGraftingBatchAndTasks(batch: GraftingBatch, customTasks: List<CustomTask>) {
        db.withTransaction {
            val batchId = queenRearingDao.insertGraftingBatch(batch)
            val cells = (1..batch.cellsGrafted).map { QueenCell(batchId = batchId) }
            queenRearingDao.insertQueenCells(cells)

            val graftingTime = batch.graftingDate
            val oneDayInMillis = 24 * 60 * 60 * 1000L

            if (batch.useStarterColony) {
                val checkAcceptanceTask = Task(
                    title = context.getString(R.string.check_acceptance_for, batch.name),
                    description = context.getString(R.string.check_how_many_cells_were_accepted_in_the_starter_colony),
                    dueDate = graftingTime + (1 * oneDayInMillis),
                    graftingBatchId = batchId,
                    reminderEnabled = true
                )

                val moveToFinisherTask = Task(
                    title = context.getString(R.string.move_cells_for_to_finisher, batch.name),
                    description = context.getString(R.string.check_for_capped_cells_and_move_the_cell_bar_to_a_finisher_colony),
                    dueDate = graftingTime + (5 * oneDayInMillis),
                    graftingBatchId = batchId,
                    reminderEnabled = true
                )
                taskDao.insert(checkAcceptanceTask)
                taskDao.insert(moveToFinisherTask)
            }


            val emergenceTask = Task(
                title = context.getString(R.string.queens_emerge_for, batch.name),
                description = context.getString(R.string.queens_are_expected_to_emerge_prepare_mating_nucs),
                dueDate = graftingTime + (11 * oneDayInMillis),
                graftingBatchId = batchId,
                reminderEnabled = true
            )

            val checkLayingTask = Task(
                title = context.getString(R.string.check_for_laying_queens_from, batch.name),
                description = context.getString(R.string.check_the_mating_nucs_for_laying_queens),
                dueDate = graftingTime + (25 * oneDayInMillis),
                graftingBatchId = batchId,
                reminderEnabled = true
            )

            taskDao.insert(emergenceTask)
            taskDao.insert(checkLayingTask)

            customTasks.forEach { customTask ->
                val task = Task(
                    title = customTask.title,
                    description = context.getString(R.string.custom_task_for_batch, batch.name),
                    dueDate = graftingTime + (customTask.daysAfterGrafting * oneDayInMillis),
                    graftingBatchId = batchId,
                    reminderEnabled = true
                )
                taskDao.insert(task)
            }
        }
    }

    suspend fun deleteGraftingBatchesAndTasks(batches: List<GraftingBatch>) {
        val batchIds = batches.map { it.id }
        queenRearingDao.deleteBatchesAndDependencies(batchIds)
    }

    /**
     * NEW: Calculates detailed, stage-by-stage analytics for the entire queen rearing process.
     * This provides a much clearer view of where losses occur.
     */
    fun getQueenRearingAnalytics(): Flow<QueenRearingAnalytics> {
        return getAllQueenCells().map { cells ->
            if (cells.isEmpty()) return@map QueenRearingAnalytics()

            val totalGrafted = cells.size
            val statusCounts = cells.groupingBy { it.status }.eachCount()

            // Define success for each stage. A cell is considered successful for a stage
            // if its status is that stage or any subsequent non-failure stage.
            val acceptedCount = cells.count { it.status >= QueenCellStatus.ACCEPTED && it.status != QueenCellStatus.FAILED }
            val cappedCount = cells.count { it.status >= QueenCellStatus.CAPPED && it.status != QueenCellStatus.FAILED }
            val emergedCount = cells.count { it.status >= QueenCellStatus.EMERGED && it.status != QueenCellStatus.FAILED }
            val layingCount = cells.count { it.status == QueenCellStatus.LAYING || it.status == QueenCellStatus.SOLD }

            // Calculate performance for each stage based on the success of the previous one.
            val acceptance = StagePerformance(
                startingCount = totalGrafted,
                successCount = acceptedCount,
                successRate = (acceptedCount.toFloat() / totalGrafted) * 100
            )
            val capping = StagePerformance(
                startingCount = acceptedCount,
                successCount = cappedCount,
                successRate = if (acceptedCount > 0) (cappedCount.toFloat() / acceptedCount) * 100 else 0f
            )
            val emergence = StagePerformance(
                startingCount = cappedCount,
                successCount = emergedCount,
                successRate = if (cappedCount > 0) (emergedCount.toFloat() / cappedCount) * 100 else 0f
            )
            val mating = StagePerformance(
                startingCount = emergedCount,
                successCount = layingCount,
                successRate = if (emergedCount > 0) (layingCount.toFloat() / emergedCount) * 100 else 0f
            )

            val overallSuccessRate = (layingCount.toFloat() / totalGrafted) * 100

            QueenRearingAnalytics(
                totalGrafted = totalGrafted,
                acceptance = acceptance,
                capping = capping,
                emergence = emergence,
                mating = mating,
                overallSuccessRate = overallSuccessRate,
                cellStatusDistribution = statusCounts
            )
        }
    }

    // --- Original Apiary Methods ---
    val allApiaries: Flow<List<Apiary>> = apiaryDao.getAllApiaries()
    suspend fun insertApiary(apiary: Apiary): Long {
        return apiaryDao.insert(apiary)
    }

    suspend fun updateApiary(apiary: Apiary): Int {
        return apiaryDao.update(apiary)
    }

    suspend fun deleteApiary(apiary: Apiary) {
        apiaryDao.delete(apiary)
    }

    suspend fun getApiaryById(apiaryId: Long): Apiary? {
        return apiaryDao.getApiaryById(apiaryId)
    }

    suspend fun updateApiaries(apiaries: List<Apiary>) {
        apiaryDao.updateApiaries(apiaries)
    }

    suspend fun updateApiaryHiveCount(apiaryId: Long) {
        val currentApiary = apiaryDao.getApiaryById(apiaryId)
        currentApiary?.let { apiary ->
            val hiveCount = hiveDao.getHiveCountForApiary(apiaryId)
            if (apiary.numberOfHives != hiveCount) {
                apiaryDao.update(apiary.copy(numberOfHives = hiveCount))
            }
        }
    }

    fun getApiaryFlowById(apiaryId: Long): Flow<Apiary?> {
        return apiaryDao.getApiaryFlowById(apiaryId)
    }

    // --- Original Hive Methods (Corrected Structure) ---
    suspend fun insertHive(hive: Hive): Long {
        return hiveDao.insert(hive)
    }

    suspend fun insertHives(hives: List<Hive>) {
        hiveDao.insertAll(hives)
    }

    suspend fun updateHive(hive: Hive) {
        hiveDao.update(hive)
    }

    suspend fun deleteHive(hive: Hive) {
        hiveDao.delete(hive)
    }

    suspend fun getHiveById(hiveId: Long): Hive? {
        return hiveDao.getHiveById(hiveId)
    }

    fun getHivesForApiary(apiaryId: Long): Flow<List<Hive>> {
        return hiveDao.getHivesForApiary(apiaryId)
    }

    suspend fun moveHives(
        hiveIds: List<Long>,
        sourceApiaryId: Long,
        destinationApiaryId: Long
    ) {
        val sourceApiaryName = apiaryDao.getApiaryById(sourceApiaryId)?.name
            ?: context.getString(R.string.unknown_apiary)
        val destinationApiaryName =
            apiaryDao.getApiaryById(destinationApiaryId)?.name ?: context.getString(
                R.string.unknown_apiary
            )
        val note =
            context.getString(
                R.string.message_hive_moved_from_to,
                sourceApiaryName,
                destinationApiaryName
            )

        hiveIds.forEach { hiveId ->
            val moveRecord = Inspection(
                hiveId = hiveId,
                inspectionDate = System.currentTimeMillis(),
                notes = note,
                managementActionsTaken = context.getString(R.string.title_hive_relocation)
            )
            inspectionDao.insert(moveRecord)
        }

        hiveDao.moveHives(hiveIds, destinationApiaryId)
        updateApiaryHiveCount(sourceApiaryId)
        updateApiaryHiveCount(destinationApiaryId)
    }

    // --- Inspection Methods ---
    suspend fun insertInspection(inspection: Inspection) {
        inspectionDao.insert(inspection)
    }

    suspend fun updateInspection(inspection: Inspection): Int {
        return inspectionDao.update(inspection)
    }

    suspend fun deleteInspection(inspection: Inspection): Int {
        return inspectionDao.delete(inspection)
    }

    suspend fun getInspectionById(inspectionId: Long): Inspection? {
        return inspectionDao.getInspectionById(inspectionId)
    }

    fun getInspectionsForHive(hiveId: Long): Flow<List<Inspection>> {
        return inspectionDao.getInspectionsForHive(hiveId)
    }

    fun searchInspections(hiveId: Long, query: String): Flow<List<Inspection>> {
        return inspectionDao.searchInspections(hiveId, query)
    }

    // --- Original Export/Import Methods ---
    suspend fun getApiaryExportData(apiaryId: Long): ApiaryExportData? {
        val apiary = apiaryDao.getApiaryById(apiaryId) ?: return null
        val hives = hiveDao.getHivesForApiary(apiaryId).first()
        val hiveIds = hives.map { it.id }
        val inspections = inspectionDao.getInspectionsForHives(hiveIds)

        val inspectionsByHiveId = inspections.groupBy { it.hiveId }

        val hivesWithInspections = hives.map { hive ->
            HiveExportData(
                hive = hive,
                inspections = inspectionsByHiveId[hive.id] ?: emptyList()
            )
        }

        return ApiaryExportData(apiary = apiary, hivesWithInspections = hivesWithInspections)
    }

    @Transaction
    suspend fun importApiaryData(data: ApiaryExportData) {
        val importedApiary = data.apiary.copy(
            id = 0, name = context.getString(
                R.string.imported_label_format,
                data.apiary.name
            )
        )
        val newApiaryId = apiaryDao.insert(importedApiary)

        data.hivesWithInspections.forEach { hiveData ->
            val importedHive = hiveData.hive.copy(id = 0, apiaryId = newApiaryId)
            val newHiveId = hiveDao.insert(importedHive)

            val importedInspections = hiveData.inspections.map {
                it.copy(id = 0, hiveId = newHiveId)
            }
            importedInspections.forEach { inspectionDao.insert(it) }
        }
        updateApiaryHiveCount(newApiaryId)
    }

    // --- Task Methods ---
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)
    suspend fun insertTask(task: Task): Long = taskDao.insert(task)
    suspend fun updateTask(task: Task) = taskDao.update(task)
    suspend fun deleteTask(task: Task) = taskDao.delete(task)
    suspend fun deleteTasks(tasks: List<Task>) = taskDao.deleteTasks(tasks)
    suspend fun markTasksAsCompleted(taskIds: List<Long>) =
        taskDao.markTasksAsCompleted(taskIds)

    suspend fun markTasksAsIncomplete(taskIds: List<Long>) =
        taskDao.markTasksAsIncomplete(taskIds)
}
