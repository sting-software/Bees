package com.stingsoftware.pasika.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HiveDao {

    // ... (existing functions: insert, update, delete, etc.)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hive: Hive): Long

    @Update
    suspend fun update(hive: Hive): Int

    @Delete
    suspend fun delete(hive: Hive): Int

    @Query("SELECT * FROM hives WHERE id = :hiveId")
    suspend fun getHiveById(hiveId: Long): Hive?

    @Query("SELECT * FROM hives WHERE apiaryId = :apiaryId ORDER BY hiveType ASC")
    fun getHivesForApiary(apiaryId: Long): Flow<List<Hive>>

    @Query("DELETE FROM hives WHERE apiaryId = :apiaryId")
    suspend fun deleteHivesForApiary(apiaryId: Long): Int

    @Query("SELECT COUNT(*) FROM hives WHERE apiaryId = :apiaryId")
    suspend fun getHiveCountForApiary(apiaryId: Long): Int

    // --- NEW: Function to move hives ---
    @Query("UPDATE hives SET apiaryId = :newApiaryId WHERE id IN (:hiveIds)")
    suspend fun moveHives(hiveIds: List<Long>, newApiaryId: Long)
}