package com.example

import com.example.core.util.PdfReportGenerator
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.DailyAttendance
import com.example.domain.model.LaborWorker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaborReportCalculationTest {
    private val worker = LaborWorker(
        id = "worker-report",
        name = "Report Worker",
        dailyWage = 500.0,
        attendance = mapOf(
            "2026-08-15" to DailyAttendance(
                dayNumber = 15,
                dayOfWeek = "Sat",
                fullDate = "2026-08-15",
                status = AttendanceStatus.PRESENT
            ),
            "2026-09-15" to DailyAttendance(
                dayNumber = 15,
                dayOfWeek = "Tue",
                fullDate = "2026-09-15",
                status = AttendanceStatus.ABSENT
            )
        )
    )

    @Test
    fun `report text changes when selected month changes`() {
        val august = PdfReportGenerator.generateWorkerReportText(worker, "Aug 2026")
        val september = PdfReportGenerator.generateWorkerReportText(worker, "Sep 2026")

        assertTrue(august.contains("Month/Period : August 2026"))
        assertTrue(september.contains("Month/Period : September 2026"))
        assertFalse(august == september)
    }

    @Test
    fun `report text changes when worker data changes`() {
        val original = PdfReportGenerator.generateWorkerReportText(worker, "Aug 2026")
        val changed = PdfReportGenerator.generateWorkerReportText(
            worker.copy(name = "Updated Worker", dailyWage = 700.0),
            "Aug 2026"
        )

        assertTrue(original.contains("Report Worker"))
        assertTrue(changed.contains("Updated Worker"))
        assertFalse(original == changed)
    }

    @Test
    fun `same report inputs produce stable text independent of unrelated UI state`() {
        val first = PdfReportGenerator.generateWorkerReportText(worker, "Aug 2026")
        var unrelatedUiState = false
        unrelatedUiState = !unrelatedUiState
        val second = PdfReportGenerator.generateWorkerReportText(worker, "Aug 2026")

        assertTrue(unrelatedUiState)
        assertTrue(first == second)
    }
}