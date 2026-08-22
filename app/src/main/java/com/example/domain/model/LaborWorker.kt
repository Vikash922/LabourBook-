package com.example.domain.model

import androidx.compose.runtime.Stable
import com.example.core.util.LaborCalendarHelper
import java.util.Locale

enum class AttendanceStatus(val symbol: String) {
    PRESENT("P"),
    ABSENT("A"),
    HALF_DAY("½"),
    PRESENT_HALF("P + ½"),
    DOUBLE("P + P"),
    PAID_LEAVE("PA"),
    OVERTIME("OT"),
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

@Stable
data class DailyAttendance(
    val dayNumber: Int = 1,
    val dayOfWeek: String = "Mon", // "Mon", "Tue", etc.
    val fullDate: String = "",  // "2026-08-01"
    val status: AttendanceStatus = AttendanceStatus.UNMARKED,
    val overtimeHours: Double = 0.0,
    val overtimeRate: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.ONLINE
)

@Stable
data class WorkerMonthStats(
    val presentCount: Double = 0.0,
    val absentCount: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val estimatedEarnings: Double = 0.0,
    val halfDayCount: Double = 0.0,
    val doubleCount: Double = 0.0,
    val presentHalfCount: Double = 0.0,
    val paidLeaveCount: Double = 0.0,
    val balance: Double = 0.0
)

@Stable
data class LaborWorker(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val dailyWage: Double = 0.0,
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
        var halfDays = 0.0
        var doubles = 0.0
        var presentHalfs = 0.0
        var paidLeaves = 0.0
        var totalOtAmount = 0.0
        val defaultOtRatePerHour = if (dailyWage > 0) (dailyWage / 8.0) * 1.5 else 0.0

        for (rec in monthAtt.values) {
            when (rec.status) {
                AttendanceStatus.PRESENT -> present += 1.0
                AttendanceStatus.OVERTIME -> present += 1.0
                AttendanceStatus.HALF_DAY -> {
                    present += 0.5
                    absent += 0.5
                    halfDays += 1.0
                }
                AttendanceStatus.PRESENT_HALF -> {
                    present += 1.5
                    presentHalfs += 1.0
                }
                AttendanceStatus.DOUBLE -> {
                    present += 2.0
                    doubles += 1.0
                }
                AttendanceStatus.PAID_LEAVE -> {
                    present += 1.0
                    paidLeaves += 1.0
                }
                AttendanceStatus.ABSENT -> absent += 1.0
                AttendanceStatus.UNMARKED -> {}
            }
            ot += rec.overtimeHours
            adv += rec.advanceAmount

            val effectiveRate = rec.overtimeRate
            totalOtAmount += (rec.overtimeHours * effectiveRate)
        }

        val netEarnings = (present * dailyWage) + totalOtAmount - adv
        return WorkerMonthStats(
            presentCount = present,
            absentCount = absent,
            overtimeHours = ot,
            totalAdvance = adv,
            estimatedEarnings = netEarnings,
            halfDayCount = halfDays,
            doubleCount = doubles,
            presentHalfCount = presentHalfs,
            paidLeaveCount = paidLeaves,
            balance = netEarnings
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

@Stable
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

@Stable
data class SavedContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val avatarColorHex: String = "#000000",
    val initial: String = name.take(1).uppercase()
)

@Stable
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
    val lastCloudBackupTime: String = "Never",
    val lastCloudBackupFile: String = ""
)
