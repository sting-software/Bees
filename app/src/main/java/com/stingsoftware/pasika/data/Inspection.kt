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
    indices = [Index(value = ["hiveId"])]
)
data class Inspection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hiveId: Long,
    val inspectionDate: Long = System.currentTimeMillis(),

    // Queen & Brood Status
    val queenCellsPresent: Boolean? = null,
    val queenCellsCount: Int? = null,
    val framesEggsCount: Int? = null,
    val framesOpenBroodCount: Int? = null,
    val framesCappedBroodCount: Int? = null,
    val framesHoneyCount: Int? = null, // Renamed from framesFeed for clarity
    val framesPollenCount: Int? = null,

    // Health & Temperament
    val pestsDiseasesObserved: String? = null,
    val treatment: String? = null, // Renamed from treatmentApplied
    val defensivenessRating: Int? = null, // Renamed from temperamentRating

    // Management & Resources Given/Taken
    val managementActionsTaken: String? = null,
    val givenBuiltCombs: Int? = null,
    val givenFoundation: Int? = null,
    val givenBrood: Int? = null,
    val givenBeesKg: Double? = null,
    val givenHoneyKg: Double? = null,
    val givenSugarKg: Double? = null,

    // General Notes
    val notes: String? = null
)
