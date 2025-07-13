package com.stingsoftware.pasika.data

// Represents a Hive and all of its associated inspections
data class HiveExportData(
    val hive: Hive,
    val inspections: List<Inspection>
)

// Represents the top-level object for a single apiary export
data class ApiaryExportData(
    val apiary: Apiary,
    val hivesWithInspections: List<HiveExportData>
)