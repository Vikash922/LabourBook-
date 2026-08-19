package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.LaborWorker
import com.example.data.model.CashTransaction
import com.example.data.model.AttendanceStatus
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    private val titlePaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#64748B")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    
    private val headerPaint = Paint().apply {
        color = Color.parseColor("#0F172A")
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.parseColor("#334155")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    
    private val textBoldPaint = Paint().apply {
        color = Color.parseColor("#0F172A")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#F1F5F9")
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        strokeWidth = 1f
    }
    
    private fun drawBrandingFooter(canvas: Canvas) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val currentDate = sdf.format(Date())
        
        val footerY = PAGE_HEIGHT - 40f
        
        // Line above footer
        val topBorderPaint = Paint().apply { color = Color.parseColor("#e2e8f0"); strokeWidth = 1f }
        canvas.drawLine(MARGIN, footerY - 20f, PAGE_WIDTH - MARGIN, footerY - 20f, topBorderPaint)
        
        // Logo Background
        val logoBgPaint = Paint().apply { color = Color.parseColor("#0ea5e9"); isAntiAlias = true }
        canvas.drawRoundRect(MARGIN, footerY - 10f, MARGIN + 20f, footerY + 10f, 4f, 4f, logoBgPaint)
        
        // Logo Text (L)
        val logoTextPaint = Paint().apply { color = Color.WHITE; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        canvas.drawText("L", MARGIN + 6f, footerY + 5f, logoTextPaint)
        
        // App Label
        val appLabelPaint = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        canvas.drawText("Laborbook App", MARGIN + 30f, footerY + 5f, appLabelPaint)
        
        // Date
        val datePaint = Paint().apply { color = Color.parseColor("#94a3b8"); textSize = 10f; isAntiAlias = true }
        val dateStr = "Generated: $currentDate"
        val dateWidth = datePaint.measureText(dateStr)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - dateWidth, footerY + 5f, datePaint)
    }

    fun shareWorkerReportPdf(context: Context, worker: LaborWorker, month: String) {
        val document = PdfDocument()
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas
        var y = MARGIN
        
        // Header
        canvas.drawText("ATTENDANCE & WAGE SLIP", MARGIN, y + 20, titlePaint)
        y += 40
        canvas.drawText("Month: $month", MARGIN, y, subtitlePaint)
        y += 30
        
        // Worker details box
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 60, 8f, 8f, bgPaint)
        canvas.drawText("Worker Name: ${worker.name}", MARGIN + 15, y + 25, textBoldPaint)
        canvas.drawText("Phone: ${worker.phoneNumber}", MARGIN + 15, y + 45, textPaint)
        
        canvas.drawText("Daily Wage:", PAGE_WIDTH - MARGIN - 150, y + 25, textPaint)
        canvas.drawText("Rs.${worker.dailyWage.toInt()}", PAGE_WIDTH - MARGIN - 150, y + 45, titlePaint)
        y += 80
        
        // Summary
        val present = worker.getTotalPresent(month)
        val absent = worker.getTotalAbsent(month)
        val otHours = worker.getTotalOvertimeHours(month)
        val adv = worker.getTotalAdvance(month)
        val totalGross = (present * worker.dailyWage) + (otHours * (worker.dailyWage / 8.0) * 1.5)
        val netPayable = totalGross - adv
        
        canvas.drawText("Summary", MARGIN, y + 20, headerPaint)
        canvas.drawLine(MARGIN, y + 30, PAGE_WIDTH - MARGIN, y + 30, linePaint)
        y += 50
        
        canvas.drawText("Total Present:", MARGIN, y, textPaint)
        canvas.drawText("${if (present % 1.0 == 0.0) present.toInt() else present} Days", MARGIN + 120, y, textBoldPaint)
        
        canvas.drawText("Total Absent:", MARGIN + 250, y, textPaint)
        canvas.drawText("${absent.toInt()} Days", MARGIN + 350, y, textBoldPaint)
        y += 25
        
        canvas.drawText("Overtime:", MARGIN, y, textPaint)
        canvas.drawText("${otHours.toInt()} Hours", MARGIN + 120, y, textBoldPaint)
        
        canvas.drawText("Advances Taken:", MARGIN + 250, y, textPaint)
        canvas.drawText("Rs.${String.format(Locale.US, "%.1f", adv)}", MARGIN + 350, y, textBoldPaint)
        y += 35
        
        // Pay
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 50, 8f, 8f, Paint().apply { color = Color.parseColor("#E0F2FE") })
        canvas.drawText("NET PAYABLE AMOUNT", MARGIN + 15, y + 30, textBoldPaint)
        canvas.drawText("Rs.${String.format(Locale.US, "%.1f", netPayable)}", PAGE_WIDTH - MARGIN - 150, y + 30, titlePaint)
        y += 80
        
        // Daily logs
        canvas.drawText("Daily Attendance Logs", MARGIN, y + 20, headerPaint)
        canvas.drawLine(MARGIN, y + 30, PAGE_WIDTH - MARGIN, y + 30, linePaint)
        y += 50
        
        val monthAtt = worker.getAttendanceForMonth(month)
        val activeDays = monthAtt.values.filter { it.status != AttendanceStatus.UNMARKED || it.advanceAmount > 0 }.sortedBy { it.dayNumber }
        
        if (activeDays.isEmpty()) {
            canvas.drawText("No active days logged yet for this month.", MARGIN, y, textPaint)
        } else {
            // Table Header
            canvas.drawRect(MARGIN, y - 15, PAGE_WIDTH - MARGIN, y + 10, bgPaint)
            canvas.drawText("Date", MARGIN + 10, y, textBoldPaint)
            canvas.drawText("Status", MARGIN + 100, y, textBoldPaint)
            canvas.drawText("Advance", MARGIN + 220, y, textBoldPaint)
            canvas.drawText("Remarks", MARGIN + 320, y, textBoldPaint)
            y += 25
            
            for (d in activeDays) {
                if (y > PAGE_HEIGHT - 100) {
                    drawBrandingFooter(canvas)
                    document.finishPage(page)
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
                    canvas = page.canvas
                    y = MARGIN + 20
                }
                
                val dateStr = "${String.format(Locale.US, "%02d", d.dayNumber)} ${d.dayOfWeek}"
                val statusText = when (d.status) {
                    AttendanceStatus.PRESENT -> "Present"
                    AttendanceStatus.ABSENT -> "Absent"
                    AttendanceStatus.OVERTIME -> "OT (${d.overtimeHours.toInt()}h)"
                    AttendanceStatus.HALF_DAY -> "Half Day"
                    AttendanceStatus.UNMARKED -> "-"
                }
                val advanceText = if (d.advanceAmount > 0) "Rs.${d.advanceAmount.toInt()}" else "-"
                
                canvas.drawText(dateStr, MARGIN + 10, y, textPaint)
                canvas.drawText(statusText, MARGIN + 100, y, textPaint)
                canvas.drawText(advanceText, MARGIN + 220, y, textPaint)
                
                var note = d.note
                if (note.length > 25) note = note.substring(0, 22) + "..."
                canvas.drawText(if(note.isBlank()) "-" else note, MARGIN + 320, y, textPaint)
                
                canvas.drawLine(MARGIN, y + 10, PAGE_WIDTH - MARGIN, y + 10, linePaint)
                y += 25
            }
        }
        
        drawBrandingFooter(canvas)
        document.finishPage(page)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        saveAndSharePdf(context, document, "Worker_Report_${worker.name.replace(" ", "_")}_$timestamp.pdf")
    }
    
    fun shareCashBookReportPdf(context: Context, transactions: List<CashTransaction>, startDate: String, endDate: String) {
        val document = PdfDocument()
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas
        var y = MARGIN
        
        canvas.drawText("CASH BOOK LEDGER", MARGIN, y + 20, titlePaint)
        y += 40
        canvas.drawText("Duration: $startDate - $endDate", MARGIN, y, subtitlePaint)
        y += 30
        
        val totalIn = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
        val totalOut = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
        val balance = totalIn - totalOut
        
        // Summary Cards
        val cardWidth = (PAGE_WIDTH - 2 * MARGIN - 20) / 3
        
        // In
        canvas.drawRoundRect(MARGIN, y, MARGIN + cardWidth, y + 60, 8f, 8f, Paint().apply { color = Color.parseColor("#DCFCE7") })
        canvas.drawText("Total In", MARGIN + 10, y + 20, subtitlePaint)
        canvas.drawText("Rs.${String.format(Locale.US, "%.0f", totalIn)}", MARGIN + 10, y + 45, textBoldPaint.apply { color = Color.parseColor("#15803D") })
        
        // Out
        canvas.drawRoundRect(MARGIN + cardWidth + 10, y, MARGIN + 2 * cardWidth + 10, y + 60, 8f, 8f, Paint().apply { color = Color.parseColor("#FEE2E2") })
        canvas.drawText("Total Out", MARGIN + cardWidth + 20, y + 20, subtitlePaint)
        canvas.drawText("Rs.${String.format(Locale.US, "%.0f", totalOut)}", MARGIN + cardWidth + 20, y + 45, textBoldPaint.apply { color = Color.parseColor("#B91C1C") })
        
        // Balance
        canvas.drawRoundRect(MARGIN + 2 * cardWidth + 20, y, PAGE_WIDTH - MARGIN, y + 60, 8f, 8f, Paint().apply { color = Color.parseColor("#F1F5F9") })
        canvas.drawText("Net Balance", MARGIN + 2 * cardWidth + 30, y + 20, subtitlePaint)
        canvas.drawText("Rs.${String.format(Locale.US, "%.0f", balance)}", MARGIN + 2 * cardWidth + 30, y + 45, titlePaint.apply { textSize = 16f })
        
        // reset colors
        textBoldPaint.color = Color.parseColor("#0F172A")
        titlePaint.textSize = 22f
        
        y += 90
        
        // Table Header
        canvas.drawRect(MARGIN, y - 15, PAGE_WIDTH - MARGIN, y + 10, bgPaint)
        canvas.drawText("Date", MARGIN + 10, y, textBoldPaint)
        canvas.drawText("Type", MARGIN + 120, y, textBoldPaint)
        canvas.drawText("Amount", MARGIN + 200, y, textBoldPaint)
        canvas.drawText("Mode", MARGIN + 300, y, textBoldPaint)
        canvas.drawText("Notes", MARGIN + 380, y, textBoldPaint)
        y += 25
        
        for (tx in transactions) {
            if (y > PAGE_HEIGHT - 100) {
                drawBrandingFooter(canvas)
                document.finishPage(page)
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
                canvas = page.canvas
                y = MARGIN + 20
            }
            
            canvas.drawText(tx.dateDisplay, MARGIN + 10, y, textPaint)
            
            val isCashIn = tx.type == TransactionType.CASH_IN
            val typeColor = if (isCashIn) Color.parseColor("#15803D") else Color.parseColor("#B91C1C")
            val typeText = if (isCashIn) "IN" else "OUT"
            
            val prevColor = textBoldPaint.color
            textBoldPaint.color = typeColor
            canvas.drawText(typeText, MARGIN + 120, y, textBoldPaint)
            canvas.drawText("Rs.${tx.amount}", MARGIN + 200, y, textBoldPaint)
            textBoldPaint.color = prevColor
            
            canvas.drawText(tx.paymentMethod.name, MARGIN + 300, y, textPaint)
            
            var note = tx.notes
            if (note.length > 15) note = note.substring(0, 12) + "..."
            canvas.drawText(if(note.isBlank()) "-" else note, MARGIN + 380, y, textPaint)
            
            canvas.drawLine(MARGIN, y + 10, PAGE_WIDTH - MARGIN, y + 10, linePaint)
            y += 25
        }
        
        drawBrandingFooter(canvas)
        document.finishPage(page)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        saveAndSharePdf(context, document, "Cash_Book_Ledger_$timestamp.pdf")
    }
    
    fun shareBatchWorkersReportPdf(context: Context, workers: List<LaborWorker>, month: String) {
        val document = PdfDocument()
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas
        var y = MARGIN
        
        canvas.drawText("MONTHLY STAFF REPORT", MARGIN, y + 20, titlePaint)
        y += 40
        canvas.drawText("Month: $month", MARGIN, y, subtitlePaint)
        val totalStaffStr = "Total Staff: ${workers.size}"
        val totalStaffWidth = subtitlePaint.measureText(totalStaffStr)
        canvas.drawText(totalStaffStr, PAGE_WIDTH - MARGIN - totalStaffWidth, y, subtitlePaint)
        y += 30
        
        var grandGross = 0.0
        var grandAdvance = 0.0
        var grandNet = 0.0
        
        // Table Header
        canvas.drawRect(MARGIN, y - 15, PAGE_WIDTH - MARGIN, y + 10, bgPaint)
        canvas.drawText("Worker", MARGIN + 10, y, textBoldPaint)
        canvas.drawText("Rate", MARGIN + 150, y, textBoldPaint)
        canvas.drawText("Attend. (P/A/OT)", MARGIN + 220, y, textBoldPaint)
        canvas.drawText("Advance", MARGIN + 360, y, textBoldPaint)
        canvas.drawText("Net Pay", MARGIN + 440, y, textBoldPaint)
        y += 25
        
        for (w in workers) {
            if (y > PAGE_HEIGHT - 100) {
                drawBrandingFooter(canvas)
                document.finishPage(page)
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
                canvas = page.canvas
                y = MARGIN + 20
            }
            
            val p = w.getTotalPresent(month)
            val a = w.getTotalAbsent(month)
            val ot = w.getTotalOvertimeHours(month)
            val adv = w.getTotalAdvance(month)
            val gross = (p * w.dailyWage) + (ot * (w.dailyWage / 8.0) * 1.5)
            val net = gross - adv
            grandGross += gross
            grandAdvance += adv
            grandNet += net
            
            var wName = w.name.uppercase()
            if (wName.length > 16) wName = wName.substring(0, 13) + "..."
            
            canvas.drawText(wName, MARGIN + 10, y, textBoldPaint)
            canvas.drawText("${w.dailyWage.toInt()}/d", MARGIN + 150, y, textPaint)
            canvas.drawText("${if(p % 1 == 0.0) p.toInt() else p}P | ${a.toInt()}A | ${ot.toInt()}h", MARGIN + 220, y, textPaint)
            canvas.drawText(adv.toInt().toString(), MARGIN + 360, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.0f", net), MARGIN + 440, y, textBoldPaint)
            
            canvas.drawLine(MARGIN, y + 10, PAGE_WIDTH - MARGIN, y + 10, linePaint)
            y += 25
        }
        
        y += 20
        if (y > PAGE_HEIGHT - 140) {
            drawBrandingFooter(canvas)
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
            canvas = page.canvas
            y = MARGIN + 20
        }
        
        // Grand Totals
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 70, 8f, 8f, Paint().apply { color = Color.parseColor("#E0F2FE") })
        canvas.drawText("GRAND TOTALS", MARGIN + 15, y + 25, headerPaint)
        
        canvas.drawText("Total Gross: Rs.${String.format(Locale.US, "%.0f", grandGross)}", MARGIN + 15, y + 50, textPaint)
        canvas.drawText("Total Advances: Rs.${String.format(Locale.US, "%.0f", grandAdvance)}", MARGIN + 180, y + 50, textPaint)
        canvas.drawText("Total Net: Rs.${String.format(Locale.US, "%.0f", grandNet)}", MARGIN + 360, y + 50, textBoldPaint)
        
        drawBrandingFooter(canvas)
        document.finishPage(page)
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        saveAndSharePdf(context, document, "Monthly_Staff_Report_$timestamp.pdf")
    }

    private fun saveAndSharePdf(context: Context, document: PdfDocument, fileName: String) {
        try {
            // 1. Write the document to a temporary cache file for sharing
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()

            // 2. Save a permanent copy to the device's Downloads folder
            var savedSuccessfully = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Laborbook")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { destUri ->
                    context.contentResolver.openOutputStream(destUri)?.use { os ->
                        cacheFile.inputStream().use { input -> input.copyTo(os) }
                        savedSuccessfully = true
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val laborbookDir = File(downloadsDir, "Laborbook")
                if (!laborbookDir.exists()) laborbookDir.mkdirs()
                val destFile = File(laborbookDir, fileName)
                cacheFile.inputStream().use { input -> 
                    FileOutputStream(destFile).use { output -> input.copyTo(output) } 
                }
                savedSuccessfully = true
            }

            // Toast to notify user that it has been saved
            if (savedSuccessfully) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Saved to Downloads/Laborbook", android.widget.Toast.LENGTH_LONG).show()
                }
            }

            // 3. Open Share Intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName.replace(".pdf", "").replace("_", " "))
                putExtra(Intent.EXTRA_TEXT, "Please find the attached report generated securely via Laborbook App.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "Failed to generate PDF.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateWorkerReportText(worker: LaborWorker, month: String): String {
        val present = worker.getTotalPresent(month)
        val absent = worker.getTotalAbsent(month)
        val otHours = worker.getTotalOvertimeHours(month)
        val adv = worker.getTotalAdvance(month)
        val totalGross = (present * worker.dailyWage) + (otHours * (worker.dailyWage / 8.0) * 1.5)
        val netPayable = totalGross - adv

        val sb = StringBuilder()
        sb.append("ATTENDANCE & WAGE SLIP\n")
        sb.append("Worker Name: ${worker.name}\n")
        sb.append("Period: $month\n")
        sb.append("Daily Wage Rate: Rs.${worker.dailyWage.toInt()}\n")
        sb.append("--------------------------------------------------\n")
        sb.append("Total Present: ${if (present % 1.0 == 0.0) present.toInt() else present} Days\n")
        sb.append("Total Absent: ${absent.toInt()} Days\n")
        sb.append("Overtime: ${otHours.toInt()} Hours\n")
        sb.append("Advances Taken: Rs.${String.format(Locale.US, "%.1f", adv)}\n")
        sb.append("--------------------------------------------------\n")
        sb.append("NET PAYABLE AMOUNT: Rs.${String.format(Locale.US, "%.1f", netPayable)}\n")
        return sb.toString()
    }
}
