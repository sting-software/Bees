package com.stingsoftware.pasika.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the Apiary entity.
 * Defines methods for database operations related to Apiaries.
 */
@Dao
interface ApiaryDao {

    /**
     * Inserts a new apiary into the database.
     * If an apiary with the same primary key already exists, it will be replaced.
     * @param apiary The Apiary object to insert.
     * @return The row ID of the newly inserted apiary.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apiary: Apiary): Long

    /**
     * Updates an existing apiary in the database.
     * @param apiary The Apiary object to update.
     * @return The number of rows updated.
     */
    @Update
    suspend fun update(apiary: Apiary): Int

    /**
     * Deletes an apiary from the database.
     * @param apiary The Apiary object to delete.
     * @return The number of rows deleted.
     */
    @Delete
    suspend fun delete(apiary: Apiary): Int

    /**
     * Retrieves a specific apiary by its ID.
     * @param apiaryId The ID of the apiary to retrieve.
     * @return The Apiary object, or null if not found.
     */
    @Query("SELECT * FROM apiaries WHERE id = :apiaryId")
    suspend fun getApiaryById(apiaryId: Long): Apiary?

    /**
     * Retrieves all apiaries from the database, ordered by name.
     * Returns a Flow, which emits new lists of apiaries whenever the data changes.
     * This is ideal for observing real-time updates in the UI.
     * @return A Flow emitting a list of all Apiary objects.
     */
    @Query("SELECT * FROM apiaries ORDER BY name ASC")
    fun getAllApiaries(): Flow<List<Apiary>>

    /**
     * Retrieves the total number of apiaries in the database.
     * Returns a Flow for real-time updates.
     * @return A Flow emitting the count of apiaries.
     */
    @Query("SELECT COUNT(*) FROM apiaries")
    fun getTotalApiariesCount(): Flow<Int>

    /**
     * Retrieves the sum of all hives across all apiaries.
     * Returns a Flow for real-time updates.
     * @return A Flow emitting the total number of hives.
     */
    @Query("SELECT SUM(numberOfHives) FROM apiaries")
    fun getTotalHivesCount(): Flow<Int?> // Use Int? as SUM can return null if no rows

    @Query("SELECT * FROM apiaries WHERE id = :apiaryId")
    fun getApiaryFlowById(apiaryId: Long): Flow<Apiary?>
}
