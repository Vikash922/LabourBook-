package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LaborCalendarHelper {

    val monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthsFull = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val years = listOf("2024", "2025", "2026", "2027", "2028")

    /**
     * Parses a string like "Aug 2026", "August 2026", "2026-08", or "Oct 2026"
     * into a Pair of (Year, Month 1..12).
     */
    fun parseYearMonth(monthStr: String): Pair<Int, Int> {
        val trimmed = monthStr.trim()
        if (trimmed.isBlank()) {
            return Pair(2026, 8)
        }

        // Try format "2026-08"
        if (trimmed.contains("-") && trimmed.length >= 7) {
            val parts = trimmed.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 8
            return Pair(y, m)
        }

        // Try format "Aug 2026" or "October 2026"
        val parts = trimmed.split(" ", "_", ",")
        var year = 2026
        var month = 8

        for (part in parts) {
            val p = part.trim()
            val y = p.toIntOrNull()
            if (y != null && y >= 2000) {
                year = y
            } else if (p.isNotBlank()) {
                val idx = monthsShort.indexOfFirst { it.equals(p, ignoreCase = true) }
                if (idx >= 0) {
                    month = idx + 1
                } else {
                    val fullIdx = monthsFull.indexOfFirst { it.equals(p, ignoreCase = true) }
                    if (fullIdx >= 0) {
                        month = fullIdx + 1
                    }
                }
            }
        }

        return Pair(year, month)
    }

    /**
     * Formats (Year, Month) into standard "Aug 2026" format.
     */
    fun formatYearMonth(year: Int, month: Int): String {
        val safeMonth = month.coerceIn(1, 12)
        val mName = monthsShort[safeMonth - 1]
        return "$mName $year"
    }

    /**
     * Formats (Year, Month) into full "August 2026" format.
     */
    fun formatFullYearMonth(year: Int, month: Int): String {
        val safeMonth = month.coerceIn(1, 12)
        val mName = monthsFull[safeMonth - 1]
        return "$mName $year"
    }

    /**
     * Returns the exact number of days in a given month/year (e.g. Aug 2026 -> 31, Sep 2026 -> 30, Feb 2026 -> 28).
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Returns the short 3-letter day of week name for a specific date (e.g. "Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri").
     */
    fun getDayOfWeekShort(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        return SimpleDateFormat("EEE", Locale.US).format(cal.time)
    }

    /**
     * Generates a date key formatted as "yyyy-MM-dd" (e.g. "2026-08-15").
     */
    fun getDateKey(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    /**
     * Checks if the given date corresponds to today's local date.
     */
    fun isToday(year: Int, month: Int, day: Int): Boolean {
        val now = Calendar.getInstance()
        return (now.get(Calendar.YEAR) == year &&
                now.get(Calendar.MONTH) + 1 == month &&
                now.get(Calendar.DAY_OF_MONTH) == day)
    }

    /**
     * Gets today's display string (e.g. "15 Sat").
     */
    fun getTodayDisplayString(): String {
        val now = Calendar.getInstance()
        return SimpleDateFormat("dd EEE", Locale.US).format(now.time)
    }

    /**
     * Gets today's full date string (e.g. "2026-08-15").
     */
    fun getTodayFullDateString(): String {
        val now = Calendar.getInstance()
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
    }
}
