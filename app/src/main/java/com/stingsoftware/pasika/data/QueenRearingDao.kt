package com.stingsoftware.pasika.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Query("DELETE FROM grafting_batches WHERE id = :batchId")
    suspend fun deleteGraftingBatch(batchId: Long)

    // --- QueenCell Queries ---

    @Insert
    suspend fun insertQueenCells(cells: List<QueenCell>)

    @Update
    suspend fun updateQueenCell(cell: QueenCell)

    @Query("SELECT * FROM queen_cells WHERE batchId = :batchId")
    fun getQueenCellsForBatch(batchId: Long): Flow<List<QueenCell>>

    @Query("DELETE FROM queen_cells WHERE batchId = :batchId")
    suspend fun deleteQueenCellsForBatch(batchId: Long)

    @Query("SELECT * FROM queen_cells")
    fun getAllQueenCells(): Flow<List<QueenCell>>
}
