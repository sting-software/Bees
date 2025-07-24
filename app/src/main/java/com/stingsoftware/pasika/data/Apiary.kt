package com.stingsoftware.pasika.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.annotation.StringRes
import com.stingsoftware.pasika.R

// Enum for Apiary Type provides type safety
enum class ApiaryType(@param:StringRes val stringResId: Int) {
    STATIONARY(R.string.apiary_type_stationary),
    MIGRATORY(R.string.apiary_type_migratory)
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
    val notes: String? = null,
    val displayOrder: Int
)
