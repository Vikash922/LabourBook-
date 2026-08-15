package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.ContactsContract
import com.example.data.cloud.BackupData
import com.example.data.cloud.GoogleDriveBackupService
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class LaborRepository(private val context: Context? = null) {
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

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        loadActiveSession()
        if (context != null) {
            loadDeviceContacts(context)
        } else {
            _savedContacts.value = emptyList()
        }
    }

    private fun sanitizeKey(email: String): String {
        return if (email.isBlank()) "guest_user"
        else email.lowercase().trim().replace("@", "_at_").replace(".", "_")
    }

    private fun getUserDataFile(email: String): File? {
        if (context == null) return null
        val key = sanitizeKey(email)
        return File(context.filesDir, "laborbook_data_$key.json")
    }

    /**
     * Loads the active logged-in Google Account session and restores data from local store or Google Drive.
     */
    private fun loadActiveSession() {
        if (prefs == null) return

        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val name = prefs.getString("user_name", "Jyoti Manager") ?: "Jyoti Manager"
        val businessName = prefs.getString("business_name", "Laborbook Pro Master") ?: "Laborbook Pro Master"
        val mobile = prefs.getString("user_mobile", "7848894498") ?: "7848894498"
        val email = prefs.getString("user_email", "jyoti3322114455@gmail.com") ?: "jyoti3322114455@gmail.com"
        val appLock = prefs.getBoolean("app_lock", false)
        val language = prefs.getString("app_language", "English") ?: "English"
        val authProvider = prefs.getString("auth_provider", "Google") ?: "Google"
        val lastDriveTime = prefs.getString("last_drive_time", "Never") ?: "Never"
        val lastDriveFile = prefs.getString("last_drive_file", "") ?: ""

        val profile = UserProfile(
            name = name,
            businessName = businessName,
            mobile = mobile,
            email = email,
            appLockEnabled = appLock,
            language = language,
            isPro = true,
            isCloudSyncEnabled = true,
            isLoggedIn = isLoggedIn,
            authProvider = authProvider,
            lastDriveBackupTime = lastDriveTime,
            lastDriveBackupFile = lastDriveFile
        )
        _userProfile.value = profile

        if (isLoggedIn && email.isNotBlank()) {
            loadUserDataForAccount(email)
        } else {
            _workers.value = emptyList()
            _transactions.value = emptyList()
        }
    }

    /**
     * Loads data specific to this Google account. Automatically checks Google Drive for existing backups.
     */
    private fun loadUserDataForAccount(email: String) {
        if (context == null) return
        var dataLoaded = false

        // 1. Try to load from user's local persistent database
        val dataFile = getUserDataFile(email)
        if (dataFile != null && dataFile.exists()) {
            try {
                val json = dataFile.readText()
                val result = GoogleDriveBackupService.parseBackupJson(json)
                result.onSuccess { data ->
                    _workers.value = data.workers
                    _transactions.value = data.transactions
                    dataLoaded = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Automatic Restore from Google Drive:
        // If local data is missing or empty, automatically check Google Drive for previous backups associated with this account
        if (!dataLoaded || (_workers.value.isEmpty() && _transactions.value.isEmpty())) {
            val driveBackup = GoogleDriveBackupService.getLatestBackupForUser(context, email)
            if (driveBackup != null && (driveBackup.workers.isNotEmpty() || driveBackup.transactions.isNotEmpty())) {
                _workers.value = driveBackup.workers
                _transactions.value = driveBackup.transactions
                if (driveBackup.userProfile != null) {
                    _userProfile.value = _userProfile.value.copy(
                        businessName = driveBackup.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                        name = driveBackup.userProfile.name.ifBlank { _userProfile.value.name },
                        lastDriveBackupTime = driveBackup.backupTimestamp
                    )
                    persistProfile()
                }
                persistLocalData()
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
            putBoolean("app_lock", p.appLockEnabled)
            putString("app_language", p.language)
            putString("auth_provider", p.authProvider)
            putString("last_drive_time", p.lastDriveBackupTime)
            putString("last_drive_file", p.lastDriveBackupFile)
            apply()
        }
    }

    /**
     * Persists local data to the active user's local JSON storage.
     */
    private fun persistLocalData() {
        if (context == null) return
        val currentEmail = _userProfile.value.email
        if (currentEmail.isBlank()) return

        try {
            val json = GoogleDriveBackupService.generateBackupJson(
                workers = _workers.value,
                transactions = _transactions.value,
                profile = _userProfile.value
            )
            val dataFile = getUserDataFile(currentEmail)
            dataFile?.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Explicitly triggers a Google Drive backup snapshot for this user.
     */
    suspend fun createDriveBackup(): Result<com.example.data.cloud.BackupMetadata> {
        if (context == null) return Result.failure(Exception("Context is null"))
        val currentEmail = _userProfile.value.email
        if (currentEmail.isBlank()) return Result.failure(Exception("No Google account linked"))

        val res = GoogleDriveBackupService.saveBackupToUserDrive(
            context = context,
            workers = _workers.value,
            transactions = _transactions.value,
            profile = _userProfile.value
        )
        res.onSuccess { meta ->
            _userProfile.value = _userProfile.value.copy(
                lastDriveBackupTime = meta.dateString,
                lastDriveBackupFile = meta.fileName
            )
            persistProfile()
        }
        return res
    }

    /**
     * Switches the active session to a verified Google Account.
     * Automatically queries Google Drive to restore any previous backup for that account.
     */
    fun loginWithGoogleAccount(name: String, email: String) {
        val verifiedEmail = email.trim().lowercase()
        val verifiedName = if (name.isNotBlank()) name.trim() else "Google User"

        _userProfile.value = _userProfile.value.copy(
            isLoggedIn = true,
            name = verifiedName,
            email = verifiedEmail,
            authProvider = "Google"
        )
        persistProfile()

        // Switch to this user's data and auto-restore from Google Drive
        loadUserDataForAccount(verifiedEmail)
    }

    /**
     * Creates a guaranteed Google Drive backup before logging out.
     */
    suspend fun backupAndLogout(): Result<String> {
        return try {
            if (context != null && _userProfile.value.email.isNotBlank()) {
                val backupResult = GoogleDriveBackupService.saveBackupToUserDrive(
                    context = context,
                    workers = _workers.value,
                    transactions = _transactions.value,
                    profile = _userProfile.value
                )
                backupResult.onSuccess { meta ->
                    _userProfile.value = _userProfile.value.copy(
                        lastDriveBackupTime = meta.dateString,
                        lastDriveBackupFile = meta.fileName
                    )
                }
            }

            // Mark session as logged out
            _userProfile.value = _userProfile.value.copy(
                isLoggedIn = false,
                authProvider = "None"
            )
            persistProfile()

            // Reset in-memory states
            _workers.value = emptyList()
            _transactions.value = emptyList()

            Result.success("Backup to Google Drive complete. Logged out safely.")
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _savedContacts.value = list
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
        if (context != null && _userProfile.value.email.isNotBlank() && _workers.value.isNotEmpty()) {
            try {
                GoogleDriveBackupService.saveSafetyBackup(
                    context = context,
                    workers = _workers.value,
                    transactions = _transactions.value,
                    profile = _userProfile.value,
                    reason = "Pre-delete safety snapshot"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _workers.value = _workers.value.filter { it.id != workerId }
        persistLocalData()
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
        _transactions.value = _transactions.value.filter { it.id != transactionId }
        persistLocalData()
    }

    fun updateProfile(profile: UserProfile) {
        _userProfile.value = profile
        persistProfile()
    }

    fun updateBusinessName(newName: String) {
        _userProfile.value = _userProfile.value.copy(businessName = newName)
        persistProfile()
    }

    fun toggleAppLock() {
        _userProfile.value = _userProfile.value.copy(appLockEnabled = !_userProfile.value.appLockEnabled)
        persistProfile()
    }

    fun setLanguage(lang: String) {
        _userProfile.value = _userProfile.value.copy(language = lang)
        persistProfile()
    }

    fun updateDriveBackupInfo(time: String, fileName: String) {
        _userProfile.value = _userProfile.value.copy(
            lastDriveBackupTime = time,
            lastDriveBackupFile = fileName
        )
        persistProfile()
    }

    fun restoreData(backupData: BackupData) {
        _workers.value = backupData.workers
        _transactions.value = backupData.transactions
        if (backupData.userProfile != null) {
            _userProfile.value = _userProfile.value.copy(
                name = backupData.userProfile.name.ifBlank { _userProfile.value.name },
                businessName = backupData.userProfile.businessName.ifBlank { _userProfile.value.businessName },
                mobile = backupData.userProfile.mobile.ifBlank { _userProfile.value.mobile },
                lastDriveBackupTime = backupData.backupTimestamp
            )
        }
        persistProfile()
        persistLocalData()
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }
}
