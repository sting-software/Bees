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
    indices = [Index(value = ["apiaryId"])] // Index added for performance
)
data class Hive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val apiaryId: Long,
    val hiveNumber: String?,
    val hiveType: String?,
    val hiveTypeOther: String? = null,
    val frameType: String?,
    val frameTypeOther: String? = null,
    val material: String?,
    val materialOther: String? = null,
    val breed: String?,
    val breedOther: String? = null,
    val lastInspectionDate: Long? = null,
    val notes: String? = null,
    val queenTagColor: String? = null,
    val queenTagColorOther: String? = null,
    val queenNumber: String? = null,
    val queenYear: String? = null,
    val queenLine: String? = null,
    val queenCells: Int? = null,
    val isolationFromDate: Long? = null,
    val isolationToDate: Long? = null,
    val defensivenessRating: Int? = null,
    val framesTotal: Int? = null,
    val framesEggs: Int? = null,
    val framesOpenBrood: Int? = null,
    val framesCappedBrood: Int? = null,
    val framesFeed: Int? = null,
    val givenBuiltCombs: Int? = null,
    val givenFoundation: Int? = null,
    val givenBrood: Int? = null,
    val givenBeesKg: Double? = null,
    val givenHoneyKg: Double? = null,
    val givenSugarKg: Double? = null,
    val treatment: String? = null
)
