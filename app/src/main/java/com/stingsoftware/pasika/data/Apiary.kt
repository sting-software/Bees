package com.stingsoftware.pasika.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enum for Apiary Type provides type safety
enum class ApiaryType {
    STATIONARY,
    MIGRATORY
}

@Entity(tableName = "apiaries")
data class Apiary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val location: String,
    val numberOfHives: Int,
    val type: ApiaryType,
    val lastInspectionDate: Long? = null,
    val notes: String? = null
)
