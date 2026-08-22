package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.LaborRepository
import com.example.domain.model.CashTransaction
import com.example.domain.model.LaborWorker
import com.example.domain.model.UserProfile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LogoutBackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun repository(
        sync: suspend (UserProfile, List<LaborWorker>, List<CashTransaction>, Context?) -> Result<String>,
        signOut: suspend (Context) -> Result<Unit> = { Result.success(Unit) }
    ): LaborRepository = LaborRepository(context, sync, signOut)

    private fun setSessionData(repository: LaborRepository) {
        repository.loginWithGoogleAccount("Test User", "logout-${System.nanoTime()}@example.com")
        repository.addWorker("Worker", "9999999999", 500.0)
    }

    @Test
    fun `sync success logs out and clears session data`() = runTest {
        val repository = repository { _, _, _, _ -> Result.success("synced") }
        setSessionData(repository)
        yield()

        val result = repository.backupAndLogout()

        assertTrue(result.isSuccess)
        assertTrue(repository.workers.value.isEmpty())
        assertTrue(repository.transactions.value.isEmpty())
        assertFalse(repository.userProfile.value.isLoggedIn)
    }

    @Test
    fun `sync failure retains local data and reports failure`() = runTest {
        val repository = repository { _, _, _, _ -> Result.failure(Exception("offline")) }
        setSessionData(repository)
        yield()
        val workerCount = repository.workers.value.size

        val result = repository.backupAndLogout()

        assertTrue(result.isFailure)
        assertEquals(workerCount, repository.workers.value.size)
        assertTrue(repository.userProfile.value.isLoggedIn)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("still available"))
    }

    @Test
    fun `sync exception and retry preserve data then allow logout`() = runTest {
        var attempts = 0
        val repository = repository { _, _, _, _ ->
            attempts++
            if (attempts == 1) throw IllegalStateException("timeout")
            Result.success("synced")
        }
        setSessionData(repository)
        yield()

        assertTrue(repository.backupAndLogout().isFailure)
        assertFalse(repository.workers.value.isEmpty())
        assertTrue(repository.backupAndLogout().isSuccess)
        assertTrue(repository.workers.value.isEmpty())
    }

    @Test
    fun `missing local backup fails without clearing data`() = runTest {
        val repository = LaborRepository(null, { _, _, _, _ -> Result.success("synced") })
        setSessionData(repository)

        val result = repository.backupAndLogout()

        assertTrue(result.isFailure)
        assertFalse(repository.workers.value.isEmpty())
        assertTrue(repository.userProfile.value.isLoggedIn)
    }

    @Test
    fun `sign out failure retains data after cloud sync`() = runTest {
        val repository = repository(
            sync = { _, _, _, _ -> Result.success("synced") },
            signOut = { Result.failure(Exception("auth unavailable")) }
        )
        setSessionData(repository)

        val result = repository.backupAndLogout()

        assertTrue(result.isFailure)
        assertFalse(repository.workers.value.isEmpty())
        assertTrue(repository.userProfile.value.isLoggedIn)
    }
}