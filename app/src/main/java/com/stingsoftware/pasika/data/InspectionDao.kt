package com.stingsoftware.pasika.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the Inspection entity.
 * Defines methods for database operations related to Hive Inspections.
 */
@Dao
interface InspectionDao {

    /**
     * Inserts a new inspection record into the database.
     * If an inspection with the same primary key already exists, it will be replaced.
     * @param inspection The Inspection object to insert.
     * @return The row ID of the newly inserted inspection.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inspection: Inspection): Long

    /**
     * Updates an existing inspection record in the database.
     * @param inspection The Inspection object to update.
     * @return The number of rows updated.
     */
    @Update
    suspend fun update(inspection: Inspection): Int

    /**
     * Deletes an inspection record from the database.
     * @param inspection The Inspection object to delete.
     * @return The number of rows deleted.
     */
    @Delete
    suspend fun delete(inspection: Inspection): Int

    /**
     * Retrieves a specific inspection record by its ID.
     * @param inspectionId The ID of the inspection to retrieve.
     * @return The Inspection object, or null if not found.
     */
    @Query("SELECT * FROM inspections WHERE id = :inspectionId")
    suspend fun getInspectionById(inspectionId: Long): Inspection?

    /**
     * Retrieves all inspection records for a specific hive, ordered by inspection date (newest first).
     * Returns a Flow, which emits new lists of inspections whenever the data changes.
     * @param hiveId The ID of the parent hive.
     * @return A Flow emitting a list of Inspection objects for the given hive.
     */
    @Query("SELECT * FROM inspections WHERE hiveId = :hiveId ORDER BY inspectionDate DESC")
    fun getInspectionsForHive(hiveId: Long): Flow<List<Inspection>>

    /**
     * Deletes all inspection records associated with a specific hive.
     * This might be used if a hive is deleted, though ForeignKey.CASCADE handles this.
     * @param hiveId The ID of the hive whose inspections should be deleted.
     * @return The number of rows deleted.
     */
    @Query("DELETE FROM inspections WHERE hiveId = :hiveId")
    suspend fun deleteInspectionsForHive(hiveId: Long): Int

    /**
     * Retrieves the count of inspections for a specific hive.
     * @param hiveId The ID of the parent hive.
     * @return The number of inspections for the given hive.
     */
    @Query("SELECT COUNT(*) FROM inspections WHERE hiveId = :hiveId")
    suspend fun getInspectionCountForHive(hiveId: Long): Int
}
