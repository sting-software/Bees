package com.stingsoftware.pasika.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QueenRearingDao {

    // --- GraftingBatch Queries ---
    @Insert
    suspend fun insertGraftingBatch(batch: GraftingBatch): Long

    @Update
    suspend fun updateGraftingBatch(batch: GraftingBatch)

    @Query("SELECT * FROM grafting_batches ORDER BY graftingDate DESC")
    fun getAllGraftingBatches(): Flow<List<GraftingBatch>>

    @Query("SELECT * FROM grafting_batches WHERE id = :batchId")
    fun getGraftingBatchById(batchId: Long): Flow<GraftingBatch>

    @Query("DELETE FROM grafting_batches WHERE id IN (:batchIds)")
    suspend fun deleteGraftingBatches(batchIds: List<Long>)

    @Query("DELETE FROM tasks WHERE graftingBatchId IN (:batchIds)")
    suspend fun deleteTasksForBatches(batchIds: List<Long>)

    /**
     * Checks if there is at least one grafting batch that uses a starter colony.
     * @return A Flow emitting true if at least one batch uses a starter, false otherwise.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM grafting_batches WHERE useStarterColony = 1)")
    fun anyBatchUsesStarter(): Flow<Boolean>

    // --- QueenCell Queries ---
    @Insert
    suspend fun insertQueenCells(cells: List<QueenCell>)

    @Update
    suspend fun updateQueenCell(cell: QueenCell)

    @Update
    suspend fun updateQueenCells(cells: List<QueenCell>)

    @Delete
    suspend fun deleteQueenCells(cells: List<QueenCell>)

    @Query("SELECT * FROM queen_cells WHERE batchId = :batchId")
    fun getQueenCellsForBatch(batchId: Long): Flow<List<QueenCell>>

    @Query("DELETE FROM queen_cells WHERE batchId IN (:batchIds)")
    suspend fun deleteQueenCellsForBatches(batchIds: List<Long>)

    @Query("SELECT * FROM queen_cells")
    fun getAllQueenCells(): Flow<List<QueenCell>>

    @Transaction
    suspend fun deleteBatchesAndDependencies(batchIds: List<Long>) {
        deleteQueenCellsForBatches(batchIds)
        deleteTasksForBatches(batchIds)
        deleteGraftingBatches(batchIds)
    }
}
