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
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Inspection
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Canvas

object PdfExporter {

    // Page and Margin Constants
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN = 40
    private const val HEADER_FOOTER_HEIGHT = 60

    private const val CELL_PADDING = 8f

    // Font Size Constants
    private const val FONT_SIZE_TITLE = 18f
    private const val FONT_SIZE_SUBTITLE = 14f
    private const val FONT_SIZE_BODY = 10f
    private const val FONT_SIZE_FOOTER = 8f

    // Spacing Constants
    private const val LINE_SPACING = 4f
    private const val SECTION_SPACING = 12f

    // Layout Constants
    private const val CONTENT_WIDTH = A4_WIDTH - 2 * MARGIN
    private const val KEY_COLUMN_WIDTH = (CONTENT_WIDTH * 0.4f).toInt()
    private const val VALUE_COLUMN_WIDTH = (CONTENT_WIDTH * 0.6f).toInt()
    private const val VALUE_COLUMN_START = MARGIN + KEY_COLUMN_WIDTH

    fun exportInspections(
        context: Context,
        hiveNumber: String,
        inspections: List<Inspection>
    ): Boolean {
        if (inspections.isEmpty()) return false

        val document = PdfDocument()

        // Paint objects
        val titlePaint = createTextPaint(Typeface.BOLD, Color.BLACK, FONT_SIZE_TITLE)
        val datePaint = createTextPaint(Typeface.NORMAL, Color.DKGRAY, FONT_SIZE_BODY)
        val subtitlePaint = createTextPaint(Typeface.BOLD, Color.BLACK, FONT_SIZE_SUBTITLE)
        val bodyKeyPaint = createTextPaint(Typeface.BOLD, Color.DKGRAY, FONT_SIZE_BODY)
        val bodyValuePaint = createTextPaint(Typeface.NORMAL, Color.DKGRAY, FONT_SIZE_BODY)
        val footerPaint = createTextPaint(Typeface.NORMAL, Color.GRAY, FONT_SIZE_FOOTER).apply {
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val sortedInspections = inspections.sortedBy { it.inspectionDate }
        val reportDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = MARGIN.toFloat()

        // Draw initial header
        drawHeader(context, canvas, hiveNumber, reportDate, titlePaint, datePaint)
        yPosition = HEADER_FOOTER_HEIGHT.toFloat()

        sortedInspections.forEach { inspection ->
            val inspectionFields = formatInspectionFields(context, inspection)
            val blockHeight = calculateInspectionBlockHeight(inspectionFields, subtitlePaint, bodyValuePaint)

            // Check for page break
            if (yPosition + blockHeight > A4_HEIGHT - HEADER_FOOTER_HEIGHT) {
                drawFooter(canvas, pageNumber, footerPaint)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(context, canvas, hiveNumber, reportDate, titlePaint, datePaint)
                yPosition = HEADER_FOOTER_HEIGHT.toFloat()
            }

            // Draw separator line
            if (yPosition > HEADER_FOOTER_HEIGHT) {
                canvas.drawLine(MARGIN.toFloat(), yPosition, (A4_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
                yPosition += SECTION_SPACING
            }


            // Draw inspection content
            yPosition = drawInspectionBlock(
                canvas,
                inspection,
                inspectionFields,
                yPosition,
                subtitlePaint,
                bodyKeyPaint,
                bodyValuePaint,
                linePaint
            )
        }

        drawFooter(canvas, pageNumber, footerPaint)
        document.finishPage(page)
        return savePdf(context, hiveNumber, document)
    }

    private fun createTextPaint(style: Int, color: Int, size: Float): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, style)
            this.color = color
            textSize = size
        }
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        hiveNumber: String,
        reportDate: String,
        titlePaint: Paint,
        datePaint: Paint
    ) {
        val titleText = context.getString(R.string.pdf_title, hiveNumber)
        canvas.drawText(titleText, MARGIN.toFloat(), MARGIN.toFloat(), titlePaint)

        val dateLabel = context.getString(R.string.pdf_report_date)
        val dateText = "$dateLabel: $reportDate"
        val dateWidth = datePaint.measureText(dateText)
        canvas.drawText(dateText, (A4_WIDTH - MARGIN - dateWidth), MARGIN.toFloat(), datePaint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, footerPaint: Paint) {
        val yPos = (A4_HEIGHT - MARGIN / 2).toFloat()
        canvas.drawText("Page $pageNumber", (A4_WIDTH / 2).toFloat(), yPos, footerPaint)
    }
    private fun drawInspectionBlock(
        canvas: Canvas,
        inspection: Inspection,
        fields: Map<String, String>,
        startY: Float,
        subtitlePaint: Paint,
        keyPaint: TextPaint,
        valuePaint: TextPaint,
        linePaint: Paint
    ): Float {
        var yPos = startY
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val inspectionDate = dateFormatter.format(Date(inspection.inspectionDate))

        // Draw inspection date as a subtitle
        canvas.drawText(inspectionDate, MARGIN.toFloat(), yPos, subtitlePaint)
        yPos += subtitlePaint.descent() - subtitlePaint.ascent() + LINE_SPACING * 2

        val tableTopY = yPos

        // Draw the top border of the table
        canvas.drawLine(
            MARGIN.toFloat(),
            tableTopY,
            (A4_WIDTH - MARGIN).toFloat(),
            tableTopY,
            linePaint
        )

        // Calculate the new width for the value column's text area
        val paddedValueWidth = (VALUE_COLUMN_WIDTH - (CELL_PADDING * 2)).toInt()

        fields.forEach { (key, value) ->
            if (value.isNotBlank()) {
                val valueLayout = StaticLayout.Builder.obtain(
                    value, 0, value.length, valuePaint, paddedValueWidth // Use new padded width
                ).build()

                // Calculate row height with vertical padding
                val rowHeight = valueLayout.height + (CELL_PADDING * 2)

                // Center the key text vertically and apply horizontal padding
                val keyX = MARGIN.toFloat() + CELL_PADDING
                val keyY = yPos + (rowHeight / 2) - ((keyPaint.descent() + keyPaint.ascent()) / 2)
                canvas.drawText(key, keyX, keyY, keyPaint)

                // Draw value, offsetting by the padding
                canvas.save()
                canvas.translate(
                    VALUE_COLUMN_START.toFloat() + CELL_PADDING, // Apply left padding
                    yPos + CELL_PADDING // Apply top padding
                )
                valueLayout.draw(canvas)
                canvas.restore()

                yPos += rowHeight

                // Draw bottom border for the row
                canvas.drawLine(
                    MARGIN.toFloat(),
                    yPos,
                    (A4_WIDTH - MARGIN).toFloat(),
                    yPos,
                    linePaint
                )
            }
        }

        val tableBottomY = yPos

        // Draw the three vertical lines
        canvas.drawLine(MARGIN.toFloat(), tableTopY, MARGIN.toFloat(), tableBottomY, linePaint) // Left
        canvas.drawLine(VALUE_COLUMN_START.toFloat(), tableTopY, VALUE_COLUMN_START.toFloat(), tableBottomY, linePaint) // Middle
        canvas.drawLine((A4_WIDTH - MARGIN).toFloat(), tableTopY, (A4_WIDTH - MARGIN).toFloat(), tableBottomY, linePaint) // Right

        return yPos + SECTION_SPACING
    }
    private fun calculateInspectionBlockHeight(
        fields: Map<String, String>,
        subtitlePaint: Paint,
        bodyPaint: TextPaint
    ): Float {
        var height = (subtitlePaint.descent() - subtitlePaint.ascent()) + LINE_SPACING * 2
        height += SECTION_SPACING // For separator line

        fields.forEach { (_, value) ->
            if (value.isNotBlank()) {
                val layout = StaticLayout.Builder.obtain(
                    value, 0, value.length, bodyPaint, VALUE_COLUMN_WIDTH
                ).build()
                height += layout.height + LINE_SPACING
            }
        }
        return height + SECTION_SPACING
    }

    private fun formatInspectionFields(
        context: Context,
        inspection: Inspection
    ): Map<String, String> {
        val fields = linkedMapOf<String, String>() // Use LinkedHashMap to preserve order
        val textYes = context.getString(R.string.dialog_yes)
        val textNo = context.getString(R.string.dialog_no)

        // Queen Cells
        val queenCellsKey = context.getString(R.string.hint_queen_cells_count)
        val queenCellsValue = (if (inspection.queenCellsPresent == true) textYes else textNo) +
                (inspection.queenCellsCount?.let { " ($it)" } ?: "")
        fields[queenCellsKey] = queenCellsValue

        // Brood Frames
        val broodKey = context.getString(R.string.label_given_taken_brood)
        val broodValue = "${inspection.framesEggsCount ?: 0} / " +
                "${inspection.framesOpenBroodCount ?: 0} / " +
                "${inspection.framesCappedBroodCount ?: 0}"
        fields[broodKey] = broodValue

        // Feed Frames
        val feedKey = context.getString(R.string.label_frames_feed)
        val feedValue = "${inspection.framesHoneyCount ?: 0} / " +
                "${inspection.framesPollenCount ?: 0}"
        fields[feedKey] = feedValue

        // Pests & Diseases
        val pestsKey = context.getString(R.string.hint_pests_diseases)
        fields[pestsKey] = inspection.pestsDiseasesObserved ?: ""

        // Treatment
        val treatmentKey = context.getString(R.string.hint_treatment_applied)
        fields[treatmentKey] = inspection.treatmentApplied ?: ""

        // Temperament
        val temperamentKey = context.getString(R.string.title_temperament_rating)
        fields[temperamentKey] = inspection.temperamentRating?.toString() ?: ""

        // Management
        val managementKey = context.getString(R.string.hint_management_actions)
        fields[managementKey] = inspection.managementActionsTaken ?: ""

        // Notes
        val notesKey = context.getString(R.string.hint_notes_optional)
        fields[notesKey] = inspection.notes ?: ""

        return fields
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