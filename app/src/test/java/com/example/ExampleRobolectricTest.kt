package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.CompactCsvBackupService
import com.example.data.cloud.GoogleDriveBackupService
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashTransaction
import com.example.data.model.DailyAttendance
import com.example.data.model.LaborWorker
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Laborbook", appName)
  }

  @Test
  fun `test complete csv backup and restore roundtrip`() {
    val sampleWorker = LaborWorker(
      id = "w_test_1",
      name = "Ramesh Kumar",
      phoneNumber = "9876543210",
      dailyWage = 650.0,
      skills = listOf("Carpenter"),
      attendance = mapOf(
        "2026-08-15" to DailyAttendance(
          dayNumber = 15,
          dayOfWeek = "Sat",
          fullDate = "2026-08-15",
          status = AttendanceStatus.PRESENT,
          overtimeHours = 2.0,
          advanceAmount = 100.0,
          note = "Site work"
        ),
        "2026-08-16" to DailyAttendance(
          dayNumber = 16,
          dayOfWeek = "Sun",
          fullDate = "2026-08-16",
          status = AttendanceStatus.HALF_DAY,
          overtimeHours = 0.0,
          advanceAmount = 0.0,
          note = "Morning only"
        )
      )
    )

    val sampleTransaction = CashTransaction(
      id = "tx_test_1",
      dateDisplay = "15 Sat",
      fullDate = "2026-08-15",
      type = TransactionType.CASH_IN,
      amount = 5000.0,
      paymentMethod = PaymentMethod.ONLINE,
      notes = "Token advance"
    )

    val profile = UserProfile(
      name = "Jyoti Contractor",
      businessName = "Jyoti Constructions",
      email = "jyoti3322114455@gmail.com",
      language = "English"
    )

    // Generate CSV
    val csvString = CompactCsvBackupService.generateCompleteBackupCsv(
      listOf(sampleWorker),
      listOf(sampleTransaction),
      profile
    )

    // Check that CSV contains sections and data
    assertTrue(csvString.contains("[SECTION_WORKERS]"))
    assertTrue(csvString.contains("Ramesh Kumar"))
    assertTrue(csvString.contains("[SECTION_ATTENDANCE_LOGS]"))
    assertTrue(csvString.contains("2026-08-15"))
    assertTrue(csvString.contains("[SECTION_TRANSACTIONS]"))
    assertTrue(csvString.contains("Token advance"))

    // Parse CSV back using Universal Parser
    val parseResult = GoogleDriveBackupService.parseBackupUniversal(csvString)
    assertTrue(parseResult.isSuccess)

    val restoredData = parseResult.getOrNull()
    assertNotNull(restoredData)
    assertEquals(1, restoredData!!.workers.size)
    assertEquals("Ramesh Kumar", restoredData.workers[0].name)
    assertEquals(2, restoredData.workers[0].attendance.size)
    assertEquals(AttendanceStatus.PRESENT, restoredData.workers[0].attendance["2026-08-15"]?.status)
    assertEquals(2.0, restoredData.workers[0].attendance["2026-08-15"]?.overtimeHours ?: 0.0, 0.01)
    assertEquals(100.0, restoredData.workers[0].attendance["2026-08-15"]?.advanceAmount ?: 0.0, 0.01)
    assertEquals(1, restoredData.transactions.size)
    assertEquals(5000.0, restoredData.transactions[0].amount, 0.01)
  }

  @Test
  fun `test delete worker creates safety backup and restore brings worker back`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.LaborRepository(context)
    repo.loginWithGoogleAccount(name = "Test Contractor", email = "test.contractor@gmail.com")

    // Add a worker with attendance
    val worker = repo.addWorker("Suresh Raina", "9123456789", 700.0, listOf("Mason"))
    repo.setAttendanceStatus(worker.id, "Aug 2026", 10, AttendanceStatus.PRESENT)
    repo.updateDayDetails(worker.id, "Aug 2026", 10, 200.0, "Site A", 1.5)

    // Take manual backup
    val backupResult = repo.createDriveBackup()
    assertTrue(backupResult.isSuccess)
    val backupMeta = backupResult.getOrNull()
    assertNotNull(backupMeta)
    assertTrue(backupMeta!!.workerCount >= 1)

    // Save drive backup snapshot locally as well for immediate retrieval in tests
    GoogleDriveBackupService.saveBackupToUserDrive(
        context = context,
        workers = repo.workers.value,
        transactions = repo.transactions.value,
        profile = repo.userProfile.value
    )

    val initialCount = repo.workers.value.size

    // Delete worker
    repo.deleteWorker(worker.id)
    assertEquals(initialCount - 1, repo.workers.value.size)

    // Check that available backups include the backup taken or safety backup
    val backups = GoogleDriveBackupService.getAvailableBackupsForUser(context, repo.userProfile.value.email)
    assertTrue(backups.isNotEmpty())

    // Restore from the backup
    val targetBackup = backups.firstOrNull { it.workerCount >= initialCount && it.file != null }
    assertNotNull(targetBackup)
    val content = targetBackup!!.file!!.readText()
    val parseResult = GoogleDriveBackupService.parseBackupUniversal(content)
    assertTrue(parseResult.isSuccess)

    repo.restoreData(parseResult.getOrNull()!!)

    // Worker must be back in the repository with attendance!
    val restoredWorker = repo.workers.value.find { it.id == worker.id || it.name == "Suresh Raina" }
    assertNotNull(restoredWorker)
    assertEquals("Suresh Raina", restoredWorker!!.name)
    assertEquals(AttendanceStatus.PRESENT, restoredWorker.attendance["2026-08-10"]?.status)
    assertEquals(1.5, restoredWorker.attendance["2026-08-10"]?.overtimeHours ?: 0.0, 0.01)
    assertEquals(200.0, restoredWorker.attendance["2026-08-10"]?.advanceAmount ?: 0.0, 0.01)
  }
}


