package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.BackupMetadata
import com.example.data.remote.CloudSyncService
import com.example.data.remote.CompactCsvBackupService
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CashTransaction
import com.example.domain.model.LaborWorker
import com.example.domain.model.PaymentMethod
import com.example.domain.model.SavedContact
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import com.example.data.repository.LaborRepository
import com.example.core.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object Login : Screen()
    data object LaborHome : Screen()
    data object AddLabor : Screen()
    data class LaborDetail(val workerId: String) : Screen()
    data class LaborReport(val workerId: String) : Screen()
    data object CashBook : Screen()
    data object CashBookReport : Screen()
    data object Settings : Screen()
    data object BatchPdfHub : Screen()
}

class LaborViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LaborRepository(application.applicationContext)

    val workers: StateFlow<List<LaborWorker>> = repository.workers
    val transactions: StateFlow<List<CashTransaction>> = repository.transactions
    val savedContacts: StateFlow<List<SavedContact>> = repository.savedContacts
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val selectedMonth: StateFlow<String> = repository.selectedMonth
    val lastBackupStatus: StateFlow<String> = repository.lastBackupStatus
    val isCloudSyncing: StateFlow<Boolean> = repository.isCloudSyncing
    val lastDeletedWorker: StateFlow<LaborWorker?> = repository.lastDeletedWorker

    fun updateSelectedMonth(month: String) {
        repository.updateSelectedMonth(month)
    }

    fun refreshContacts(context: android.content.Context) {
        repository.loadDeviceContacts(context)
    }

    // Current navigation destination & Bottom Tab index (0 = Labor, 1 = Cash book, 2 = Settings)
    private val _currentScreen = MutableStateFlow<Screen>(
        resolveInitialScreen()
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    init {
        // Automatically check if Firebase authenticated user exists and navigate directly to dashboard
        checkFirebaseAutoLogin(isStartup = true)
    }

    fun onSplashFinished() {
        if (repository.userProfile.value.isLoggedIn) {
            _currentScreen.value = Screen.LaborHome
            _selectedTabIndex.value = 0
        } else {
            val currentFirebaseUser = com.example.data.remote.FirebaseAuthHelper.getCurrentFirebaseUser(getApplication())
            if (currentFirebaseUser != null && !currentFirebaseUser.email.isNullOrBlank()) {
                _currentScreen.value = Screen.LaborHome
                _selectedTabIndex.value = 0
            } else {
                _currentScreen.value = Screen.Login
            }
        }
    }

    private fun resolveInitialScreen(): Screen {
        return Screen.Splash
    }

    /**
     * Checks if a Firebase user is already authenticated on startup and synchronizes session.
     */
    fun checkFirebaseAutoLogin(isStartup: Boolean = false) {
        val currentFbUser = com.example.data.remote.FirebaseAuthHelper.getCurrentFirebaseUser(getApplication())
        if (currentFbUser != null && !currentFbUser.email.isNullOrBlank()) {
            val userEmail = currentFbUser.email!!
            val userName = currentFbUser.displayName ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            if (!repository.userProfile.value.isLoggedIn || repository.userProfile.value.email != userEmail) {
                repository.loginWithGoogleAccount(
                    name = userName,
                    email = userEmail,
                    businessName = repository.userProfile.value.businessName.ifBlank { "My Business" },
                    mobile = repository.userProfile.value.mobile
                )
            }
            // Only switch screen to LaborHome if currently on the Login screen
            if (_currentScreen.value is Screen.Login) {
                _currentScreen.value = Screen.LaborHome
                _selectedTabIndex.value = 0
            }
        }
    }

    // Search filters
    private val _contactsSearchQuery = MutableStateFlow("")
    val contactsSearchQuery: StateFlow<String> = _contactsSearchQuery.asStateFlow()

    private val _workerSearchQuery = MutableStateFlow("")
    val workerSearchQuery: StateFlow<String> = _workerSearchQuery.asStateFlow()

    private val _transactionSearchQuery = MutableStateFlow("")
    val transactionSearchQuery: StateFlow<String> = _transactionSearchQuery.asStateFlow()

    // Add Staff Accordion in AddLaborScreen
    private val _isAddStaffExpanded = MutableStateFlow(false)
    val isAddStaffExpanded: StateFlow<Boolean> = _isAddStaffExpanded.asStateFlow()

    private val _newLaborName = MutableStateFlow("")
    val newLaborName: StateFlow<String> = _newLaborName.asStateFlow()

    private val _newLaborPhone = MutableStateFlow("")
    val newLaborPhone: StateFlow<String> = _newLaborPhone.asStateFlow()

    private val _newLaborWage = MutableStateFlow("800")
    val newLaborWage: StateFlow<String> = _newLaborWage.asStateFlow()

    // Selected Transaction for View / Edit Bottom Sheets
    private val _activeTransaction = MutableStateFlow<CashTransaction?>(null)
    val activeTransaction: StateFlow<CashTransaction?> = _activeTransaction.asStateFlow()

    private val _transactionSheetMode = MutableStateFlow<TransactionSheetMode?>(null)
    val transactionSheetMode: StateFlow<TransactionSheetMode?> = _transactionSheetMode.asStateFlow()

    enum class TransactionSheetMode {
        VIEW,
        EDIT,
        CREATE_IN,
        CREATE_OUT
    }

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    val filteredContacts = combine(savedContacts, contactsSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredWorkers = combine(workers, workerSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions = combine(transactions, transactionSearchQuery) { list, query ->
        val validList = list.filter { it.amount > 0.0 }
        if (query.isBlank()) validList
        else validList.filter { it.notes.contains(query, ignoreCase = true) || it.dateDisplay.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        when (screen) {
            is Screen.Splash -> _selectedTabIndex.value = 0
            is Screen.Login -> _selectedTabIndex.value = 0
            is Screen.LaborHome, is Screen.AddLabor, is Screen.LaborDetail, is Screen.LaborReport -> _selectedTabIndex.value = 0
            is Screen.CashBook, is Screen.CashBookReport -> _selectedTabIndex.value = 1
            is Screen.Settings, is Screen.BatchPdfHub -> _selectedTabIndex.value = 2
        }
    }

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
        when (index) {
            0 -> _currentScreen.value = Screen.LaborHome
            1 -> _currentScreen.value = Screen.CashBook
            2 -> _currentScreen.value = Screen.Settings
        }
    }

    fun toggleAddStaffExpanded() {
        _isAddStaffExpanded.value = !_isAddStaffExpanded.value
    }

    fun onNewLaborNameChanged(name: String) {
        _newLaborName.value = name
    }

    fun onNewLaborPhoneChanged(phone: String) {
        _newLaborPhone.value = phone
    }

    fun onNewLaborWageChanged(wage: String) {
        _newLaborWage.value = wage
    }

    fun onContactsSearchQueryChanged(q: String) {
        _contactsSearchQuery.value = q
    }

    fun onWorkerSearchQueryChanged(q: String) {
        _workerSearchQuery.value = q
    }

    fun onTransactionSearchQueryChanged(q: String) {
        _transactionSearchQuery.value = q
    }

    fun addLaborFromForm(): Boolean {
        val name = _newLaborName.value.trim()
        val phone = _newLaborPhone.value.trim()
        if (name.isBlank() || phone.isBlank()) return false

        val wage = _newLaborWage.value.toDoubleOrNull() ?: 800.0
        repository.addWorker(name, phone, wage)

        // Reset form
        _newLaborName.value = ""
        _newLaborPhone.value = ""
        _isAddStaffExpanded.value = false
        navigateTo(Screen.LaborHome)
        return true
    }

    fun addLaborFromContact(contact: SavedContact, wage: Double = 800.0) {
        repository.addWorker(
            name = contact.name,
            phone = contact.phoneNumber,
            wage = wage
        )
        navigateTo(Screen.LaborHome)
    }

    fun setAttendance(workerId: String, dayNumber: Int, status: AttendanceStatus, monthStr: String = selectedMonth.value) {
        repository.setAttendanceStatus(workerId, monthStr, dayNumber, status)
    }

    fun updateDayDetails(
        workerId: String,
        dayNumber: Int,
        advance: Double,
        note: String,
        otHours: Double,
        otRate: Double = 0.0,
        monthStr: String = selectedMonth.value,
        paymentMethod: PaymentMethod = PaymentMethod.ONLINE
    ) {
        repository.updateDayDetails(workerId, monthStr, dayNumber, advance, note, otHours, otRate, paymentMethod)
    }

    fun updateWorker(workerId: String, name: String, phone: String, dailyWage: Double) {
        repository.updateWorker(workerId, name, phone, dailyWage)
    }

    fun deleteWorker(workerId: String) {
        val worker = workers.value.find { it.id == workerId }
        val name = worker?.name ?: "Worker"
        repository.deleteWorker(workerId)
        
        _syncMessage.value = "$name deleted. Tap to UNDO."
        
        navigateTo(Screen.LaborHome)
    }

    fun undoDeleteWorker() {
        val restored = repository.undoDeleteWorker()
        if (restored) {
            _syncMessage.value = "Worker restored successfully!"
        }
    }

    fun restoreFromSafetyBackup(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val email = userProfile.value.email
            val safetyBackup: com.example.data.remote.BackupData? = null
            if (safetyBackup != null && (safetyBackup.workers.isNotEmpty() || safetyBackup.transactions.isNotEmpty())) {
                repository.restoreData(safetyBackup)
                _syncMessage.value = "Restored ${safetyBackup.totalWorkers} workers from safety snapshot"
                onComplete?.invoke(true, "Successfully restored ${safetyBackup.totalWorkers} workers.")
            } else {
                onComplete?.invoke(false, "No safety recovery snapshot found.")
            }
        }
    }

    fun openTransactionDetail(tx: CashTransaction) {
        _activeTransaction.value = tx
        _transactionSheetMode.value = TransactionSheetMode.VIEW
    }

    fun openTransactionEdit(tx: CashTransaction) {
        _activeTransaction.value = tx
        _transactionSheetMode.value = TransactionSheetMode.EDIT
    }

    fun openNewTransaction(type: TransactionType) {
        val cal = java.util.Calendar.getInstance()
        val fullFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val displayFormat = java.text.SimpleDateFormat("dd EEE", java.util.Locale.getDefault())
        
        _activeTransaction.value = CashTransaction(
            id = "",
            dateDisplay = displayFormat.format(cal.time),
            fullDate = fullFormat.format(cal.time),
            type = type,
            amount = 0.0,
            paymentMethod = PaymentMethod.CASH,
            notes = ""
        )
        _transactionSheetMode.value = if (type == TransactionType.CASH_IN) TransactionSheetMode.CREATE_IN else TransactionSheetMode.CREATE_OUT
    }

    fun closeTransactionSheet() {
        _transactionSheetMode.value = null
        _activeTransaction.value = null
    }

    fun saveTransaction(
        id: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        notes: String,
        type: TransactionType,
        dateDisplay: String,
        fullDate: String
    ) {
        if (amount <= 0.0) {
            closeTransactionSheet()
            return
        }
        if (id.isBlank()) {
            repository.addTransaction(type, amount, paymentMethod, notes, dateDisplay, fullDate)
        } else {
            val updated = CashTransaction(
                id = id,
                dateDisplay = dateDisplay,
                fullDate = fullDate,
                type = type,
                amount = amount,
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.updateTransaction(updated)
        }
        closeTransactionSheet()
    }

    fun deleteTransaction(txId: String) {
        repository.deleteTransaction(txId)
        closeTransactionSheet()
    }

    fun updateBusinessName(newName: String) {
        repository.updateBusinessName(newName)
    }

    fun updateMobile(newMobile: String) {
        repository.updateProfile(userProfile.value.copy(mobile = newMobile))
    }

    fun setLanguage(lang: String) {
        repository.setLanguage(lang)
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.backupToCloud()
            CloudSyncService.syncDataToCloud(workers.value.size, transactions.value.size)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun showMessage(msg: String) {
        _syncMessage.value = msg
    }

    fun getDeviceAccounts(context: android.content.Context): List<String> {
        return com.example.data.remote.FirebaseAuthHelper.getDeviceAccounts(context)
    }

    // Google Authentication Only
    fun loginWithGoogle(
        name: String = "User",
        email: String = "",
        businessName: String = "",
        mobile: String = "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        repository.loginWithGoogleAccount(
            name = name,
            email = email,
            businessName = businessName,
            mobile = mobile,
            onComplete = onComplete
        )
        _currentScreen.value = Screen.LaborHome
        _selectedTabIndex.value = 0
    }

    /**
     * Signs in using Android Google Credential Manager or seamlessly connects the selected Google account.
     */
    fun signInWithGoogleCredentialManager(
        context: android.content.Context,
        businessName: String = "",
        mobile: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = com.example.data.remote.FirebaseAuthHelper.signInWithGoogleCredentialManager(context)
            result.onSuccess { authUser ->
                loginWithGoogle(
                    name = authUser.displayName,
                    email = authUser.email,
                    businessName = businessName,
                    mobile = mobile,
                    onComplete = { success, msg ->
                        onComplete(success, "Authenticated as ${authUser.email}\n$msg")
                    }
                )
            }.onFailure { err ->
                val errMsg = err.message ?: "Google sign-in was cancelled or unavailable."
                onComplete(false, errMsg)
            }
        }
    }

    /**
     * Signs in with Email & Password via Firebase Auth and syncs Firestore data.
     */
    fun signInWithEmail(
        context: android.content.Context,
        email: String,
        pass: String,
        businessName: String = "",
        mobile: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = com.example.data.remote.FirebaseAuthHelper.signInWithEmail(context, email, pass)
            result.onSuccess { authUser ->
                loginWithGoogle(
                    name = authUser.displayName,
                    email = authUser.email,
                    businessName = businessName,
                    mobile = mobile,
                    onComplete = { success, msg ->
                        onComplete(success, "Firebase authenticated: ${authUser.email}\n$msg")
                    }
                )
            }.onFailure { err ->
                val errMsg = err.message ?: "Invalid email or password"
                onComplete(false, errMsg)
            }
        }
    }

    /**
     * Creates a new account with Email & Password via Firebase Auth and initializes Firestore profile.
     */
    fun signUpWithEmail(
        context: android.content.Context,
        email: String,
        pass: String,
        businessName: String,
        mobile: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = com.example.data.remote.FirebaseAuthHelper.signUpWithEmail(context, email, pass, businessName)
            result.onSuccess { authUser ->
                val finalName = if (businessName.isNotBlank()) businessName else authUser.displayName
                loginWithGoogle(
                    name = finalName,
                    email = authUser.email,
                    businessName = businessName.ifBlank { "My Business" },
                    mobile = mobile,
                    onComplete = { success, msg ->
                        onComplete(success, "Account created & synced for ${authUser.email}\n$msg")
                    }
                )
            }.onFailure { err ->
                val errMsg = err.message ?: "Account creation failed"
                onComplete(false, errMsg)
            }
        }
    }



    /**
     * Aggressively scans all device directories and cloud Firestore to recover lost backup data.
     */
    fun deepScanAndRestoreLostData(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.deepScanAndRestoreAllData()
            res.onSuccess { msg ->
                _syncMessage.value = msg
                onComplete(true, msg)
            }.onFailure { err ->
                val msg = err.message ?: "Scan failed"
                _syncMessage.value = msg
                onComplete(false, msg)
            }
        }
    }

    /**
     * Explicitly triggers an immediate Cloud & Cloud backup upload.
     */
    fun backupToCloudNow(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.backupToCloud()
            res.onSuccess { record ->
                val statusMsg = repository.lastBackupStatus.value
                _syncMessage.value = statusMsg
                onComplete(true, statusMsg)
            }.onFailure { err ->
                val statusMsg = "Backup failed: ${err.message}"
                _syncMessage.value = statusMsg
                onComplete(false, statusMsg)
            }
        }
    }

    /**
     * Explicitly downloads and restores the latest snapshot from Cloud & Cloud.
     */
    fun restoreFromCloudNow(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.restoreFromCloud()
            res.onSuccess { backupData ->
                _syncMessage.value = "Restored ${backupData.totalWorkers} workers"
                onComplete(true, "Successfully restored from Cloud!")
            }.onFailure { err ->
                val msg = if (err.message?.contains("No cloud backup found") == true) "No cloud backup found" else (err.message ?: "Restore failed")
                _syncMessage.value = msg
                onComplete(false, msg)
            }
        }
    }

    /**
     * Automatic Backup on Logout:
     * Saves/updates the latest Cloud backup for this specific account, then logs out.
     */
    fun logoutWithCloudBackup(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoggingOut.value = true
            val result = repository.backupAndLogout()
            _isLoggingOut.value = false
            _currentScreen.value = Screen.Login
            _selectedTabIndex.value = 0
            onComplete(true, result.getOrNull() ?: "Backed up to Cloud & Logged out.")
        }
    }

    // Cloud Backup & Restore Methods

    fun exportAndShareBackupCsv(context: android.content.Context, onComplete: ((Boolean, String) -> Unit)? = null) {
        val result = CompactCsvBackupService.shareBackupCsvFile(
            context = context,
            workers = workers.value,
            transactions = transactions.value,
            profile = userProfile.value
        )
        result.onSuccess {
            onComplete?.invoke(true, "CSV Backup ready to share/export.")
        }.onFailure { err ->
            onComplete?.invoke(false, "Failed to share CSV: ${err.message}")
        }
    }

    fun saveCsvBackupToDevice(context: android.content.Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = CompactCsvBackupService.saveBackupCsvToDeviceDownloads(
                context = context,
                workers = workers.value,
                transactions = transactions.value,
                profile = userProfile.value
            )
            result.onSuccess { msg ->
                _syncMessage.value = "CSV Backup saved to Downloads"
                onComplete(true, msg)
            }.onFailure { err ->
                onComplete(false, "Failed to save CSV to device: ${err.message}")
            }
        }
    }

    fun exportBackupCsvFile(context: android.content.Context, onComplete: (Boolean, String, java.io.File?) -> Unit) {
        viewModelScope.launch {
            try {
                val file = CompactCsvBackupService.saveBackupToCsvFile(
                    context = context,
                    workers = workers.value,
                    transactions = transactions.value,
                    profile = userProfile.value
                )
                val totalAtt = workers.value.sumOf { it.attendance.size }
                val sizeKb = String.format(java.util.Locale.US, "%.1f", file.length().toDouble() / 1024.0)
                onComplete(
                    true,
                    "Master backup rewritten to CSV (${file.name}, $sizeKb KB).\nIncludes ${workers.value.size} workers, $totalAtt attendance logs, and ${transactions.value.size} cash entries.",
                    file
                )
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to export CSV backup.", null)
            }
        }
    }


    fun shareWorkerReport(worker: LaborWorker) {
        PdfReportGenerator.shareWorkerReportPdf(getApplication(), worker, selectedMonth.value)
    }

    fun shareCashBookReport(
        startDate: String = "Sat, 01 Aug 26", 
        endDate: String = "Mon, 31 Aug 26",
        customTransactions: List<CashTransaction>? = null
    ) {
        val listToShare = (customTransactions ?: transactions.value).filter { it.amount > 0.0 }
        PdfReportGenerator.shareCashBookReportPdf(getApplication(), listToShare, startDate, endDate)
    }

    fun shareBatchRoster() {
        PdfReportGenerator.shareBatchWorkersReportPdf(getApplication(), workers.value, selectedMonth.value)
    }

    fun resetPassword(context: android.content.Context, email: String) {
        if (email.isBlank() || !email.contains("@")) {
            showMessage("Please enter a valid email to reset your password")
            return
        }
        viewModelScope.launch {
            val result = com.example.data.remote.FirebaseAuthHelper.resetPassword(context, email)
            if (result.isSuccess) {
                showMessage("Password reset link sent to your email")
            } else {
                showMessage("Failed to send reset link: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }
}
