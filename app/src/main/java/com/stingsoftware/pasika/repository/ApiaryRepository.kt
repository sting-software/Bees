package com.stingsoftware.pasika.repository

import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.ApiaryDao
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveDao
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.data.InspectionDao
import kotlinx.coroutines.flow.Flow

class ApiaryRepository(
    private val apiaryDao: ApiaryDao,
    private val hiveDao: HiveDao,
    private val inspectionDao: InspectionDao
) {

    // ... (existing properties and functions)

    val allApiaries: Flow<List<Apiary>> = apiaryDao.getAllApiaries()
    val totalApiariesCount: Flow<Int> = apiaryDao.getTotalApiariesCount()
    val totalHivesCount: Flow<Int?> = apiaryDao.getTotalHivesCount()

    suspend fun insertApiary(apiary: Apiary): Long { //...
    }
    suspend fun updateApiary(apiary: Apiary): Int { //...
    }
    suspend fun deleteApiary(apiary: Apiary) { //...
    }
    suspend fun getApiaryById(apiaryId: Long): Apiary? { //...
    }
    suspend fun updateApiaryHiveCount(apiaryId: Long) { //...
    }
    suspend fun insertHive(hive: Hive): Long { //...
    }
    suspend fun updateHive(hive: Hive) { //...
    }
    suspend fun deleteHive(hive: Hive) { //...
    }
    suspend fun getHiveById(hiveId: Long): Hive? { //...
    }
    fun getHivesForApiary(apiaryId: Long): Flow<List<Hive>> { //...
    }
    suspend fun insertInspection(inspection: Inspection): Long { //...
    }
    suspend fun updateInspection(inspection: Inspection): Int { //...
    }
    suspend fun deleteInspection(inspection: Inspection): Int { //...
    }
    suspend fun getInspectionById(inspectionId: Long): Inspection? { //...
    }
    fun getInspectionsForHive(hiveId: Long): Flow<List<Inspection>> { //...
    }
    suspend fun getInspectionCountForHive(hiveId: Long): Int { //...
    }

    // --- NEW: Function to handle moving hives and updating counts ---
    suspend fun moveHives(hiveIds: List<Long>, sourceApiaryId: Long, destinationApiaryId: Long) {
        hiveDao.moveHives(hiveIds, destinationApiaryId)
        // Update counts for both the old and new apiaries
        updateApiaryHiveCount(sourceApiaryId)
        updateApiaryHiveCount(destinationApiaryId)
    }
}