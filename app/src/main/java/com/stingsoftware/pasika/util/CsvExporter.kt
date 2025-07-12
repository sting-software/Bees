package com.stingsoftware.pasika.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.stingsoftware.pasika.data.Inspection
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val FILE_FORMAT = "yyyyMMdd_HHmmss"

    // Defines the header row for the CSV file
    private val CSV_HEADER = listOf(
        "InspectionDate",
        "QueenCellsPresent",
        "QueenCellsCount",
        "FramesWithEggs",
        "FramesWithOpenBrood",
        "FramesWithCappedBrood",
        "FramesWithHoney",
        "FramesWithPollen",
        "PestsOrDiseases",
        "TreatmentApplied",
        "TemperamentRating (1-4)",
        "ManagementActions",
        "Notes"
    )

    fun exportInspections(context: Context, hiveNumber: String, inspections: List<Inspection>): Boolean {
        if (inspections.isEmpty()) {
            return false // Nothing to export
        }

        val timestamp = SimpleDateFormat(FILE_FORMAT, Locale.US).format(Date())
        val fileName = "Pasika_Hive_${hiveNumber}_Inspections_$timestamp.csv"
        val csvContent = generateCsvContent(inspections)

        return try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                // Place file in the Downloads directory
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                }
            }

            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: throw IOException("Failed to create new MediaStore entry.")

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray())
            } ?: throw IOException("Failed to get output stream.")

            true // Success
        } catch (e: IOException) {
            e.printStackTrace()
            false // Failure
        }
    }

    private fun generateCsvContent(inspections: List<Inspection>): String {
        val stringBuilder = StringBuilder()
        // Append header
        stringBuilder.append(CSV_HEADER.joinToString(",")).append("\n")

        // Append rows
        val sortedInspections = inspections.sortedBy { it.inspectionDate }
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        sortedInspections.forEach { inspection ->
            val row = listOf(
                dateFormatter.format(Date(inspection.inspectionDate)),
                if (inspection.queenCellsPresent == true) "Yes" else "No",
                inspection.queenCellsCount?.toString() ?: "",
                inspection.framesEggsCount?.toString() ?: "",
                inspection.framesOpenBroodCount?.toString() ?: "",
                inspection.framesCappedBroodCount?.toString() ?: "",
                inspection.framesHoneyCount?.toString() ?: "",
                inspection.framesPollenCount?.toString() ?: "",
                "\"${inspection.pestsDiseasesObserved?.replace("\"", "\"\"") ?: ""}\"",
                "\"${inspection.treatmentApplied?.replace("\"", "\"\"") ?: ""}\"",
                inspection.temperamentRating?.toString() ?: "",
                "\"${inspection.managementActionsTaken?.replace("\"", "\"\"") ?: ""}\"",
                "\"${inspection.notes?.replace("\"", "\"\"") ?: ""}\""
            ).joinToString(",")
            stringBuilder.append(row).append("\n")
        }

        return stringBuilder.toString()
    }
}