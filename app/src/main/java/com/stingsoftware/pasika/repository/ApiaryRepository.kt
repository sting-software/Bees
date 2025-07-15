package com.stingsoftware.pasika.repository

import android.content.Context
import androidx.room.Transaction
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiaryRepository @Inject constructor(
    private val apiaryDao: ApiaryDao,
    private val hiveDao: HiveDao,
    private val inspectionDao: InspectionDao,
    private val taskDao: TaskDao,
    private val context: Context
) {

    val allApiaries: Flow<List<Apiary>> = apiaryDao.getAllApiaries()

    // Apiary operations
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

    suspend fun updateApiaryHiveCount(apiaryId: Long) {
        val currentApiary = apiaryDao.getApiaryById(apiaryId)
        currentApiary?.let { apiary ->
            val hiveCount = hiveDao.getHiveCountForApiary(apiaryId)
            if (apiary.numberOfHives != hiveCount) {
                apiaryDao.update(apiary.copy(numberOfHives = hiveCount))
            }
        }
    }

    // Hive operations
    suspend fun insertHive(hive: Hive): Long {
        return hiveDao.insert(hive)
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

    suspend fun moveHives(hiveIds: List<Long>, sourceApiaryId: Long, destinationApiaryId: Long) {
        val sourceApiaryName = apiaryDao.getApiaryById(sourceApiaryId)?.name ?: context.getString(R.string.unknown_apiary)
        val destinationApiaryName = apiaryDao.getApiaryById(destinationApiaryId)?.name ?: context.getString(
            R.string.unknown_apiary
        )
        val note =
            context.getString(R.string.message_hive_moved_from_to, sourceApiaryName, destinationApiaryName)

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

    // Inspection operations
    suspend fun insertInspection(inspection: Inspection): Long {
        return inspectionDao.insert(inspection)
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

    // Export/Import operations
    suspend fun getApiaryExportData(apiaryId: Long): ApiaryExportData? {
        val apiary = apiaryDao.getApiaryById(apiaryId) ?: return null
        val hives = hiveDao.getHivesForApiary(apiaryId).first()
        val hiveIds = hives.map { it.id }
        val inspections = inspectionDao.getInspectionsForHives(hiveIds)

        val inspectionsByHiveId = inspections.groupBy { it.hiveId }

        val hivesWithInspections = hives.map { hive ->
            HiveExportData(hive = hive, inspections = inspectionsByHiveId[hive.id] ?: emptyList())
        }

        return ApiaryExportData(apiary = apiary, hivesWithInspections = hivesWithInspections)
    }

    @Transaction
    suspend fun importApiaryData(data: ApiaryExportData) {
        val importedApiary = data.apiary.copy(id = 0, name = context.getString(
            R.string.imported_label_format,
            data.apiary.name
        ))
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

    // Task operations
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    suspend fun markTasksAsCompleted(taskIds: List<Long>) {
        taskDao.markTasksAsCompleted(taskIds)
    }

    suspend fun markTasksAsIncomplete(taskIds: List<Long>) {
        taskDao.markTasksAsIncomplete(taskIds)
    }
}
