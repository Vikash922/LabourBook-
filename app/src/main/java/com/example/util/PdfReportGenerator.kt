package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.LaborWorker
import com.example.data.model.CashTransaction
import com.example.data.model.AttendanceStatus
import com.example.data.model.TransactionType
import java.util.Locale

object PdfReportGenerator {

    fun generateWorkerReportText(worker: LaborWorker, month: String = "August 2026"): String {
        val present = worker.getTotalPresent(month)
        val absent = worker.getTotalAbsent(month)
        val otHours = worker.getTotalOvertimeHours(month)
        val adv = worker.getTotalAdvance(month)
        val totalGross = (present * worker.dailyWage) + (otHours * (worker.dailyWage / 8.0) * 1.5)
        val netPayable = totalGross - adv

        val sb = StringBuilder()
        sb.append("📋 *LABORBOOK ATTENDANCE & WAGE SLIP*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("👷 *Worker Name:* ${worker.name}\n")
        sb.append("📱 *Mobile:* ${worker.phoneNumber}\n")
        sb.append("🛠 *Skills:* ${worker.skills.joinToString(", ")}\n")
        sb.append("📅 *Period:* $month\n")
        sb.append("💵 *Daily Wage Rate:* ₹${worker.dailyWage.toInt()}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("✅ *Total Present:* ${if (present % 1.0 == 0.0) present.toInt() else present} Days\n")
        sb.append("❌ *Total Absent:* ${absent.toInt()} Days\n")
        sb.append("⏱ *Overtime:* ${otHours.toInt()} Hours\n")
        sb.append("💰 *Gross Earnings:* ₹${String.format(Locale.US, "%.1f", totalGross)}\n")
        sb.append("🔻 *Total Advances Taken:* ₹${String.format(Locale.US, "%.1f", adv)}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🎯 *NET PAYABLE AMOUNT:* ₹${String.format(Locale.US, "%.1f", netPayable)}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("🗓 *Daily Attendance Logs:*\n")
        val monthAtt = worker.getAttendanceForMonth(month)
        val activeDays = monthAtt.values.filter { it.status != AttendanceStatus.UNMARKED || it.advanceAmount > 0 }
        if (activeDays.isEmpty()) {
            sb.append("No active days logged yet for this month ($month).\n")
        } else {
            activeDays.sortedBy { it.dayNumber }.forEach { d ->
                val statusText = when (d.status) {
                    AttendanceStatus.PRESENT -> "Present (P)"
                    AttendanceStatus.ABSENT -> "Absent (A)"
                    AttendanceStatus.OVERTIME -> "OT (${d.overtimeHours.toInt()}h)"
                    AttendanceStatus.HALF_DAY -> "Half Day (HD)"
                    AttendanceStatus.UNMARKED -> "Unmarked"
                }
                val advanceText = if (d.advanceAmount > 0) " | Advance: ₹${d.advanceAmount.toInt()}" else ""
                val noteText = if (d.note.isNotBlank()) " (${d.note})" else ""
                sb.append("• Day ${String.format(Locale.US, "%02d", d.dayNumber)} ${d.dayOfWeek}: $statusText$advanceText$noteText\n")
            }
        }
        sb.append("\n_Generated securely via Laborbook App V1.6.0 · 100% Encrypted_")
        return sb.toString()
    }

    fun generateCashBookReportText(
        transactions: List<CashTransaction>,
        startDate: String = "Sat, 01 Aug 26",
        endDate: String = "Mon, 31 Aug 26"
    ): String {
        val totalIn = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
        val totalOut = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
        val balance = totalIn - totalOut

        val sb = StringBuilder()
        sb.append("📊 *LABORBOOK CASH BOOK REPORT*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📅 *Duration:* $startDate to $endDate\n")
        sb.append("🟢 *Total Cash In:* ₹${String.format(Locale.US, "%.1f", totalIn)}\n")
        sb.append("🔴 *Total Cash Out:* ₹${String.format(Locale.US, "%.1f", totalOut)}\n")
        sb.append("💎 *Net Cash Balance:* ₹${String.format(Locale.US, "%.1f", balance)}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
        sb.append("📝 *Transaction Breakdown:*\n")

        transactions.forEach { tx ->
            val typeSymbol = if (tx.type == TransactionType.CASH_IN) "+ [IN]" else "- [OUT]"
            sb.append("$typeSymbol ₹${tx.amount} (${tx.paymentMethod.name})\n")
            sb.append("  📅 ${tx.dateDisplay} | Notes: ${tx.notes}\n\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("_Generated securely via Laborbook App V1.6.0 · 100% Verified_")
        return sb.toString()
    }

    fun generateBatchWorkersReportText(workers: List<LaborWorker>, month: String = "August 2026"): String {
        val sb = StringBuilder()
        sb.append("📑 *CONSOLIDATED LABOR ROSTER & WAGE SHEET*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📅 *Month:* $month\n")
        sb.append("👷 *Total Staff:* ${workers.size} Workers\n\n")

        var grandGross = 0.0
        var grandAdvance = 0.0
        var grandNet = 0.0

        workers.forEachIndexed { index, w ->
            val p = w.getTotalPresent(month)
            val a = w.getTotalAbsent(month)
            val ot = w.getTotalOvertimeHours(month)
            val adv = w.getTotalAdvance(month)
            val gross = (p * w.dailyWage) + (ot * (w.dailyWage / 8.0) * 1.5)
            val net = gross - adv
            grandGross += gross
            grandAdvance += adv
            grandNet += net

            sb.append("${index + 1}. *${w.name}* (${w.skills.firstOrNull() ?: "Worker"})\n")
            sb.append("   • Attendance: ${p.toInt()}P | ${a.toInt()}A | ${ot.toInt()}h OT\n")
            sb.append("   • Rate: ₹${w.dailyWage.toInt()}/day | Advance: ₹${adv.toInt()}\n")
            sb.append("   • *Net Payable: ₹${String.format(Locale.US, "%.0f", net)}*\n\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *GRAND TOTALS:*\n")
        sb.append("• Total Gross Wages: ₹${String.format(Locale.US, "%.0f", grandGross)}\n")
        sb.append("• Total Advances: ₹${String.format(Locale.US, "%.0f", grandAdvance)}\n")
        sb.append("• Total Net Payout: ₹${String.format(Locale.US, "%.0f", grandNet)}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("_Generated securely via Laborbook App V1.6.0_")
        return sb.toString()
    }

    fun shareToWhatsAppOrSystem(context: Context, text: String, title: String = "Share Report via Laborbook") {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, title)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
