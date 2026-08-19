package com.example

import com.example.data.cloud.GoogleDriveBackupService
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashTransaction
import com.example.data.model.DailyAttendance
import com.example.data.model.LaborWorker
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
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
    fun testGoogleDriveBackupJsonGenerationAndParsing() {
        val worker = LaborWorker(
            id = "w-1",
            name = "Ramesh Kumar",
            phoneNumber = "9876543210",
            dailyWage = 900.0,
            attendance = mapOf(
                "2026-08-01" to DailyAttendance(
                    dayNumber = 1,
                    dayOfWeek = "Sat",
                    fullDate = "2026-08-01",
                    status = AttendanceStatus.PRESENT,
                    advanceAmount = 200.0
                )
            )
        )

        val transaction = CashTransaction(
            id = "tx-1",
            dateDisplay = "15 Sat",
            fullDate = "2026-08-15",
            type = TransactionType.CASH_IN,
            amount = 5000.0,
            paymentMethod = PaymentMethod.ONLINE,
            notes = "Client advance payment"
        )

        val profile = UserProfile(
            name = "Jyoti",
            businessName = "Apex Constructions",
            email = "jyoti3322114455@gmail.com"
        )

        val json = GoogleDriveBackupService.generateBackupJson(
            workers = listOf(worker),
            transactions = listOf(transaction),
            profile = profile
        )

        assertTrue(json.contains("Ramesh Kumar"))
        assertTrue(json.contains("Apex Constructions"))
        assertTrue(json.contains("5000"))

        val parseResult = GoogleDriveBackupService.parseBackupJson(json)
        assertTrue(parseResult.isSuccess)

        val backupData = parseResult.getOrNull()
        assertNotNull(backupData)
        assertEquals(1, backupData?.totalWorkers)
        assertEquals(1, backupData?.totalTransactions)
        assertEquals("Ramesh Kumar", backupData?.workers?.first()?.name)
        assertEquals(AttendanceStatus.PRESENT, backupData?.workers?.first()?.attendance?.get("2026-08-01")?.status)
        assertEquals(200.0, backupData?.workers?.first()?.attendance?.get("2026-08-01")?.advanceAmount ?: 0.0, 0.01)
        assertEquals("Apex Constructions", backupData?.userProfile?.businessName)
    }
}

