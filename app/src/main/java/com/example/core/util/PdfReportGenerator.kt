package com.example.core.util

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
import com.example.domain.model.LaborWorker
import com.example.domain.model.CashTransaction
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
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
    
    private fun drawBrandingFooter(canvas: Canvas, context: Context) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val currentDate = sdf.format(Date())
        
        val footerY = PAGE_HEIGHT - 40f
        
        // Line above footer
        val topBorderPaint = Paint().apply { color = Color.parseColor("#e2e8f0"); strokeWidth = 1f }
        canvas.drawLine(MARGIN, footerY - 20f, PAGE_WIDTH - MARGIN, footerY - 20f, topBorderPaint)
        
        // Draw ic_app_logo bitmap
        var logoDrawn = false
        try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inScaled = true
            }
            val logoBitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources, 
                com.example.R.drawable.ic_app_logo, 
                options
            )
            if (logoBitmap != null) {
                val targetSize = 20f
                val srcRect = android.graphics.Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val destRect = android.graphics.RectF(MARGIN, footerY - 10f, MARGIN + targetSize, footerY - 10f + targetSize)
                canvas.drawBitmap(logoBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true })
                logoBitmap.recycle()
                logoDrawn = true
            }
        } catch (_: Exception) {}

        if (!logoDrawn) {
            // Fallback Logo if loading fails
            val logoBgPaint = Paint().apply { color = Color.parseColor("#1656D6"); isAntiAlias = true }
            canvas.drawRoundRect(MARGIN, footerY - 10f, MARGIN + 20f, footerY + 10f, 4f, 4f, logoBgPaint)
            val logoTextPaint = Paint().apply { color = Color.WHITE; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            canvas.drawText("L", MARGIN + 6f, footerY + 5f, logoTextPaint)
        }
        
        // App Label
        val appLabelPaint = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        canvas.drawText("Laborbook App", MARGIN + 26f, footerY + 5f, appLabelPaint)
        
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
        val stats = worker.calculateMonthStats(month)
        val present = stats.presentCount
        val absent = stats.absentCount
        val otHours = stats.overtimeHours
        val adv = stats.totalAdvance
        val netPayable = stats.balance
        
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
                    drawBrandingFooter(canvas, context)
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
                    AttendanceStatus.HALF_DAY -> "Half Day (0.5)"
                    AttendanceStatus.PRESENT_HALF -> "1.5 Day"
                    AttendanceStatus.DOUBLE -> "Double (2.0)"
                    AttendanceStatus.PAID_LEAVE -> "Paid Leave"
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
        
        drawBrandingFooter(canvas, context)
        document.finishPage(page)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        saveAndSharePdf(context, document, "Worker_Report_${worker.name.replace(" ", "_")}_$timestamp.pdf")
    }
    
    private fun formatTxDateForPdf(tx: CashTransaction): String {
        val targetFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        // 1. Try fullDate (e.g. "2026-08-21" or "21/08/2026")
        if (tx.fullDate.isNotBlank()) {
            val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd")
            for (pattern in formats) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    val date = sdf.parse(tx.fullDate)
                    if (date != null) return targetFormat.format(date)
                } catch (_: Exception) {}
            }
        }

        // 2. Try dateDisplay if it's already a full date string like "Aug 21, 2026"
        if (tx.dateDisplay.isNotBlank()) {
            val formats = listOf("MMM dd, yyyy", "MMM d, yyyy", "dd MMM yyyy", "d MMM yyyy", "dd MMM, yyyy")
            for (pattern in formats) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    val date = sdf.parse(tx.dateDisplay)
                    if (date != null) return targetFormat.format(date)
                } catch (_: Exception) {}
            }
        }

        // 3. Fallback to timestamp if available
        if (tx.timestamp > 1000000000L) {
            return targetFormat.format(Date(tx.timestamp))
        }

        // 4. Fallback if dateDisplay is "17 Mon" or "15 Sat"
        if (tx.dateDisplay.isNotBlank()) {
            val parts = tx.dateDisplay.trim().split(" ")
            if (parts.isNotEmpty() && parts[0].all { it.isDigit() }) {
                val cal = Calendar.getInstance()
                if (tx.timestamp > 0) cal.timeInMillis = tx.timestamp
                val monthYearStr = SimpleDateFormat("MMM yyyy", Locale.US).format(cal.time)
                val dayNum = parts[0].padStart(2, '0')
                return "$dayNum $monthYearStr"
            }
            return tx.dateDisplay
        }

        return targetFormat.format(Date())
    }

    fun shareCashBookReportPdf(context: Context, transactions: List<CashTransaction>, startDate: String, endDate: String) {
        val validTransactions = transactions.filter { it.amount > 0.0 }
        val document = PdfDocument()
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas
        var y = MARGIN

        // Title Paints
        val primaryHeaderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dateBadgePaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Summary Card Paints
        val cardBgIn = Paint().apply { color = Color.parseColor("#E8F8F0"); isAntiAlias = true }
        val cardBorderIn = Paint().apply { color = Color.parseColor("#A7F3D0"); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val cardTextIn = Paint().apply { color = Color.parseColor("#047857"); textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val cardValIn = Paint().apply { color = Color.parseColor("#1E9E5A"); textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        val cardBgOut = Paint().apply { color = Color.parseColor("#FDE8E8"); isAntiAlias = true }
        val cardBorderOut = Paint().apply { color = Color.parseColor("#FECACA"); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val cardTextOut = Paint().apply { color = Color.parseColor("#B91C1C"); textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val cardValOut = Paint().apply { color = Color.parseColor("#E23E3E"); textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        val cardBgBalIn = Paint().apply { color = Color.parseColor("#EAF1FF"); isAntiAlias = true }
        val cardBorderBalIn = Paint().apply { color = Color.parseColor("#BFDBFE"); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val cardTextBalIn = Paint().apply { color = Color.parseColor("#1656D6"); textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val cardValBalIn = Paint().apply { color = Color.parseColor("#1656D6"); textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Table Header & Grid Paints
        val tableHeaderBg = Paint().apply { color = Color.parseColor("#0F172A"); isAntiAlias = true }
        val tableHeaderFont = Paint().apply { color = Color.WHITE; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val tableHeaderRightFont = Paint().apply { color = Color.WHITE; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT; isAntiAlias = true }

        val rowEvenBg = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        val rowOddBg = Paint().apply { color = Color.parseColor("#F8FAFC"); isAntiAlias = true }
        val rowTotalBg = Paint().apply { color = Color.parseColor("#F1F5F9"); isAntiAlias = true }

        val gridLinePaint = Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f; isAntiAlias = true }
        val gridOuterBorderPaint = Paint().apply { color = Color.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }
        val totalTopLinePaint = Paint().apply { color = Color.parseColor("#94A3B8"); strokeWidth = 1.5f; isAntiAlias = true }

        // Cell Text Paints
        val cellFontNormal = Paint().apply { color = Color.parseColor("#1E293B"); textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val cellFontMuted = Paint().apply { color = Color.parseColor("#64748B"); textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }

        val cellFontAmountIn = Paint().apply { color = Color.parseColor("#1E9E5A"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT; isAntiAlias = true }
        val cellFontAmountOut = Paint().apply { color = Color.parseColor("#E23E3E"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT; isAntiAlias = true }

        // Pill Badge Paints
        val pillBgIn = Paint().apply { color = Color.parseColor("#E8F8F0"); isAntiAlias = true }
        val pillTextIn = Paint().apply { color = Color.parseColor("#1E9E5A"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        val pillBgOut = Paint().apply { color = Color.parseColor("#FDE8E8"); isAntiAlias = true }
        val pillTextOut = Paint().apply { color = Color.parseColor("#E23E3E"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // 1. Header Block with Accent Color Stripe
        val stripePaint = Paint().apply { color = Color.parseColor("#1656D6"); isAntiAlias = true }
        canvas.drawRoundRect(MARGIN, y, MARGIN + 5f, y + 42f, 2.5f, 2.5f, stripePaint)

        canvas.drawText("CASH BOOK STATEMENT", MARGIN + 15f, y + 18f, primaryHeaderPaint)
        canvas.drawText("Period: $startDate - $endDate", MARGIN + 15f, y + 38f, dateBadgePaint)

        val totalIn = validTransactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
        val totalOut = validTransactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
        val balance = totalIn - totalOut

        y += 56f

        // 2. Top Summary Cards (Equal Width)
        val tableLeft = MARGIN
        val tableRight = PAGE_WIDTH - MARGIN
        val totalTableWidth = tableRight - tableLeft // 515f
        val cardGap = 10f
        val cardWidth = (totalTableWidth - 2 * cardGap) / 3f // 165f
        val cardHeight = 52f

        // Cash In Card
        val c1Left = tableLeft
        val c1Right = c1Left + cardWidth
        canvas.drawRoundRect(c1Left, y, c1Right, y + cardHeight, 6f, 6f, cardBgIn)
        canvas.drawRoundRect(c1Left, y, c1Right, y + cardHeight, 6f, 6f, cardBorderIn)
        canvas.drawText("TOTAL CASH IN", c1Left + 12f, y + 18f, cardTextIn)
        val formattedIn = if (totalIn % 1.0 == 0.0) "₹${totalIn.toInt()}" else "₹${String.format(Locale.US, "%,.2f", totalIn)}"
        canvas.drawText(formattedIn, c1Left + 12f, y + 38f, cardValIn)

        // Cash Out Card
        val c2Left = c1Right + cardGap
        val c2Right = c2Left + cardWidth
        canvas.drawRoundRect(c2Left, y, c2Right, y + cardHeight, 6f, 6f, cardBgOut)
        canvas.drawRoundRect(c2Left, y, c2Right, y + cardHeight, 6f, 6f, cardBorderOut)
        canvas.drawText("TOTAL CASH OUT", c2Left + 12f, y + 18f, cardTextOut)
        val formattedOut = if (totalOut % 1.0 == 0.0) "₹${totalOut.toInt()}" else "₹${String.format(Locale.US, "%,.2f", totalOut)}"
        canvas.drawText(formattedOut, c2Left + 12f, y + 38f, cardValOut)

        // Net Balance Card
        val c3Left = c2Right + cardGap
        val c3Right = tableRight
        val dynamicCardBg = if (balance >= 0) cardBgBalIn else cardBgOut
        val dynamicCardBorder = if (balance >= 0) cardBorderBalIn else cardBorderOut
        val dynamicCardText = if (balance >= 0) cardTextBalIn else cardTextOut
        val dynamicCardVal = if (balance >= 0) cardValBalIn else cardValOut

        canvas.drawRoundRect(c3Left, y, c3Right, y + cardHeight, 6f, 6f, dynamicCardBg)
        canvas.drawRoundRect(c3Left, y, c3Right, y + cardHeight, 6f, 6f, dynamicCardBorder)
        canvas.drawText("NET BALANCE", c3Left + 12f, y + 18f, dynamicCardText)
        val formattedBal = if (balance % 1.0 == 0.0) "₹${balance.toInt()}" else "₹${String.format(Locale.US, "%,.2f", balance)}"
        canvas.drawText(formattedBal, c3Left + 12f, y + 38f, dynamicCardVal)

        y += cardHeight + 22f

        // Column X-Coordinates (Perfect spacing and alignment)
        val x0 = tableLeft               // 40f
        val x1 = x0 + 95f                // 135f
        val x2 = x1 + 80f                // 215f
        val x3 = x2 + 60f                // 275f
        val x4 = x3 + 170f               // 445f
        val x5 = tableRight              // 555f

        fun drawTableHeader(c: Canvas, curY: Float) {
            val hHeight = 30f
            c.drawRect(tableLeft, curY, tableRight, curY + hHeight, tableHeaderBg)
            c.drawText("Date", x0 + 10f, curY + 19f, tableHeaderFont)
            c.drawText("Type", x1 + 24f, curY + 19f, tableHeaderFont)
            c.drawText("Mode", x2 + 10f, curY + 19f, tableHeaderFont)
            c.drawText("Notes / Remarks", x3 + 8f, curY + 19f, tableHeaderFont)
            c.drawText("Amount", x5 - 12f, curY + 19f, tableHeaderRightFont)
        }

        drawTableHeader(canvas, y)
        var tableHeaderStartY = y
        y += 30f

        val rowHeight = 32f

        if (validTransactions.isEmpty()) {
            canvas.drawRect(tableLeft, y, tableRight, y + rowHeight, rowEvenBg)
            canvas.drawLine(tableLeft, y + rowHeight, tableRight, y + rowHeight, gridLinePaint)
            canvas.drawText("No transactions logged for this period.", x0 + 12f, y + 20f, cellFontMuted)
            y += rowHeight
        } else {
            for ((index, tx) in validTransactions.withIndex()) {
                if (y + rowHeight > PAGE_HEIGHT - 80f) {
                    canvas.drawRoundRect(tableLeft, tableHeaderStartY, tableRight, y, 6f, 6f, gridOuterBorderPaint)

                    drawBrandingFooter(canvas, context)
                    document.finishPage(page)

                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
                    canvas = page.canvas
                    y = MARGIN

                    drawTableHeader(canvas, y)
                    tableHeaderStartY = y
                    y += 30f
                }

                // Alternating Row Background
                val bg = if (index % 2 == 0) rowEvenBg else rowOddBg
                canvas.drawRect(tableLeft, y, tableRight, y + rowHeight, bg)

                // Horizontal separator bottom line
                canvas.drawLine(tableLeft, y + rowHeight, tableRight, y + rowHeight, gridLinePaint)

                // Cell 1: Date
                val dateText = formatTxDateForPdf(tx)
                canvas.drawText(dateText, x0 + 10f, y + 20f, cellFontNormal)

                // Cell 2: Type (Beautiful Centered Pill Badge)
                val isCashIn = tx.type == TransactionType.CASH_IN
                val badgeStartX = x1 + 8f
                val badgeEndX = x1 + 72f
                val badgeStartY = y + 6f
                val badgeEndY = y + 26f

                if (isCashIn) {
                    canvas.drawRoundRect(badgeStartX, badgeStartY, badgeEndX, badgeEndY, 4f, 4f, pillBgIn)
                    val textWidth = pillTextIn.measureText("CASH IN")
                    val textX = badgeStartX + (64f - textWidth) / 2f
                    canvas.drawText("CASH IN", textX, y + 19f, pillTextIn)
                } else {
                    canvas.drawRoundRect(badgeStartX, badgeStartY, badgeEndX, badgeEndY, 4f, 4f, pillBgOut)
                    val textWidth = pillTextOut.measureText("CASH OUT")
                    val textX = badgeStartX + (64f - textWidth) / 2f
                    canvas.drawText("CASH OUT", textX, y + 19f, pillTextOut)
                }

                // Cell 3: Payment Mode with Premium subtle Pill-like indicator
                val modeStr = tx.paymentMethod.name.uppercase()
                val modeBgPaint = Paint().apply {
                    color = if (modeStr == "ONLINE") Color.parseColor("#EBF5FF") else Color.parseColor("#FFF7ED")
                    isAntiAlias = true
                }
                val modeTextPaint = Paint().apply {
                    color = if (modeStr == "ONLINE") Color.parseColor("#1E40AF") else Color.parseColor("#C2410C")
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val mBadgeX = x2 + 4f
                val mBadgeW = 50f
                canvas.drawRoundRect(mBadgeX, y + 6f, mBadgeX + mBadgeW, y + 26f, 4f, 4f, modeBgPaint)
                val mTextW = modeTextPaint.measureText(modeStr)
                val mTextX = mBadgeX + (mBadgeW - mTextW) / 2f
                canvas.drawText(modeStr, mTextX, y + 19f, modeTextPaint)

                // Cell 4: Notes / Remarks
                var notes = tx.notes.ifBlank { "-" }
                if (notes.length > 32) notes = notes.substring(0, 29) + "..."
                val notesFont = if (notes == "-") cellFontMuted else cellFontNormal
                canvas.drawText(notes, x3 + 8f, y + 20f, notesFont)

                // Cell 5: Amount
                val amtVal = if (tx.amount % 1.0 == 0.0) "${tx.amount.toInt()}" else String.format(Locale.US, "%,.2f", tx.amount)
                val amtText = "₹$amtVal"
                val amtFont = if (isCashIn) cellFontAmountIn else cellFontAmountOut
                canvas.drawText(amtText, x5 - 12f, y + 20f, amtFont)

                y += rowHeight
            }
        }

        // Summary / Total Footer Row
        val totalRowHeight = 32f
        if (y + totalRowHeight > PAGE_HEIGHT - 80f) {
            canvas.drawRoundRect(tableLeft, tableHeaderStartY, tableRight, y, 6f, 6f, gridOuterBorderPaint)
            drawBrandingFooter(canvas, context)
            document.finishPage(page)

            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
            canvas = page.canvas
            y = MARGIN
            tableHeaderStartY = y
        }

        canvas.drawRect(tableLeft, y, tableRight, y + totalRowHeight, rowTotalBg)
        canvas.drawLine(tableLeft, y, tableRight, y, totalTopLinePaint)
        canvas.drawLine(tableLeft, y + totalRowHeight, tableRight, y + totalRowHeight, gridLinePaint)

        val totalFontLabel = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val totalFontValue = Paint().apply {
            color = if (balance >= 0) Color.parseColor("#1E9E5A") else Color.parseColor("#E23E3E")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        canvas.drawText("SUMMARY TOTAL", x0 + 10f, y + 21f, totalFontLabel)
        canvas.drawText("${validTransactions.size} Transactions logged", x3 + 8f, y + 21f, cellFontMuted)

        val totalBalVal = if (balance % 1.0 == 0.0) "${balance.toInt()}" else String.format(Locale.US, "%,.2f", balance)
        canvas.drawText("Net: ₹$totalBalVal", x5 - 12f, y + 21f, totalFontValue)

        y += totalRowHeight

        // Final Outer Rounded Border around Entire Table Section
        canvas.drawRoundRect(tableLeft, tableHeaderStartY, tableRight, y, 6f, 6f, gridOuterBorderPaint)

        drawBrandingFooter(canvas, context)
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
                drawBrandingFooter(canvas, context)
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
            drawBrandingFooter(canvas, context)
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
        
        drawBrandingFooter(canvas, context)
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
        val (year, monthNum) = LaborCalendarHelper.parseYearMonth(month)
        val fullMonthName = "${LaborCalendarHelper.monthsFull.getOrElse(monthNum - 1) { "August" }} $year"
        val monthAttendance = worker.getAttendanceForMonth(month)

        var presentCount = 0
        var absentCount = 0
        var halfDayCount = 0.0
        var presentHalfCount = 0.0
        var doubleCount = 0.0
        var paidLeaveCount = 0.0
        var totalOtHours = 0.0
        var totalAdvance = 0.0
        var totalOtEarnings = 0.0
        val defaultOtRate = if (worker.dailyWage > 0) (worker.dailyWage / 8.0) * 1.5 else 0.0

        for (rec in monthAttendance.values) {
            when (rec.status) {
                AttendanceStatus.PRESENT -> presentCount++
                AttendanceStatus.ABSENT -> absentCount++
                AttendanceStatus.HALF_DAY -> halfDayCount += 1.0
                AttendanceStatus.PRESENT_HALF -> presentHalfCount += 1.0
                AttendanceStatus.DOUBLE -> doubleCount += 1.0
                AttendanceStatus.PAID_LEAVE -> paidLeaveCount += 1.0
                AttendanceStatus.OVERTIME -> presentCount++
                AttendanceStatus.UNMARKED -> {}
            }
            totalOtHours += rec.overtimeHours
            totalAdvance += rec.advanceAmount
            val effectiveOtRate = rec.overtimeRate
            totalOtEarnings += (rec.overtimeHours * effectiveOtRate)
        }

        val effectiveUnits = (presentCount * 1.0) + (halfDayCount * 0.5) + (presentHalfCount * 1.5) + (doubleCount * 2.0) + (paidLeaveCount * 1.0)
        val grossEarnings = (effectiveUnits * worker.dailyWage) + totalOtEarnings
        val netBalance = grossEarnings - totalAdvance

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("         ATTENDANCE & WAGE SLIP          \n")
        sb.append("=========================================\n")
        sb.append("Worker Name  : ${worker.name}\n")
        if (worker.phoneNumber.isNotBlank()) {
            sb.append("Phone        : ${worker.phoneNumber}\n")
        }
        sb.append("Month/Period : $fullMonthName\n")
        sb.append("Daily Wage   : ₹${String.format(Locale.ENGLISH, "%,.2f", worker.dailyWage)}\n")
        sb.append("-----------------------------------------\n")
        sb.append("ATTENDANCE SUMMARY:\n")
        sb.append("  • Present (P)      : $presentCount Days\n")
        sb.append("  • Absent (A)       : $absentCount Days\n")
        sb.append("  • Half Day (½)     : ${if (halfDayCount % 1.0 == 0.0) halfDayCount.toInt() else halfDayCount}\n")
        sb.append("  • Present+Half     : ${String.format(Locale.ENGLISH, "%.1f", presentHalfCount)}\n")
        sb.append("  • Double (P+P)     : ${String.format(Locale.ENGLISH, "%.1f", doubleCount)}\n")
        if (paidLeaveCount > 0) {
            sb.append("  • Paid Leave (PA)  : ${paidLeaveCount.toInt()}\n")
        }
        sb.append("  • Overtime Hours   : ${if (totalOtHours % 1.0 == 0.0) totalOtHours.toInt() else totalOtHours} hrs\n")
        sb.append("-----------------------------------------\n")
        sb.append("PAYMENT SUMMARY:\n")
        sb.append("  • Total Earnings   : ₹${String.format(Locale.ENGLISH, "%,.2f", grossEarnings)}\n")
        sb.append("  • Advance Taken    : ₹${String.format(Locale.ENGLISH, "%,.2f", totalAdvance)}\n")
        sb.append("-----------------------------------------\n")
        sb.append("  BALANCE PAYABLE    : ₹${String.format(Locale.ENGLISH, "%,.2f", netBalance)}\n")
        sb.append("=========================================")
        return sb.toString()
    }
}
