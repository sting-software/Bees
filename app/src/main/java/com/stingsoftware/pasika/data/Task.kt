package com.stingsoftware.pasika.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = false
)