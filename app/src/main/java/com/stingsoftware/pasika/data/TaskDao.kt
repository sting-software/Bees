package com.stingsoftware.pasika.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id IN (:taskIds)")
    suspend fun markTasksAsCompleted(taskIds: List<Long>)

    // NEW: Query to mark multiple tasks as incomplete
    @Query("UPDATE tasks SET isCompleted = 0 WHERE id IN (:taskIds)")
    suspend fun markTasksAsIncomplete(taskIds: List<Long>)

    @Query("SELECT * FROM tasks WHERE graftingBatchId IS NOT NULL ORDER BY dueDate ASC")
    fun getQueenRearingTasks(): Flow<List<Task>>
}
