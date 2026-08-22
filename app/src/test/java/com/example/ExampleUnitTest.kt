package com.example

import com.example.core.util.LaborCalendarHelper
import com.example.core.util.PdfReportGenerator
import com.example.data.remote.CompactCsvBackupService
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CashTransaction
import com.example.domain.model.DailyAttendance
import com.example.domain.model.LaborWorker
import com.example.domain.model.PaymentMethod
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testScenarioA_DailyWageEarnings() {
        // Daily ₹750, 20 present days -> 20 * 750 = 15,000
        val attendanceMap = mutableMapOf<String, DailyAttendance>()
        for (day in 1..20) {
            val key = String.format("2026-08-%02d", day)
            attendanceMap[key] = DailyAttendance(
                dayNumber = day,
                dayOfWeek = "Day",
                fullDate = key,
                status = AttendanceStatus.PRESENT
            )
        }

        val worker = LaborWorker(
            id = "w-daily",
            name = "Ramesh",
            dailyWage = 750.0,
            salaryType = "Daily",
            attendance = attendanceMap
        )

        val stats = worker.calculateMonthStats("Aug 2026")
        assertEquals(20.0, stats.presentCount, 0.001)
        assertEquals(15000.0, stats.estimatedEarnings, 0.001)
        assertEquals(15000.0, stats.balance, 0.001)
    }

    @Test
    fun testScenarioB_MonthlySalaryEarnings_PartialAttendance() {
        // Monthly ₹15,000, 20 present days in a 30-day month (e.g. Sept 2026) -> (20 / 30.0) * 15,000 = 10,000
        val attendanceMap = mutableMapOf<String, DailyAttendance>()
        for (day in 1..20) {
            val key = String.format("2026-09-%02d", day)
            attendanceMap[key] = DailyAttendance(
                dayNumber = day,
                dayOfWeek = "Day",
                fullDate = key,
                status = AttendanceStatus.PRESENT
            )
        }

        val worker = LaborWorker(
            id = "w-monthly",
            name = "Suresh",
            dailyWage = 15000.0,
            salaryType = "Monthly",
            attendance = attendanceMap
        )

        val stats = worker.calculateMonthStats("Sep 2026")
        assertEquals(20.0, stats.presentCount, 0.001)
        assertEquals(10000.0, stats.estimatedEarnings, 0.01)
        assertEquals(10000.0, stats.balance, 0.01)
    }

    @Test
    fun testScenarioC_MonthlySalary_ZeroPresentDays() {
        // Monthly ₹15,000 with 0 present days -> 0
        val worker = LaborWorker(
            id = "w-monthly-zero",
            name = "Mahesh",
            dailyWage = 15000.0,
            salaryType = "Monthly",
            attendance = emptyMap()
        )

        val stats = worker.calculateMonthStats("Aug 2026")
        assertEquals(0.0, stats.presentCount, 0.001)
        assertEquals(0.0, stats.estimatedEarnings, 0.001)
        assertEquals(0.0, stats.balance, 0.001)
    }

    @Test
    fun testScenarioD_MonthlySalary_FullMonthAttendance() {
        // Monthly ₹15,000 with full-month attendance -> 15,000
        val attendanceMapAug = mutableMapOf<String, DailyAttendance>()
        for (day in 1..31) {
            val key = String.format("2026-08-%02d", day)
            attendanceMapAug[key] = DailyAttendance(
                dayNumber = day,
                dayOfWeek = "Day",
                fullDate = key,
                status = AttendanceStatus.PRESENT
            )
        }

        val workerAug = LaborWorker(
            id = "w-monthly-full-aug",
            name = "Dinesh",
            dailyWage = 15000.0,
            salaryType = "Monthly",
            attendance = attendanceMapAug
        )

        val statsAug = workerAug.calculateMonthStats("Aug 2026")
        assertEquals(31.0, statsAug.presentCount, 0.001)
        assertEquals(15000.0, statsAug.estimatedEarnings, 0.001)
        assertEquals(15000.0, statsAug.balance, 0.001)
    }

    @Test
    fun testScenarioE_and_F_DailyToMonthlyAndMonthlyToDailySwitch() {
        // Worker starts as Daily ₹500
        val attendanceMap = mutableMapOf<String, DailyAttendance>()
        for (day in 1..15) {
            val key = String.format("2026-09-%02d", day)
            attendanceMap[key] = DailyAttendance(
                dayNumber = day,
                dayOfWeek = "Day",
                fullDate = key,
                status = AttendanceStatus.PRESENT
            )
        }

        var worker = LaborWorker(
            id = "w-switch",
            name = "Vikas",
            dailyWage = 500.0,
            salaryType = "Daily",
            attendance = attendanceMap
        )

        // As Daily: 15 * 500 = 7500
        assertEquals(7500.0, worker.calculateMonthStats("Sep 2026").estimatedEarnings, 0.01)

        // E: Switch Daily -> Monthly ₹15,000
        worker = worker.copy(dailyWage = 15000.0, salaryType = "Monthly")
        // As Monthly: (15 / 30) * 15,000 = 7500
        assertEquals(7500.0, worker.calculateMonthStats("Sep 2026").estimatedEarnings, 0.01)

        // F: Switch Monthly -> Daily ₹600
        worker = worker.copy(dailyWage = 600.0, salaryType = "Daily")
        // As Daily: 15 * 600 = 9000
        assertEquals(9000.0, worker.calculateMonthStats("Sep 2026").estimatedEarnings, 0.01)
    }

    @Test
    fun testScenarioG_CsvBackupRestoreWithSalaryTypeAndBackwardCompatibility() {
        val workerDaily = LaborWorker(
            id = "w-1",
            name = "Ramesh Kumar",
            phoneNumber = "9876543210",
            dailyWage = 750.0,
            salaryType = "Daily"
        )
        val workerMonthly = LaborWorker(
            id = "w-2",
            name = "Suresh Verma",
            phoneNumber = "9123456780",
            dailyWage = 18000.0,
            salaryType = "Monthly"
        )

        val profile = UserProfile(
            name = "Jyoti Contractor",
            businessName = "Apex Constructions",
            email = "jyoti@example.com"
        )

        val csv = CompactCsvBackupService.generateCompleteBackupCsv(
            listOf(workerDaily, workerMonthly),
            emptyList(),
            profile
        )

        assertTrue(csv.contains("Daily"))
        assertTrue(csv.contains("Monthly"))

        val parsed = CompactCsvBackupService.parseCompleteBackupCsv(csv)
        assertTrue(parsed.isSuccess)
        val data = parsed.getOrNull()
        assertNotNull(data)
        assertEquals(2, data!!.workers.size)

        val restoredDaily = data.workers.find { it.id == "w-1" }
        assertNotNull(restoredDaily)
        assertEquals("Daily", restoredDaily!!.salaryType)
        assertEquals(750.0, restoredDaily.dailyWage, 0.01)

        val restoredMonthly = data.workers.find { it.id == "w-2" }
        assertNotNull(restoredMonthly)
        assertEquals("Monthly", restoredMonthly!!.salaryType)
        assertEquals(18000.0, restoredMonthly.dailyWage, 0.01)

        // Test backward compatibility: legacy 6-token line without SalaryType column defaults to "Daily"
        val legacyCsv = """
            [SECTION_WORKERS]
            WorkerId,Name,PhoneNumber,DailyWage,AvatarColorHex,CreatedAt
            w-legacy,Old Worker,9999999999,500.0,#1656D6,1700000000000
        """.trimIndent()
        val legacyParsed = CompactCsvBackupService.parseCompleteBackupCsv(legacyCsv)
        assertTrue(legacyParsed.isSuccess)
        val legacyWorker = legacyParsed.getOrNull()?.workers?.firstOrNull()
        assertNotNull(legacyWorker)
        assertEquals("Daily", legacyWorker!!.salaryType)
        assertEquals(500.0, legacyWorker.dailyWage, 0.01)
    }

    @Test
    fun testScenarioH_AdvanceDeductionConsistentAcrossSalaryTypes() {
        val attendanceMap = mutableMapOf<String, DailyAttendance>()
        attendanceMap["2026-09-01"] = DailyAttendance(
            dayNumber = 1,
            dayOfWeek = "Tue",
            fullDate = "2026-09-01",
            status = AttendanceStatus.PRESENT,
            advanceAmount = 500.0
        )

        // Daily worker: 1 * 1000 - 500 adv = 500
        val workerDaily = LaborWorker(
            id = "w-adv-daily",
            name = "A",
            dailyWage = 1000.0,
            salaryType = "Daily",
            attendance = attendanceMap
        )
        val statsDaily = workerDaily.calculateMonthStats("Sep 2026")
        assertEquals(500.0, statsDaily.totalAdvance, 0.01)
        assertEquals(500.0, statsDaily.balance, 0.01)

        // Monthly worker: (1/30)*30000 = 1000 - 500 adv = 500
        val workerMonthly = LaborWorker(
            id = "w-adv-monthly",
            name = "B",
            dailyWage = 30000.0,
            salaryType = "Monthly",
            attendance = attendanceMap
        )
        val statsMonthly = workerMonthly.calculateMonthStats("Sep 2026")
        assertEquals(500.0, statsMonthly.totalAdvance, 0.01)
        assertEquals(500.0, statsMonthly.balance, 0.01)
    }

    @Test
    fun testScenarioI_WhatsAppReportTextFormatting() {
        val workerDaily = LaborWorker(
            id = "w-1",
            name = "Amit",
            dailyWage = 800.0,
            salaryType = "Daily"
        )
        val textDaily = PdfReportGenerator.generateWorkerReportText(workerDaily, "Aug 2026")
        assertTrue(textDaily.contains("Daily Wage   : ₹800.00"))

        val workerMonthly = LaborWorker(
            id = "w-2",
            name = "Rohit",
            dailyWage = 25000.0,
            salaryType = "Monthly"
        )
        val textMonthly = PdfReportGenerator.generateWorkerReportText(workerMonthly, "Aug 2026")
        assertTrue(textMonthly.contains("Monthly Salary   : ₹25,000.00"))
    }
}
