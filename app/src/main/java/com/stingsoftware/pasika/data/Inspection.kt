package com.stingsoftware.pasika.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inspections",
    foreignKeys = [ForeignKey(
        entity = Hive::class,
        parentColumns = ["id"],
        childColumns = ["hiveId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["hiveId"])] // Index added for performance
)
data class Inspection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hiveId: Long,
    val inspectionDate: Long = System.currentTimeMillis(),
    val queenCellsPresent: Boolean? = null,
    val queenCellsCount: Int? = null,
    val framesEggsCount: Int? = null,
    val framesOpenBroodCount: Int? = null,
    val framesCappedBroodCount: Int? = null,
    val framesHoneyCount: Int? = null,
    val framesPollenCount: Int? = null,
    val honeyStoresEstimateFrames: Int? = null,
    val pollenStoresEstimateFrames: Int? = null,
    val pestsDiseasesObserved: String? = null,
    val treatmentApplied: String? = null,
    val temperamentRating: Int? = null,
    val managementActionsTaken: String? = null,
    val notes: String? = null
)
