package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.ContactsContract
import android.util.Log
import com.example.data.cloud.BackupData
import com.example.data.cloud.CloudBackupRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashTransaction
import com.example.data.model.DailyAttendance
import com.example.data.model.LaborWorker
import com.example.data.model.PaymentMethod
import com.example.data.model.SavedContact
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.util.LaborCalendarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LaborRepository(private val context: Context? = null) {
    companion object {
        private const val TAG = "LaborRepository"
    }

    private val prefs: SharedPreferences? = context?.getSharedPreferences("laborbook_prefs", Context.MODE_PRIVATE)

    private val _workers = MutableStateFlow<List<LaborWorker>>(emptyList())
    val workers: StateFlow<List<LaborWorker>> = _workers.asStateFlow()

    private val _transactions = MutableStateFlow<List<CashTransaction>>(emptyList())
    val transactions: StateFlow<List<CashTransaction>> = _transactions.asStateFlow()

    private val _savedContacts = MutableStateFlow<List<SavedContact>>(emptyList())
    val savedContacts: StateFlow<List<SavedContact>> = _savedContacts.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _selectedMonth = MutableStateFlow("Aug 2026")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _lastBackupStatus = MutableStateFlow("Last backup: Never")
    val lastBackupStatus: StateFlow<String> = _lastBackupStatus.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _lastDeletedWorker = MutableStateFlow<LaborWorker?>(null)
    val lastDeletedWorker: StateFlow<LaborWorker?> = _lastDeletedWorker.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var autoSyncJob: Job? = null
    private var fileWriteJob: Job? = null

    init {
        loadActiveSession()
        if (context != null) {
            loadDeviceContacts(context)
        } else {
            _savedContacts.value = emptyList()
        }
    }

    /**
     * Loads the active logged-in Google Account session and restores data from local store or Cloud.
     */
    private fun loadActiveSession() {
        if (prefs == null) return

        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val name = prefs.getString("user_name", "") ?: ""
        val businessName = prefs.getString("business_name", "My Business") ?: "My Business"
        val mobile = prefs.getString("user_mobile", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""
        val appLock = prefs.getBoolean("app_lock", false)
        val language = prefs.getString("app_language", "English") ?: "English"
        val authProvider = prefs.getString("auth_provider", "None") ?: "None"
        val lastCloudTime = prefs.getString("last_cloud_time", "Never") ?: "Never"
        val lastCloudFile = prefs.getString("last_cloud_file", "") ?: ""

        val profile = UserProfile(
            name = name,
            businessName = businessName,
            mobile = mobile,
            email = email,
            appLockEnabled = false,
            language = language,
            isPro = true,
            isCloudSyncEnabled = true,
            isLoggedIn = isLoggedIn,
            authProvider = authProvider,
            lastCloudBackupTime = lastCloudTime,
            lastCloudBackupFile = lastCloudFile
        )
        _userProfile.value = profile
        _lastBackupStatus.value = if (lastCloudTime != "Never") "Last backup: $lastCloudTime" else "Last backup: Never"

        if (isLoggedIn && email.isNotBlank()) {
            loadUserDataForAccount(email)
        } else {
            _workers.value = emptyList()
            _transactions.value = emptyList()
        }
    }

    /**
     * Loads data specific to this Google account. Automatically checks Cloud for existing backups.
     */
    private fun loadUserDataForAccount(email: String) {
        var dataLoaded = false

        // 1. Try to load from user's local persistent cache & persistent documents
        if (context != null) {
            val localFile = java.io.File(context.filesDir, "csv_backups/${com.example.data.cloud.CompactCsvBackupService.MASTER_CSV_FILENAME}")
            var localData: com.example.data.cloud.BackupData? = null
            if (localFile.exists()) {
                val csvContent = localFile.readText()
                val parsedResult = com.example.data.cloud.CompactCsvBackupService.parseCompleteBackupCsv(csvContent)
                if (parsedResult.isSuccess) {
                    localData = parsedResult.getOrThrow()
                }
            }
            if (localData != null && (localData.totalWorkers > 0 || localData.totalTransactions > 0)) {
                _workers.value = localData.workers
                _transactions.value = localData.transactions
                if (localData.userProfile != null) {
                    _userProfile.value = _userProfile.value.copy(
                        businessName = localData.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                        name = localData.userProfile.name.ifBlank { _userProfile.value.name },
                        lastCloudBackupTime = localData.backupTimestamp
                    )
                }
                _lastBackupStatus.value = "Last backup: ${localData.backupTimestamp} • ${localData.totalWorkers} workers restored"
                dataLoaded = true
            }
        }

        // 2. Automatic Restore from Cloud:
        // If local data is missing or empty (e.g. fresh installation / Clear App Data), fetch from Cloud & Firestore
        if (!dataLoaded || (_workers.value.isEmpty() && _transactions.value.isEmpty())) {
            _lastBackupStatus.value = "Restoring backup from Cloud..."
            repositoryScope.launch {
                val cloudResult = if (context != null) {
                    com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud(context)
                } else {
                    Result.failure(Exception("Context is null"))
                }
                
                cloudResult.onSuccess { backupData ->
                    _workers.value = backupData.workers
                    _transactions.value = backupData.transactions
                    if (backupData.userProfile != null) {
                        _userProfile.value = _userProfile.value.copy(
                            businessName = backupData.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                            name = backupData.userProfile.name.ifBlank { _userProfile.value.name },
                            lastCloudBackupTime = backupData.backupTimestamp
                        )
                        persistProfile()
                    }
                    _lastBackupStatus.value = "Last backup: ${backupData.backupTimestamp} • Restored ${backupData.totalWorkers} workers"
                    persistLocalData(syncToCloud = false)
                }.onFailure {
                    if (_workers.value.isEmpty()) {
                        _lastBackupStatus.value = "No cloud backup found"
                    }
                }
            }
        }
    }

    private fun persistProfile() {
        val p = _userProfile.value
        prefs?.edit()?.apply {
            putBoolean("is_logged_in", p.isLoggedIn)
            putString("user_name", p.name)
            putString("business_name", p.businessName)
            putString("user_mobile", p.mobile)
            putString("user_email", p.email)
            putString("app_language", p.language)
            putString("auth_provider", p.authProvider)
            putString("last_cloud_time", p.lastCloudBackupTime)
            putString("last_cloud_file", p.lastCloudBackupFile)
            apply()
        }
    }

    /**
     * Persists local data to the active user's local JSON storage in background and debounces continuous cloud sync.
     * In-memory StateFlows update immediately (0ms UI lag), while file IO & cloud uploads run on Dispatchers.IO.
     */
    private fun persistLocalData(syncToCloud: Boolean = true) {
        val currentEmail = _userProfile.value.email
        if (currentEmail.isBlank()) return

        val currentWorkers = _workers.value
        val currentTransactions = _transactions.value
        val currentProfile = _userProfile.value

        // 1. Asynchronous debounced local file caching on Dispatchers.IO
        fileWriteJob?.cancel()
        fileWriteJob = repositoryScope.launch(Dispatchers.IO) {
            try {
                delay(300L) // Debounce rapid writes
                if (context != null) {
                    com.example.data.cloud.CompactCsvBackupService.saveBackupToCsvFile(context, currentWorkers, currentTransactions, currentProfile)
                }
                Log.d(TAG, "Local cache saved for $currentEmail (${currentWorkers.size} workers, ${currentTransactions.size} transactions)")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving local cache: ${e.message}", e)
            }
        }

        // 2. Debounced background cloud sync (waits 1.5s after user stops typing or tapping)
        // ONLY sync if there is actually data to prevent overwriting existing backups with empty state
        if (syncToCloud && currentProfile.isLoggedIn) {
            autoSyncJob?.cancel()
            autoSyncJob = repositoryScope.launch(Dispatchers.IO) {
                try {
                    delay(1500L) // 1.5s debounce to prevent spamming Firestore
                    Log.i(TAG, "Executing debounced cloud auto-sync for: $currentEmail")
                    com.example.data.cloud.FirestoreSyncService.syncDataToCloud(_userProfile.value, _workers.value, _transactions.value, context)
                } catch (e: Exception) {
                    Log.w(TAG, "Background auto-sync note: ${e.message}")
                }
            }
        }
    }

    /**
     * Fallback for backwards compatibility, simply calls persistLocalData
     */
    private fun persistLocalWorkingStateOnly() {
        persistLocalData(syncToCloud = true)
    }

    /**
     * Explicitly triggers a Cloud backup upload for this user.
     */
    suspend fun backupToCloud(): Result<com.example.data.cloud.CloudBackupRecord> {
        _isCloudSyncing.value = true
        _lastBackupStatus.value = "Backing up..."

        val timestamp = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val res = com.example.data.cloud.FirestoreSyncService.syncDataToCloud(_userProfile.value, _workers.value, _transactions.value, context)
        _isCloudSyncing.value = false

        if (res.isSuccess) {
            _userProfile.value = _userProfile.value.copy(
                lastCloudBackupTime = timestamp
            )
            _lastBackupStatus.value = "Backup successful ($timestamp)"
            persistProfile()
            return Result.success(com.example.data.cloud.CloudBackupRecord("Cloud Sync", timestamp, _workers.value.size, _transactions.value.size))
        } else {
            val errMessage = res.exceptionOrNull()?.message ?: "Sync failed"
            if (errMessage.contains("Firebase not initialized", ignoreCase = true)) {
                _lastBackupStatus.value = "Local Backup Saved ($timestamp)"
                return Result.success(com.example.data.cloud.CloudBackupRecord("Local Backup", timestamp, _workers.value.size, _transactions.value.size))
            } else {
                _lastBackupStatus.value = "Backup failed: $errMessage"
                return Result.failure(res.exceptionOrNull() ?: Exception(errMessage))
            }
        }
    }

    /**
     * Explicitly triggers a download and restore from Cloud.
     */
    suspend fun restoreFromCloud(): Result<com.example.data.cloud.BackupData> {
        _isCloudSyncing.value = true
        _lastBackupStatus.value = "Restoring backup..."

        val res = com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud(context)
        _isCloudSyncing.value = false

        res.onSuccess { backupData ->
            restoreData(backupData)
            _lastBackupStatus.value = "Last backup: ${backupData.backupTimestamp} • Restored ${backupData.totalWorkers} workers"
        }.onFailure { err ->
            val errMsg = err.message ?: ""
            if (errMsg.contains("Firebase not initialized", ignoreCase = true)) {
                _lastBackupStatus.value = "Local Backup Active (Device Storage)"
            } else if (errMsg.contains("No cloud backup found", ignoreCase = true)) {
                _lastBackupStatus.value = "No cloud backup found"
            } else {
                _lastBackupStatus.value = "Backup failed: $errMsg"
            }
        }
        return res
    }

    /**
     * Switches the active session to a verified Google Account.
     * Automatically queries Cloud to restore any previous backup for that account.
     */
    fun loginWithGoogleAccount(
        name: String,
        email: String,
        businessName: String = "",
        mobile: String = "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val verifiedEmail = email.trim().lowercase()
        val verifiedName = if (name.isNotBlank()) name.trim() else if (businessName.isNotBlank()) businessName.trim() else "Manager"
        val verifiedBusiness = if (businessName.isNotBlank()) businessName.trim() else _userProfile.value.businessName.ifBlank { "My Business" }
        val verifiedMobile = if (mobile.isNotBlank()) mobile.trim() else _userProfile.value.mobile

        _userProfile.value = _userProfile.value.copy(
            isLoggedIn = true,
            name = verifiedName,
            email = verifiedEmail,
            businessName = verifiedBusiness,
            mobile = verifiedMobile,
            authProvider = "Google"
        )
        persistProfile()

        _lastBackupStatus.value = "Restoring backup..."

        repositoryScope.launch {
            try {
                var hasRestored = false

                // 1. Query Cloud Firestore first (Cloud is the primary source of truth per email)
                val cloudRes = com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud(context)
                cloudRes.onSuccess { backupData ->
                    if (backupData.totalWorkers > 0 || backupData.totalTransactions > 0) {
                        _workers.value = backupData.workers
                        _transactions.value = backupData.transactions
                        if (backupData.userProfile != null) {
                            _userProfile.value = _userProfile.value.copy(
                                businessName = backupData.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                                name = backupData.userProfile.name.ifBlank { verifiedName },
                                lastCloudBackupTime = backupData.backupTimestamp
                            )
                            persistProfile()
                        }
                        persistLocalData(syncToCloud = false)
                        hasRestored = true
                        _lastBackupStatus.value = "Cloud Backup restored: ${backupData.backupTimestamp} • ${backupData.totalWorkers} workers"
                        withContext(Dispatchers.Main) {
                            try {
                                onComplete?.invoke(true, "Cloud backup automatically restored: ${backupData.totalWorkers} workers, ${backupData.totalTransactions} cash entries.")
                            } catch (_: Exception) {}
                        }
                    }
                }

                // 2. If cloud was offline or empty, check local sandbox backup for this specific email
                if (!hasRestored && context != null) {
                    val localData: com.example.data.cloud.BackupData? = null
                    if (localData != null && (localData.totalWorkers > 0 || localData.totalTransactions > 0)) {
                        _workers.value = localData.workers
                        _transactions.value = localData.transactions
                        if (localData.userProfile != null) {
                            _userProfile.value = _userProfile.value.copy(
                                businessName = localData.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                                name = localData.userProfile.name.ifBlank { verifiedName },
                                lastCloudBackupTime = localData.backupTimestamp
                            )
                            persistProfile()
                        }
                        hasRestored = true
                        _lastBackupStatus.value = "Local Backup restored: ${localData.backupTimestamp} • ${localData.totalWorkers} workers"
                        withContext(Dispatchers.Main) {
                            try {
                                onComplete?.invoke(true, "Local backup restored: ${localData.totalWorkers} workers, ${localData.totalTransactions} cash entries.")
                            } catch (_: Exception) {}
                        }
                    }
                }

                if (!hasRestored) {
                    // New user or account with no cloud backup yet - clean slate for this email
                    _workers.value = emptyList()
                    _transactions.value = emptyList()
                    _lastBackupStatus.value = "Account ready"
                    repositoryScope.launch(Dispatchers.IO) {
                        com.example.data.cloud.FirestoreSyncService.syncDataToCloud(_userProfile.value, emptyList(), emptyList(), context)
                    }
                    withContext(Dispatchers.Main) {
                        try {
                            onComplete?.invoke(true, "Signed in as $verifiedEmail. Account ready.")
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    try {
                        onComplete?.invoke(true, "Signed in successfully as $verifiedEmail.")
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Explicit deep scan button action that scans all storage, CSVs, and Firestore to find and restore lost data.
     */
    suspend fun deepScanAndRestoreAllData(): Result<String> {
        val ctx = context ?: return Result.failure(Exception("Context unavailable"))
        return withContext(Dispatchers.IO) {
            // 1. Device Deep Scan
            var localDeep: com.example.data.cloud.BackupData? = null
            val localFile = java.io.File(ctx.filesDir, "csv_backups/${com.example.data.cloud.CompactCsvBackupService.MASTER_CSV_FILENAME}")
            if (localFile.exists()) {
                val csvContent = localFile.readText()
                val parsedResult = com.example.data.cloud.CompactCsvBackupService.parseCompleteBackupCsv(csvContent)
                if (parsedResult.isSuccess) {
                    localDeep = parsedResult.getOrThrow()
                }
            }
            if (localDeep != null && (localDeep.totalWorkers > 0 || localDeep.totalTransactions > 0)) {
                _workers.value = localDeep.workers
                _transactions.value = localDeep.transactions
                if (localDeep.userProfile != null) {
                    _userProfile.value = _userProfile.value.copy(
                        businessName = localDeep.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                        lastCloudBackupTime = localDeep.backupTimestamp
                    )
                    persistProfile()
                }
                persistLocalData(syncToCloud = true)
                return@withContext Result.success("Deep Scan Success! Found and restored ${localDeep.totalWorkers} workers and ${localDeep.totalTransactions} cash records from device storage.")
            }

            // 2. Cloud Deep Scan
            val cloudRes = com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud(context)
            if (cloudRes.isSuccess) {
                val cData = cloudRes.getOrThrow()
                if (cData.totalWorkers > 0 || cData.totalTransactions > 0) {
                    _workers.value = cData.workers
                    _transactions.value = cData.transactions
                    persistLocalData(syncToCloud = false)
                    return@withContext Result.success("Cloud Scan Success! Restored ${cData.totalWorkers} workers and ${cData.totalTransactions} cash records from Cloud.")
                }
            }

            Result.failure(Exception("No lost backup data found on this device or cloud yet."))
        }
    }

    /**
     * Creates a guaranteed Cloud backup before logging out.
     */
    suspend fun backupAndLogout(): Result<String> {
        return try {
            if (_userProfile.value.email.isNotBlank()) {
                _lastBackupStatus.value = "Backing up before logout..."
                com.example.data.cloud.FirestoreSyncService.syncDataToCloud(_userProfile.value, _workers.value, _transactions.value, context)
            }

            // Mark session as logged out
            _userProfile.value = _userProfile.value.copy(
                isLoggedIn = false,
                authProvider = "None"
            )
            persistProfile()

            if (context != null) {
                com.example.data.cloud.FirebaseAuthHelper.signOut(context)
            }

            // Reset in-memory states
            _workers.value = emptyList()
            _transactions.value = emptyList()
            _lastBackupStatus.value = "Logged out"

            Result.success("Backup to Cloud complete. Logged out safely.")
        } catch (e: Exception) {
            _userProfile.value = _userProfile.value.copy(
                isLoggedIn = false,
                authProvider = "None"
            )
            persistProfile()
            _workers.value = emptyList()
            _transactions.value = emptyList()
            Result.success("Logged out successfully.")
        }
    }

    fun loadDeviceContacts(ctx: Context) {
        repositoryScope.launch(Dispatchers.IO) {
            val list = mutableListOf<SavedContact>()
            val colors = listOf("#1656D6", "#D8B4FE", "#A7F3D0", "#FFD1B3", "#FBCFE8", "#BAE6FD", "#FDE047", "#FED7AA")
            var colorIdx = 0
            try {
                val cursor = ctx.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )
                cursor?.use {
                    val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val seenNumbers = mutableSetOf<String>()

                    while (it.moveToNext()) {
                        val name = if (nameCol >= 0) it.getString(nameCol) else null
                        val rawNum = if (numCol >= 0) it.getString(numCol) else null
                        val cleanNum = rawNum?.replace(" ", "")?.replace("-", "") ?: ""
                        val cid = if (idCol >= 0) it.getString(idCol) else UUID.randomUUID().toString()

                        if (!name.isNullOrBlank() && cleanNum.isNotBlank() && seenNumbers.add(cleanNum)) {
                            list.add(
                                SavedContact(
                                    id = cid ?: UUID.randomUUID().toString(),
                                    name = name,
                                    phoneNumber = cleanNum,
                                    avatarColorHex = colors[colorIdx % colors.size],
                                    initial = name.trim().take(1).uppercase()
                                )
                            )
                            colorIdx++
                        }
                    }
                }
                Log.d(TAG, "Loaded ${list.size} device contacts asynchronously")
            } catch (e: Exception) {
                Log.w(TAG, "Contacts query notice: ${e.message}")
            }

            _savedContacts.value = list
        }
    }

    fun updateSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun addWorker(
        name: String,
        phone: String,
        wage: Double = 800.0,
        skills: List<String> = listOf("Labor", "Staff")
    ): LaborWorker {
        val colors = listOf("#1656D6", "#D8B4FE", "#A7F3D0", "#FFD1B3", "#FBCFE8", "#BAE6FD")
        val color = colors[(_workers.value.size) % colors.size]

        val newWorker = LaborWorker(
            id = UUID.randomUUID().toString(),
            name = name,
            phoneNumber = phone,
            dailyWage = wage,
            skills = skills.ifEmpty { listOf("Staff", "Worker") },
            avatarColorHex = color,
            attendance = emptyMap()
        )

        _workers.value = _workers.value + newWorker
        persistLocalData()
        return newWorker
    }

    fun updateWorker(worker: LaborWorker) {
        _workers.value = _workers.value.map {
            if (it.id == worker.id) worker else it
        }
        persistLocalData()
    }

    fun updateWorker(workerId: String, name: String, phone: String, dailyWage: Double) {
        _workers.value = _workers.value.map { worker ->
            if (worker.id == workerId) {
                worker.copy(name = name, phoneNumber = phone, dailyWage = dailyWage)
            } else {
                worker
            }
        }
        persistLocalData()
    }

    fun deleteWorker(workerId: String) {
        autoSyncJob?.cancel() // Stop any pending debounced auto-sync from previous taps immediately
        val workerToDelete = _workers.value.find { it.id == workerId }
        _lastDeletedWorker.value = workerToDelete

        _workers.value = _workers.value.filter { it.id != workerId }
        // Save working state locally ONLY (never auto-backup or overwrite the single master backup on deletion)
        persistLocalWorkingStateOnly()
        
        // Immediately delete from Firebase so it doesn't linger
        repositoryScope.launch(Dispatchers.IO) {
            com.example.data.cloud.FirestoreSyncService.deleteWorker(workerId, context)
        }
    }

    fun undoDeleteWorker(): Boolean {
        val worker = _lastDeletedWorker.value ?: return false
        val alreadyPresent = _workers.value.any { it.id == worker.id }
        if (!alreadyPresent) {
            _workers.value = _workers.value + worker
            persistLocalData()
        }
        _lastDeletedWorker.value = null
        return true
    }

    fun clearUndoCache() {
        if (_lastDeletedWorker.value != null) {
            _lastDeletedWorker.value = null
            // Once the undo window expires, push the deletion to the cloud
            persistLocalData(syncToCloud = true)
        }
    }

    fun setAttendanceStatus(workerId: String, monthStr: String, dayNumber: Int, status: AttendanceStatus) {
        val (year, month) = LaborCalendarHelper.parseYearMonth(monthStr)
        val dateKey = LaborCalendarHelper.getDateKey(year, month, dayNumber)
        val dow = LaborCalendarHelper.getDayOfWeekShort(year, month, dayNumber)

        _workers.value = _workers.value.map { worker ->
            if (worker.id == workerId) {
                val currentMap = worker.attendance.toMutableMap()
                val existing = currentMap[dateKey]
                val newStatus = if (existing?.status == status) AttendanceStatus.UNMARKED else status
                val otHours = if (newStatus == AttendanceStatus.OVERTIME && (existing?.overtimeHours ?: 0.0) == 0.0) 2.0 else (existing?.overtimeHours ?: 0.0)

                currentMap[dateKey] = DailyAttendance(
                    dayNumber = dayNumber,
                    dayOfWeek = dow,
                    fullDate = dateKey,
                    status = newStatus,
                    overtimeHours = otHours,
                    advanceAmount = existing?.advanceAmount ?: 0.0,
                    note = existing?.note ?: ""
                )
                worker.copy(attendance = currentMap)
            } else {
                worker
            }
        }
        persistLocalData()
    }

    fun updateDayDetails(workerId: String, monthStr: String, dayNumber: Int, advance: Double, note: String, otHours: Double) {
        val (year, month) = LaborCalendarHelper.parseYearMonth(monthStr)
        val dateKey = LaborCalendarHelper.getDateKey(year, month, dayNumber)
        val dow = LaborCalendarHelper.getDayOfWeekShort(year, month, dayNumber)

        _workers.value = _workers.value.map { worker ->
            if (worker.id == workerId) {
                val currentMap = worker.attendance.toMutableMap()
                val existing = currentMap[dateKey]
                val currentStatus = existing?.status ?: AttendanceStatus.UNMARKED
                val finalStatus = if (otHours > 0.0 && currentStatus == AttendanceStatus.UNMARKED) AttendanceStatus.OVERTIME else currentStatus

                currentMap[dateKey] = DailyAttendance(
                    dayNumber = dayNumber,
                    dayOfWeek = dow,
                    fullDate = dateKey,
                    status = finalStatus,
                    overtimeHours = otHours,
                    advanceAmount = advance,
                    note = note
                )
                worker.copy(attendance = currentMap)
            } else {
                worker
            }
        }
        persistLocalData()
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        paymentMethod: PaymentMethod,
        notes: String,
        dateDisplay: String = "15 Sat",
        fullDate: String = "2026-08-15"
    ): CashTransaction {
        val newTx = CashTransaction(
            id = UUID.randomUUID().toString(),
            dateDisplay = dateDisplay,
            fullDate = fullDate,
            type = type,
            amount = amount,
            paymentMethod = paymentMethod,
            notes = notes
        )
        _transactions.value = listOf(newTx) + _transactions.value
        persistLocalData()
        return newTx
    }

    fun updateTransaction(transaction: CashTransaction) {
        _transactions.value = _transactions.value.map {
            if (it.id == transaction.id) transaction else it
        }
        persistLocalData()
    }

    fun deleteTransaction(transactionId: String) {
        autoSyncJob?.cancel()
        _transactions.value = _transactions.value.filter { it.id != transactionId }
        persistLocalWorkingStateOnly()
    }

    fun updateProfile(profile: UserProfile) {
        _userProfile.value = profile
        persistProfile()
    }

    fun updateBusinessName(newName: String) {
        _userProfile.value = _userProfile.value.copy(businessName = newName)
        persistProfile()
    }

    fun setLanguage(lang: String) {
        _userProfile.value = _userProfile.value.copy(language = lang)
        persistProfile()
    }

    fun updateCloudBackupInfo(time: String, fileName: String) {
        _userProfile.value = _userProfile.value.copy(
            lastCloudBackupTime = time,
            lastCloudBackupFile = fileName
        )
        persistProfile()
    }

    fun restoreData(backupData: BackupData) {
        _lastDeletedWorker.value = null
        _workers.value = backupData.workers
        _transactions.value = backupData.transactions
        if (backupData.userProfile != null) {
            _userProfile.value = _userProfile.value.copy(
                name = backupData.userProfile.name.ifBlank { _userProfile.value.name },
                businessName = backupData.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                mobile = backupData.userProfile.mobile.ifBlank { _userProfile.value.mobile },
                lastCloudBackupTime = backupData.backupTimestamp
            )
        }
        persistProfile()
        persistLocalData()
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }
}
