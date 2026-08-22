package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.CompactCsvBackupService
import com.example.data.repository.LaborRepository
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CashTransaction
import com.example.domain.model.DailyAttendance
import com.example.domain.model.LaborWorker
import com.example.domain.model.PaymentMethod
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
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
  fun `test complete csv backup and restore roundtrip with salaryType`() {
    val sampleWorker = LaborWorker(
      id = "w_test_1",
      name = "Ramesh Kumar",
      phoneNumber = "9876543210",
      dailyWage = 15000.0,
      salaryType = "Monthly",
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
    assertTrue(csvString.contains("Monthly"))
    assertTrue(csvString.contains("[SECTION_ATTENDANCE_LOGS]"))
    assertTrue(csvString.contains("2026-08-15"))
    assertTrue(csvString.contains("[SECTION_TRANSACTIONS]"))
    assertTrue(csvString.contains("Token advance"))

    // Parse CSV back
    val parseResult = CompactCsvBackupService.parseCompleteBackupCsv(csvString)
    assertTrue(parseResult.isSuccess)

    val restoredData = parseResult.getOrNull()
    assertNotNull(restoredData)
    assertEquals(1, restoredData!!.workers.size)
    assertEquals("Ramesh Kumar", restoredData.workers[0].name)
    assertEquals("Monthly", restoredData.workers[0].salaryType)
    assertEquals(15000.0, restoredData.workers[0].dailyWage, 0.01)
    assertEquals(2, restoredData.workers[0].attendance.size)
    assertEquals(AttendanceStatus.PRESENT, restoredData.workers[0].attendance["2026-08-15"]?.status)
    assertEquals(2.0, restoredData.workers[0].attendance["2026-08-15"]?.overtimeHours ?: 0.0, 0.01)
    assertEquals(100.0, restoredData.workers[0].attendance["2026-08-15"]?.advanceAmount ?: 0.0, 0.01)
    assertEquals(1, restoredData.transactions.size)
    assertEquals(5000.0, restoredData.transactions[0].amount, 0.01)
  }

  @Test
  fun `test worker repository add and update salaryType persistence`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LaborRepository(context)

    // Add a worker with Monthly salary
    val worker = repo.addWorker("Suresh Raina", "9123456789", 15000.0, "Monthly")
    assertEquals("Monthly", worker.salaryType)
    assertEquals(15000.0, worker.dailyWage, 0.01)

    // Update to Daily wage
    repo.updateWorker(worker.id, "Suresh Raina", "9123456789", 600.0, "Daily")
    val updated = repo.workers.value.find { it.id == worker.id }
    assertNotNull(updated)
    assertEquals("Daily", updated!!.salaryType)
    assertEquals(600.0, updated.dailyWage, 0.01)
  }

  @Test
  fun `test legacy password cleanup removes plain-text pass_ entries from SharedPreferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authPrefs = context.getSharedPreferences("laborbook_auth_accounts", Context.MODE_PRIVATE)
    
    // Simulate legacy storage containing plaintext password
    authPrefs.edit()
      .putString("pass_test@example.com", "PlaintextSecret123!")
      .putString("name_test@example.com", "Test Contractor")
      .putString("pass_other@example.com", "AnotherSecret456!")
      .putString("name_other@example.com", "Other Contractor")
      .apply()

    // Verify legacy entries exist before cleanup
    assertTrue(authPrefs.contains("pass_test@example.com"))
    assertTrue(authPrefs.contains("pass_other@example.com"))

    // Execute cleanup
    com.example.data.remote.FirebaseAuthHelper.cleanupLegacyStoredCredentials(context)

    // Verify all pass_ entries are wiped, while harmless metadata like display names are retained
    org.junit.Assert.assertFalse(authPrefs.contains("pass_test@example.com"))
    org.junit.Assert.assertFalse(authPrefs.contains("pass_other@example.com"))
    assertEquals("Test Contractor", authPrefs.getString("name_test@example.com", null))
    assertEquals("Other Contractor", authPrefs.getString("name_other@example.com", null))
  }

  @Test
  fun `test auth session management login logout and restart flows`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LaborRepository(context)

    // Flow B: User Login
    repo.loginWithGoogleAccount(
      name = "Amit Sharma",
      email = "amit@example.com",
      businessName = "Sharma Builders",
      mobile = "9876543210"
    )

    assertEquals(true, repo.userProfile.value.isLoggedIn)
    assertEquals("amit@example.com", repo.userProfile.value.email)
    assertEquals("Sharma Builders", repo.userProfile.value.businessName)

    // Flow C: App Restart simulation (Re-instantiating repository with persistent session)
    val restartedRepo = LaborRepository(context)
    assertEquals(true, restartedRepo.userProfile.value.isLoggedIn)
    assertEquals("amit@example.com", restartedRepo.userProfile.value.email)
    assertEquals("Sharma Builders", restartedRepo.userProfile.value.businessName)

    // Flow D: Logout
    restartedRepo.loginWithGoogleAccount(
      name = "",
      email = "",
      businessName = "",
      mobile = ""
    )
    // Manually clear login flag to test logout state persistence
    val prefs = context.getSharedPreferences("laborbook_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("is_logged_in", false).apply()

    // Flow C2: App Restart after logout
    val postLogoutRepo = LaborRepository(context)
    assertEquals(false, postLogoutRepo.userProfile.value.isLoggedIn)
  }
}
