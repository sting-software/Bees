package com.stingsoftware.pasika.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hives",
    foreignKeys = [ForeignKey(
        entity = Apiary::class,
        parentColumns = ["id"],
        childColumns = ["apiaryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["apiaryId"])]
)
data class Hive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val apiaryId: Long,

    // Core Hive Identification (Static)
    val hiveNumber: String?,
    val hiveType: String?,
    val hiveTypeOther: String? = null,
    val frameType: String?,
    val frameTypeOther: String? = null,
    val material: String?,
    val materialOther: String? = null,
    val breed: String?,
    val breedOther: String? = null,
    val notes: String? = null,
    val role: HiveRole = HiveRole.PRODUCTION,

    // Queen Information (Slowly Changing)
    val queenTagColor: String? = null,
    val queenTagColorOther: String? = null,
    val queenNumber: String? = null,
    val queenYear: String? = null,
    val queenLine: String? = null,
    val isolationFromDate: Long? = null,
    val isolationToDate: Long? = null
)
