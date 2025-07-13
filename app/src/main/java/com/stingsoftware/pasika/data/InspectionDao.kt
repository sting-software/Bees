package com.stingsoftware.pasika.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inspection: Inspection): Long

    @Update
    suspend fun update(inspection: Inspection): Int

    @Delete
    suspend fun delete(inspection: Inspection): Int

    @Query("SELECT * FROM inspections WHERE id = :inspectionId")
    suspend fun getInspectionById(inspectionId: Long): Inspection?

    @Query("SELECT * FROM inspections WHERE hiveId = :hiveId ORDER BY inspectionDate DESC")
    fun getInspectionsForHive(hiveId: Long): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE hiveId IN (:hiveIds)")
    suspend fun getInspectionsForHives(hiveIds: List<Long>): List<Inspection>

    @Query("DELETE FROM inspections WHERE hiveId = :hiveId")
    suspend fun deleteInspectionsForHive(hiveId: Long): Int

    @Query("SELECT COUNT(*) FROM inspections WHERE hiveId = :hiveId")
    suspend fun getInspectionCountForHive(hiveId: Long): Int
}