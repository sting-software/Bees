package com.stingsoftware.pasika.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import com.stingsoftware.pasika.data.Inspection
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN = 40
    private const val FONT_SIZE_TITLE = 18f
    private const val FONT_SIZE_SUBTITLE = 14f
    private const val FONT_SIZE_BODY = 10f
    private const val LINE_SPACING = 4f

    fun exportInspections(context: Context, hiveNumber: String, inspections: List<Inspection>): Boolean {
        if (inspections.isEmpty()) return false

        val document = PdfDocument()
        val contentWidth = A4_WIDTH - 2 * MARGIN

        val titlePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
            textSize = FONT_SIZE_TITLE
        }
        val subtitlePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
            textSize = FONT_SIZE_SUBTITLE
        }
        val bodyPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.DKGRAY
            textSize = FONT_SIZE_BODY
        }

        val sortedInspections = inspections.sortedBy { it.inspectionDate }
        val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = MARGIN.toFloat()

        // Draw header
        canvas.drawText("Inspection History for Hive #$hiveNumber", MARGIN.toFloat(), yPosition, titlePaint)
        yPosition += (titlePaint.descent() - titlePaint.ascent()) * 2

        sortedInspections.forEach { inspection ->
            val inspectionDate = dateFormatter.format(Date(inspection.inspectionDate))
            val inspectionFields = formatInspectionFields(inspection)

            // Check if there is enough space for the next inspection block
            val blockHeight = calculateBlockHeight(inspectionFields, subtitlePaint, bodyPaint, contentWidth)
            if (yPosition + blockHeight > A4_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN.toFloat()
            }

            // Draw inspection date as a subtitle
            canvas.drawText(inspectionDate, MARGIN.toFloat(), yPosition, subtitlePaint)
            yPosition += (subtitlePaint.descent() - subtitlePaint.ascent()) + LINE_SPACING * 2

            // Draw each field
            inspectionFields.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    val keyText = "$key: "
                    val valueText = value

                    val keyWidth = bodyPaint.measureText(keyText)
                    canvas.drawText(keyText, MARGIN.toFloat(), yPosition, bodyPaint)

                    // Use StaticLayout for value text to handle wrapping
                    val textLayout = StaticLayout.Builder.obtain(valueText, 0, valueText.length, bodyPaint, contentWidth - keyWidth.toInt())
                        .build()
                    canvas.save()
                    canvas.translate(MARGIN.toFloat() + keyWidth, yPosition - bodyPaint.fontMetrics.top)
                    textLayout.draw(canvas)
                    canvas.restore()

                    yPosition += textLayout.height + LINE_SPACING
                }
            }
            yPosition += LINE_SPACING * 4 // Extra space between inspections
        }

        document.finishPage(page)

        // Save the document
        return savePdf(context, hiveNumber, document)
    }

    private fun formatInspectionFields(inspection: Inspection): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        fields["Queen Cells"] = (if (inspection.queenCellsPresent == true) "Yes" else "No") + (inspection.queenCellsCount?.let { " ($it)" } ?: "")
        fields["Frames (Eggs/Open/Capped)"] = "${inspection.framesEggsCount ?: 0}/${inspection.framesOpenBroodCount ?: 0}/${inspection.framesCappedBroodCount ?: 0}"
        fields["Frames (Honey/Pollen)"] = "${inspection.framesHoneyCount ?: 0}/${inspection.framesPollenCount ?: 0}"
        fields["Pests/Diseases"] = inspection.pestsDiseasesObserved ?: ""
        fields["Treatment"] = inspection.treatmentApplied ?: ""
        fields["Temperament"] = inspection.temperamentRating?.toString() ?: ""
        fields["Management Actions"] = inspection.managementActionsTaken ?: ""
        fields["Notes"] = inspection.notes ?: ""
        return fields
    }

    private fun calculateBlockHeight(fields: Map<String, String>, subtitlePaint: Paint, bodyPaint: TextPaint, width: Int): Float {
        var height = (subtitlePaint.descent() - subtitlePaint.ascent()) + LINE_SPACING * 6
        fields.forEach { (_, value) ->
            if (value.isNotBlank()) {
                val layout = StaticLayout.Builder.obtain(value, 0, value.length, bodyPaint, width).build()
                height += layout.height + LINE_SPACING
            }
        }
        return height
    }

    private fun savePdf(context: Context, hiveNumber: String, document: PdfDocument): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Pasika_Hive_${hiveNumber}_Inspections_$timestamp.pdf"

        return try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                }
            }
            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: throw IOException("Failed to create new MediaStore entry for PDF.")

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            } ?: throw IOException("Failed to get output stream for PDF.")

            document.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            document.close()
            false
        }
    }
}