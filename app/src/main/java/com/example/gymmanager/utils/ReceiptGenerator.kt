package com.example.gymmanager.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptGenerator {

    // This function paints the PDF and saves it to the phone's temporary cache
    fun generatePdf(
        context: Context,
        gymName: String = "FITNESS PRO GYM", // You can change this to your dynamic gym name later
        name: String,
        phone: String,
        dob: String,
        address: String,
        planMonths: Int,
        totalFee: Double,
        paidAmount: Double,
        pendingBalance: Double,
        joinDateMillis: Long,
        expiryDateMillis: Long
    ): File? {
        // 1. Create the PDF Document and specify the page size (Standard A4 dimensions)
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // 2. Setup the "Paintbrushes" (Fonts, colors, sizes)
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 28f
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 16f
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }

        val textPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 14f
            color = Color.BLACK
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // 3. Start Painting the Canvas! (Coordinates: x is left-to-right, y is top-to-bottom)
        val centerX = pageInfo.pageWidth / 2f
        var currentY = 60f

        // --- HEADER ---
        canvas.drawText(gymName, centerX, currentY, titlePaint)
        currentY += 30f
        canvas.drawText("Official Payment Receipt", centerX, currentY, subtitlePaint)
        currentY += 40f

        // Draw a separator line
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 2f }
        canvas.drawLine(40f, currentY, pageInfo.pageWidth - 40f, currentY, linePaint)
        currentY += 40f

        // --- MEMBER DETAILS ---
        canvas.drawText("MEMBER DETAILS", 40f, currentY, headerPaint)
        currentY += 30f
        canvas.drawText("Name: $name", 40f, currentY, textPaint)
        currentY += 25f
        canvas.drawText("Phone: $phone", 40f, currentY, textPaint)
        currentY += 25f
        canvas.drawText("DOB: $dob", 40f, currentY, textPaint)
        currentY += 25f
        // Note: For super long addresses, Android doesn't auto-wrap on Canvas. We keep it simple here.
        canvas.drawText("Address: $address", 40f, currentY, textPaint)

        currentY += 50f
        canvas.drawLine(40f, currentY, pageInfo.pageWidth - 40f, currentY, linePaint)
        currentY += 40f

        // --- MEMBERSHIP & PAYMENT ---
        canvas.drawText("MEMBERSHIP & PAYMENT", 40f, currentY, headerPaint)
        currentY += 30f
        canvas.drawText("Plan Duration: $planMonths Month(s)", 40f, currentY, textPaint)
        currentY += 25f
        canvas.drawText("Joining Date: ${dateFormat.format(Date(joinDateMillis))}", 40f, currentY, textPaint)
        currentY += 25f
        canvas.drawText("Valid Until: ${dateFormat.format(Date(expiryDateMillis))}", 40f, currentY, textPaint)

        currentY += 40f

        // Financials
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Plan Fee: Rs. ${totalFee.toInt()}", 40f, currentY, textPaint)
        currentY += 25f

        textPaint.color = Color.parseColor("#4CAF50") // Green
        canvas.drawText("Amount Paid Today: Rs. ${paidAmount.toInt()}", 40f, currentY, textPaint)
        currentY += 25f

        if (pendingBalance > 0) {
            textPaint.color = Color.RED
            canvas.drawText("Pending Dues: Rs. ${pendingBalance.toInt()}", 40f, currentY, textPaint)
        } else {
            textPaint.color = Color.BLACK
            canvas.drawText("Pending Dues: NIL", 40f, currentY, textPaint)
        }

        currentY += 80f

        // --- FOOTER ---
        canvas.drawLine(40f, currentY, pageInfo.pageWidth - 40f, currentY, linePaint)
        currentY += 40f
        subtitlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Thank you for choosing $gymName!", centerX, currentY, subtitlePaint)
        currentY += 25f
        subtitlePaint.textSize = 12f
        canvas.drawText("This is a computer-generated receipt.", centerX, currentY, subtitlePaint)

        // 4. Finish the page
        pdfDocument.finishPage(page)

        // 5. Save the file to the phone's hidden cache folder
        return try {
            // Remove spaces from name to make a clean filename
            val safeName = name.replace(" ", "_").take(15)
            val file = File(context.cacheDir, "Receipt_$safeName.pdf")

            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            file // Return the file so we can send it to WhatsApp!
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}