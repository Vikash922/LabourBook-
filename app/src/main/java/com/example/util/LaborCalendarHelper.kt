package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthDayInfo(
    val day: Int,
    val dateKey: String,
    val dow: String,
    val isToday: Boolean,
    val formattedDisplay: String
)

object LaborCalendarHelper {

    val monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthsFull = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val years = listOf("2024", "2025", "2026", "2027", "2028")

    private val dayOfWeekNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val sakamotoTable = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)

    private val todayCal: Calendar = Calendar.getInstance()
    val todayYear: Int = todayCal.get(Calendar.YEAR)
    val todayMonth: Int = todayCal.get(Calendar.MONTH) + 1
    val todayDay: Int = todayCal.get(Calendar.DAY_OF_MONTH)

    /**
     * Parses a string like "Aug 2026", "August 2026", "2026-08", or "Oct 2026"
     * into a Pair of (Year, Month 1..12).
     */
    fun parseYearMonth(monthStr: String): Pair<Int, Int> {
        val trimmed = monthStr.trim()
        if (trimmed.isBlank()) {
            return Pair(todayYear, todayMonth)
        }

        // Try format "2026-08"
        if (trimmed.contains("-") && trimmed.length >= 7) {
            val parts = trimmed.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: todayYear
            val m = parts.getOrNull(1)?.toIntOrNull() ?: todayMonth
            return Pair(y, m)
        }

        // Try format "Aug 2026" or "October 2026"
        val parts = trimmed.split(" ", "_", ",")
        var year = todayYear
        var month = todayMonth

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
     * Returns the exact number of days in a given month/year without object allocations.
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            else -> 30
        }
    }

    /**
     * Fast Sakamoto algorithm for Day of Week. 0 = Sun, 1 = Mon, ..., 6 = Sat.
     */
    fun getDayOfWeekIndex(year: Int, month: Int, day: Int): Int {
        var y = year
        val m = month.coerceIn(1, 12)
        if (m < 3) y -= 1
        val idx = (y + y / 4 - y / 100 + y / 400 + sakamotoTable[m - 1] + day) % 7
        return if (idx < 0) idx + 7 else idx
    }

    /**
     * Returns the short 3-letter day of week name (e.g. "Sat", "Sun") in O(1) time without allocations.
     */
    fun getDayOfWeekShort(year: Int, month: Int, day: Int): String {
        val idx = getDayOfWeekIndex(year, month, day)
        return dayOfWeekNames[idx]
    }

    /**
     * Fast string key generation "yyyy-MM-dd".
     */
    fun getDateKey(year: Int, month: Int, day: Int): String {
        val mStr = if (month < 10) "0$month" else month.toString()
        val dStr = if (day < 10) "0$day" else day.toString()
        return "$year-$mStr-$dStr"
    }

    /**
     * Checks if the given date corresponds to today's local date in O(1) time.
     */
    fun isToday(year: Int, month: Int, day: Int): Boolean {
        return year == todayYear && month == todayMonth && day == todayDay
    }

    /**
     * Precomputes all days info for a given month/year so LazyColumn rows scroll at 60/120 FPS.
     */
    fun getMonthDaysInfo(year: Int, month: Int): List<MonthDayInfo> {
        val totalDays = getDaysInMonth(year, month)
        val list = ArrayList<MonthDayInfo>(totalDays)
        for (day in 1..totalDays) {
            val dateKey = getDateKey(year, month, day)
            val dow = getDayOfWeekShort(year, month, day)
            val isTod = isToday(year, month, day)
            val display = if (day < 10) "0$day $dow" else "$day $dow"
            list.add(
                MonthDayInfo(
                    day = day,
                    dateKey = dateKey,
                    dow = dow,
                    isToday = isTod,
                    formattedDisplay = display
                )
            )
        }
        return list
    }

    fun getTodayDisplayString(): String {
        val dow = dayOfWeekNames[getDayOfWeekIndex(todayYear, todayMonth, todayDay)]
        return if (todayDay < 10) "0$todayDay $dow" else "$todayDay $dow"
    }

    fun getTodayFullDateString(): String {
        return getDateKey(todayYear, todayMonth, todayDay)
    }
}
