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
import com.stingsoftware.pasika.R

object CsvExporter {

    private const val FILE_FORMAT = "yyyyMMdd_HHmmss"

    fun exportInspections(
        context: Context,
        hiveNumber: String,
        inspections: List<Inspection>
    ): Boolean {
        if (inspections.isEmpty()) {
            return false
        }

        val timestamp = SimpleDateFormat(FILE_FORMAT, Locale.US).format(Date())
        val fileName = "Pasika_Hive_${hiveNumber}_Inspections_$timestamp.csv"
        val csvContent = generateCsvContent(context, inspections)

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

            val uri =
                contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    ?: throw IOException("Failed to create new MediaStore entry.")
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                outputStream.write(bom)
                outputStream.write(csvContent.toByteArray())

            } ?: throw IOException("Failed to get output stream.")

            true // Success
        } catch (e: IOException) {
            e.printStackTrace()
            false // Failure
        }
    }

    private fun generateCsvContent(context: Context, inspections: List<Inspection>): String {
        val stringBuilder = StringBuilder()
        val csvHeader = listOf(
            context.getString(R.string.hint_inspection_date),
            context.getString(R.string.hint_queen_cells),
            context.getString(R.string.hint_queen_cells_count),
            context.getString(R.string.label_frames_eggs),
            context.getString(R.string.label_frames_open_brood),
            context.getString(R.string.label_frames_capped_brood),
            context.getString(R.string.hint_honey_stores_frames),
            context.getString(R.string.hint_pollen_stores_frames),
            context.getString(R.string.hint_pests_diseases),
            context.getString(R.string.hint_treatment_applied),
            context.getString(R.string.title_temperament_rating),
            context.getString(R.string.hint_management_actions),
            context.getString(R.string.hint_notes_optional)
        ).joinToString(",")

        stringBuilder.append(csvHeader).append("\n")
        // Append rows
        val sortedInspections = inspections.sortedBy { it.inspectionDate }
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        sortedInspections.forEach { inspection ->
            val row = listOf(
                dateFormatter.format(Date(inspection.inspectionDate)),
                if (inspection.queenCellsPresent == true) {
                    context.getString(R.string.dialog_yes)
                } else {
                    context.getString(R.string.dialog_no)
                },
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