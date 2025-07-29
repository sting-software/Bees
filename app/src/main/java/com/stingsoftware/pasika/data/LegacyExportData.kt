package com.stingsoftware.pasika.data

import com.google.gson.annotations.SerializedName

/**
 * These data classes represent the OLD structure of the exported JSON file.
 * They are used as an intermediate step to import data from older app versions
 * into the new, refactored database schema.
 * The @SerializedName annotation maps the short, obfuscated keys from the JSON
 * to meaningful property names.
 */

// Top-level structure
data class LegacyApiaryExportData(
    @SerializedName("a") val apiary: LegacyApiary,
    @SerializedName("b") val hivesWithInspections: List<LegacyHiveExportData>
)

data class LegacyHiveExportData(
    @SerializedName("a") val hive: LegacyHive,
    @SerializedName("b") val inspections: List<LegacyInspection>
)

// Corresponds to the old Apiary data class
data class LegacyApiary(
    @SerializedName("a") val id: Long,
    @SerializedName("b") val name: String,
    @SerializedName("c") val location: String,
    @SerializedName("d") val numberOfHives: Int,
    @SerializedName("e") val type: String,
    @SerializedName("g") val notes: String?,
    @SerializedName("f") val lastInspectionDate: Long? // Old field
)

// Corresponds to the old, larger Hive data class
data class LegacyHive(
    @SerializedName("a") val id: Long,
    @SerializedName("b") val apiaryId: Long,
    @SerializedName("c") val hiveNumber: String?,
    @SerializedName("d") val hiveType: String?,
    @SerializedName("e") val frameType: String?,
    @SerializedName("f") val material: String?,
    @SerializedName("j") val breed: String?,
    @SerializedName("l") val lastInspectionDate: Long?, // Field to be moved
    @SerializedName("m") val notes: String?,
    @SerializedName("n") val queenTagColor: String?,
    @SerializedName("o") val queenNumber: String?,
    @SerializedName("p") val queenYear: String?,
    @SerializedName("q") val queenLine: String?,
    @SerializedName("s") val isolationFromDate: Long?,
    @SerializedName("t") val isolationToDate: Long?,
    @SerializedName("v") val defensivenessRating: Int?, // Field to be moved
    @SerializedName("w") val framesTotal: Int?, // Field to be moved
    @SerializedName("x") val framesEggs: Int?, // Field to be moved
    @SerializedName("y") val framesOpenBrood: Int?, // Field to be moved
    @SerializedName("z") val framesCappedBrood: Int?, // Field to be moved
    @SerializedName("A") val framesFeed: Int?, // Field to be moved
    @SerializedName("B") val givenBuiltCombs: Int?, // Field to be moved
    @SerializedName("C") val givenFoundation: Int?, // Field to be moved
    @SerializedName("D") val givenBrood: Int?, // Field to be moved
    @SerializedName("E") val givenBeesKg: Double?, // Field to be moved
    @SerializedName("F") val givenHoneyKg: Double?, // Field to be moved
    @SerializedName("G") val givenSugarKg: Double?, // Field to be moved
    @SerializedName("H") val treatment: String?, // Field to be moved
    @SerializedName("I") val role: HiveRole? // Changed to nullable to handle missing data
)

// Corresponds to the old Inspection data class
data class LegacyInspection(
    @SerializedName("a") val id: Long,
    @SerializedName("b") val hiveId: Long,
    @SerializedName("c") val inspectionDate: Long,
    @SerializedName("d") val queenCellsPresent: Boolean?,
    @SerializedName("e") val queenCellsCount: Int?,
    @SerializedName("f") val framesEggsCount: Int?,
    @SerializedName("g") val framesOpenBroodCount: Int?,
    @SerializedName("h") val framesCappedBroodCount: Int?,
    @SerializedName("i") val framesHoneyCount: Int?,
    @SerializedName("j") val framesPollenCount: Int?,
    @SerializedName("k") val honeyStoresEstimateFrames: Int?, // Old field
    @SerializedName("l") val pollenStoresEstimateFrames: Int?, // Old field
    @SerializedName("m") val pestsDiseasesObserved: String?,
    @SerializedName("n") val treatmentApplied: String?, // Old field
    @SerializedName("o") val temperamentRating: Int?, // Old field
    @SerializedName("p") val managementActionsTaken: String?,
    @SerializedName("q") val notes: String?
)
