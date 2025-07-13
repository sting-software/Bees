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

    @Query("UPDATE hives SET apiaryId = :newApiaryId WHERE id IN (:hiveIds)")
    suspend fun moveHives(hiveIds: List<Long>, newApiaryId: Long)

    // This function was missing and has been restored
    @Query("""
        SELECT breed,
               COUNT(id) as hiveCount,
               SUM(framesTotal) as totalFrames,
               AVG(defensivenessRating) as avgDefensiveness
        FROM hives
        WHERE breed IS NOT NULL AND breed != ''
        GROUP BY breed
        ORDER BY hiveCount DESC
    """)
    fun getStatsByBreed(): Flow<List<StatsByBreed>>
}