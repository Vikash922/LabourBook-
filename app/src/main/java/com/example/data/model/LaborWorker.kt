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
    val dayNumber: Int,
    val dayOfWeek: String, // "Mon", "Tue", etc.
    val fullDate: String,  // "2026-08-01"
    val status: AttendanceStatus = AttendanceStatus.UNMARKED,
    val overtimeHours: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val note: String = ""
)

data class LaborWorker(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val dailyWage: Double = 800.0,
    val skills: List<String> = listOf("Tile worker", "Carpenter", "Painter", "Marble worker"),
    val avatarColorHex: String = "#1656D6",
    val attendance: Map<String, DailyAttendance> = emptyMap(), // keyed by date "yyyy-MM-dd" e.g. "2026-08-15"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getAttendanceForMonth(monthStr: String): Map<Int, DailyAttendance> {
        val (year, month) = LaborCalendarHelper.parseYearMonth(monthStr)
        val prefix = String.format(Locale.US, "%04d-%02d", year, month)
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

    fun getTotalPresent(monthStr: String = "Aug 2026"): Double {
        val monthAtt = getAttendanceForMonth(monthStr)
        return monthAtt.values.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.OVERTIME }.toDouble() +
                (monthAtt.values.count { it.status == AttendanceStatus.HALF_DAY } * 0.5)
    }

    fun getTotalAbsent(monthStr: String = "Aug 2026"): Double {
        val monthAtt = getAttendanceForMonth(monthStr)
        return monthAtt.values.count { it.status == AttendanceStatus.ABSENT }.toDouble()
    }

    fun getTotalOvertimeHours(monthStr: String = "Aug 2026"): Double {
        return getAttendanceForMonth(monthStr).values.sumOf { it.overtimeHours }
    }

    fun getTotalAdvance(monthStr: String = "Aug 2026"): Double {
        return getAttendanceForMonth(monthStr).values.sumOf { it.advanceAmount }
    }

    fun getEstimatedEarnings(monthStr: String = "Aug 2026"): Double {
        val present = getTotalPresent(monthStr)
        val ot = getTotalOvertimeHours(monthStr)
        val adv = getTotalAdvance(monthStr)
        return (present * dailyWage) + (ot * (dailyWage / 8.0) * 1.5) - adv
    }

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
    val id: String,
    val dateDisplay: String, // e.g. "15 Sat"
    val fullDate: String,    // e.g. "2026-08-15"
    val type: TransactionType,
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SavedContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarColorHex: String,
    val initial: String = name.take(1).uppercase()
)

data class UserProfile(
    val name: String = "Manager",
    val businessName: String = "Laborbook Pro Master",
    val mobile: String = "7848894498",
    val email: String = "jyoti3322114455@gmail.com",
    val appLockEnabled: Boolean = false,
    val language: String = "English",
    val isPro: Boolean = true,
    val isCloudSyncEnabled: Boolean = true,
    val isLoggedIn: Boolean = false,
    val authProvider: String = "None",
    val lastDriveBackupTime: String = "Never",
    val lastDriveBackupFile: String = ""
)
