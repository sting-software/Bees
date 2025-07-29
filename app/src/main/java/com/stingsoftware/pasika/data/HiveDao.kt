package com.stingsoftware.pasika.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HiveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hive: Hive): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(hives: List<Hive>)

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

    @Query("UPDATE hives SET apiaryId = :newApiaryId WHERE id IN (:hiveIds)")
    suspend fun moveHives(hiveIds: List<Long>, newApiaryId: Long)

    @Query("SELECT * FROM hives WHERE role = :role")
    fun getHivesByRole(role: HiveRole): Flow<List<Hive>>
}
