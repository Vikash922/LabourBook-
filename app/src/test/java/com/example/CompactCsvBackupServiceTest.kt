package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.CompactCsvBackupService
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.DailyAttendance
import com.example.domain.model.LaborWorker
import com.example.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompactCsvBackupServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `large multi-month snapshot writes both aliases with identical content`() {
        val workers = (1..40).map { workerNumber ->
            val attendance = (0 until 6 * 30).associate { day ->
                val month = day / 30 + 1
                val dayOfMonth = day % 30 + 1
                val date = "2026-${month.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"
                date to DailyAttendance(
                    dayNumber = dayOfMonth,
                    dayOfWeek = "Day",
                    fullDate = date,
                    status = AttendanceStatus.PRESENT
                )
            }
            LaborWorker(
                id = "worker-$workerNumber",
                name = "Worker $workerNumber",
                dailyWage = 500.0,
                attendance = attendance
            )
        }

        val master = CompactCsvBackupService.saveBackupToCsvFile(
            context,
            workers,
            emptyList(),
            UserProfile(email = "backup@example.com")
        )
        val latest = File(master.parentFile, "latest_backup.csv")

        assertTrue(master.exists())
        assertTrue(latest.exists())
        assertEquals(master.readText(), latest.readText())
        assertTrue(master.readText().contains("Worker 40"))
    }

    @Test
    fun `concurrent saves leave a complete non-partial latest snapshot`() = runBlocking {
        val profiles = (1..4).map { index ->
            UserProfile(email = "concurrent-$index@example.com")
        }
        withContext(Dispatchers.Default) {
            profiles.map { profile ->
                async {
                    CompactCsvBackupService.saveBackupToCsvFile(
                        context,
                        listOf(LaborWorker(id = profile.email, name = profile.email)),
                        emptyList(),
                        profile
                    )
                }
            }.awaitAll()
        }

        val directory = File(context.filesDir, "csv_backups")
        val master = File(directory, CompactCsvBackupService.MASTER_CSV_FILENAME)
        val latest = File(directory, "latest_backup.csv")
        assertEquals(master.readText(), latest.readText())
        assertTrue(master.readText().contains("SECTION_WORKERS"))
    }
}