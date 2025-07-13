package com.stingsoftware.pasika.repository

import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.ApiaryDao
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveDao
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.data.InspectionDao
import com.stingsoftware.pasika.data.StatsByBreed
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiaryRepository @Inject constructor(
    private val apiaryDao: ApiaryDao,
    private val hiveDao: HiveDao,
    private val inspectionDao: InspectionDao
) {

    val allApiaries: Flow<List<Apiary>> = apiaryDao.getAllApiaries()
    val totalApiariesCount: Flow<Int> = apiaryDao.getTotalApiariesCount()
    val totalHivesCount: Flow<Int?> = apiaryDao.getTotalHivesCount()

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

    suspend fun getInspectionCountForHive(hiveId: Long): Int {
        return inspectionDao.getInspectionCountForHive(hiveId)
    }

    // Stats operations
    fun getStatsByBreed(): Flow<List<StatsByBreed>> {
        return hiveDao.getStatsByBreed()
    }
}