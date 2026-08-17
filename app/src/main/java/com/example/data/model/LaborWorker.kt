package com.example.data.model

import com.example.util.LaborCalendarHelper
import java.util.Locale

enum class AttendanceStatus(val symbol: String) {
    PRESENT("P"),
    ABSENT("A"),
    OVERTIME("OT"),
    HALF_DAY("HD"),
    UNMARKED("-");

    companion object {
        fun fromSymbol(symbol: String): AttendanceStatus {
            return entries.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) } ?: UNMARKED
        }
    }
}

enum class TransactionType {
    CASH_IN,
    CASH_OUT
}

enum class PaymentMethod {
    CASH,
    ONLINE
}

data class DailyAttendance(
    val dayNumber: Int = 1,
    val dayOfWeek: String = "Mon", // "Mon", "Tue", etc.
    val fullDate: String = "",  // "2026-08-01"
    val status: AttendanceStatus = AttendanceStatus.UNMARKED,
    val overtimeHours: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val note: String = ""
)

data class WorkerMonthStats(
    val presentCount: Double = 0.0,
    val absentCount: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val estimatedEarnings: Double = 0.0
)

data class LaborWorker(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val dailyWage: Double = 800.0,
    val skills: List<String> = listOf("Staff", "Worker"),
    val avatarColorHex: String = "#1656D6",
    val attendance: Map<String, DailyAttendance> = emptyMap(), // keyed by date "yyyy-MM-dd" e.g. "2026-08-15"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getAttendanceForMonth(monthStr: String): Map<Int, DailyAttendance> {
        val (year, month) = LaborCalendarHelper.parseYearMonth(monthStr)
        val mStr = if (month < 10) "0$month" else month.toString()
        val prefix = "$year-$mStr"
        val map = mutableMapOf<Int, DailyAttendance>()
        attendance.forEach { (key, value) ->
            if (key.startsWith(prefix)) {
                val day = key.substringAfterLast("-").toIntOrNull() ?: value.dayNumber
                map[day] = value
            } else if (!key.contains("-")) {
                // Legacy support if key is raw integer string like "15"
                val day = key.toIntOrNull() ?: value.dayNumber
                if (year == 2026 && month == 8) {
                    map[day] = value
                }
            }
        }
        return map
    }

    fun calculateMonthStats(monthStr: String = "Aug 2026"): WorkerMonthStats {
        val monthAtt = getAttendanceForMonth(monthStr)
        var present = 0.0
        var absent = 0.0
        var ot = 0.0
        var adv = 0.0

        for (rec in monthAtt.values) {
            when (rec.status) {
                AttendanceStatus.PRESENT, AttendanceStatus.OVERTIME -> present += 1.0
                AttendanceStatus.HALF_DAY -> present += 0.5
                AttendanceStatus.ABSENT -> absent += 1.0
                AttendanceStatus.UNMARKED -> {}
            }
            ot += rec.overtimeHours
            adv += rec.advanceAmount
        }

        val netEarnings = (present * dailyWage) + (ot * (dailyWage / 8.0) * 1.5) - adv
        return WorkerMonthStats(
            presentCount = present,
            absentCount = absent,
            overtimeHours = ot,
            totalAdvance = adv,
            estimatedEarnings = netEarnings
        )
    }

    fun getTotalPresent(monthStr: String = "Aug 2026"): Double = calculateMonthStats(monthStr).presentCount

    fun getTotalAbsent(monthStr: String = "Aug 2026"): Double = calculateMonthStats(monthStr).absentCount

    fun getTotalOvertimeHours(monthStr: String = "Aug 2026"): Double = calculateMonthStats(monthStr).overtimeHours

    fun getTotalAdvance(monthStr: String = "Aug 2026"): Double = calculateMonthStats(monthStr).totalAdvance

    fun getEstimatedEarnings(monthStr: String = "Aug 2026"): Double = calculateMonthStats(monthStr).estimatedEarnings

    val totalPresent: Double
        get() = getTotalPresent("Aug 2026")

    val totalAbsent: Double
        get() = getTotalAbsent("Aug 2026")

    val totalOvertimeHours: Double
        get() = getTotalOvertimeHours("Aug 2026")

    val totalAdvance: Double
        get() = getTotalAdvance("Aug 2026")

    val estimatedEarnings: Double
        get() = getEstimatedEarnings("Aug 2026")
}

data class CashTransaction(
    val id: String = "",
    val dateDisplay: String = "", // e.g. "15 Sat"
    val fullDate: String = "",    // e.g. "2026-08-15"
    val type: TransactionType = TransactionType.CASH_IN,
    val amount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SavedContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val avatarColorHex: String = "#000000",
    val initial: String = name.take(1).uppercase()
)

data class UserProfile(
    val name: String = "Manager",
    val businessName: String = "My Business",
    val mobile: String = "",
    val email: String = "",
    val appLockEnabled: Boolean = false,
    val language: String = "English",
    val isPro: Boolean = true,
    val isCloudSyncEnabled: Boolean = true,
    val isLoggedIn: Boolean = false,
    val authProvider: String = "None",
    val lastDriveBackupTime: String = "Never",
    val lastDriveBackupFile: String = ""
)
