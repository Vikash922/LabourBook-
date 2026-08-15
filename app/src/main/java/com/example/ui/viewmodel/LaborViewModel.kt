package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.BackupMetadata
import com.example.data.cloud.CloudSyncService
import com.example.data.cloud.CompactCsvBackupService
import com.example.data.cloud.GoogleDriveBackupService
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashTransaction
import com.example.data.model.LaborWorker
import com.example.data.model.PaymentMethod
import com.example.data.model.SavedContact
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.repository.LaborRepository
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
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

    fun updateSelectedMonth(month: String) {
        repository.updateSelectedMonth(month)
    }

    fun refreshContacts(context: android.content.Context) {
        repository.loadDeviceContacts(context)
    }

    // Current navigation destination & Bottom Tab index (0 = Labor, 1 = Cash book, 2 = Settings)
    private val _currentScreen = MutableStateFlow<Screen>(
        if (repository.userProfile.value.isLoggedIn) Screen.LaborHome else Screen.Login
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

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

    private val _newLaborSkills = MutableStateFlow(listOf("Tile worker", "Carpenter"))
    val newLaborSkills: StateFlow<List<String>> = _newLaborSkills.asStateFlow()

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
        if (query.isBlank()) list
        else list.filter { it.notes.contains(query, ignoreCase = true) || it.dateDisplay.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        when (screen) {
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

    fun toggleSkillSelection(skill: String) {
        val current = _newLaborSkills.value.toMutableList()
        if (current.contains(skill)) {
            current.remove(skill)
        } else {
            current.add(skill)
        }
        _newLaborSkills.value = current
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
        repository.addWorker(name, phone, wage, _newLaborSkills.value)

        // Reset form
        _newLaborName.value = ""
        _newLaborPhone.value = ""
        _isAddStaffExpanded.value = false
        navigateTo(Screen.LaborHome)
        return true
    }

    fun addLaborFromContact(contact: SavedContact) {
        repository.addWorker(
            name = contact.name,
            phone = contact.phoneNumber,
            wage = 800.0,
            skills = listOf("Tile worker", "Carpenter", "General staff")
        )
        navigateTo(Screen.LaborHome)
    }

    fun setAttendance(workerId: String, dayNumber: Int, status: AttendanceStatus, monthStr: String = selectedMonth.value) {
        repository.setAttendanceStatus(workerId, monthStr, dayNumber, status)
    }

    fun updateDayDetails(workerId: String, dayNumber: Int, advance: Double, note: String, otHours: Double, monthStr: String = selectedMonth.value) {
        repository.updateDayDetails(workerId, monthStr, dayNumber, advance, note, otHours)
    }

    fun updateWorker(workerId: String, name: String, phone: String, dailyWage: Double) {
        repository.updateWorker(workerId, name, phone, dailyWage)
    }

    fun deleteWorker(workerId: String) {
        repository.deleteWorker(workerId)
        navigateTo(Screen.LaborHome)
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
        _activeTransaction.value = CashTransaction(
            id = "",
            dateDisplay = "15 Sat",
            fullDate = "2026-08-15",
            type = type,
            amount = 0.0,
            paymentMethod = PaymentMethod.CASH,
            notes = if (type == TransactionType.CASH_IN) "Income" else "Material expense"
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
        dateDisplay: String = "15 Sat",
        fullDate: String = "2026-08-15"
    ) {
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

    fun toggleAppLock() {
        repository.toggleAppLock()
    }

    fun setLanguage(lang: String) {
        repository.setLanguage(lang)
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            val result = CloudSyncService.syncDataToCloud(workers.value.size, transactions.value.size)
            _syncMessage.value = result.getOrNull() ?: "Cloud synchronization complete."
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Google Authentication Only
    fun loginWithGoogle(name: String = "Google User", email: String = "jyoti3322114455@gmail.com") {
        repository.loginWithGoogleAccount(name = name, email = email)
        _currentScreen.value = Screen.LaborHome
        _selectedTabIndex.value = 0
    }

    /**
     * Automatic Backup on Logout:
     * Saves/updates the latest Google Drive backup for this specific account, then logs out.
     */
    fun logoutWithDriveBackup(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoggingOut.value = true
            val result = repository.backupAndLogout()
            _isLoggingOut.value = false
            _currentScreen.value = Screen.Login
            _selectedTabIndex.value = 0
            onComplete(true, result.getOrNull() ?: "Backed up to Google Drive & Logged out.")
        }
    }

    // Google Drive Backup & Restore Methods
    fun backupToGoogleDrive(context: android.content.Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = GoogleDriveBackupService.saveBackupToUserDrive(
                context = context,
                workers = workers.value,
                transactions = transactions.value,
                profile = userProfile.value
            )
            result.onSuccess { meta ->
                repository.updateDriveBackupInfo(meta.dateString, meta.fileName)
                _syncMessage.value = "Google Drive backup successful (${meta.workerCount} workers, ${meta.transactionCount} transactions)."
                onComplete(true, "Backup saved to Google Drive successfully for ${meta.accountEmail}!\nFile: ${meta.fileName} (${meta.fileSizeKb} KB)")
            }.onFailure { err ->
                onComplete(false, err.message ?: "Failed to create Google Drive backup.")
            }
        }
    }

    fun getGoogleDriveBackups(context: android.content.Context): List<BackupMetadata> {
        return GoogleDriveBackupService.getAvailableBackupsForUser(context, userProfile.value.email)
    }

    fun restoreFromGoogleDriveUri(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = GoogleDriveBackupService.readBackupFromUri(context, uri)
            result.onSuccess { backupData ->
                repository.restoreData(backupData)
                repository.updateDriveBackupInfo(backupData.backupTimestamp, "Imported from Drive")
                _syncMessage.value = "Data imported from Google Drive (${backupData.totalWorkers} workers, ${backupData.totalTransactions} transactions)."
                onComplete(true, "Successfully restored ${backupData.totalWorkers} workers and ${backupData.totalTransactions} transactions from Google Drive backup!")
            }.onFailure { err ->
                onComplete(false, err.message ?: "Failed to parse Google Drive backup file.")
            }
        }
    }

    fun exportAndShareBackupCsv(context: android.content.Context) {
        CompactCsvBackupService.shareBackupCsvFile(
            context = context,
            workers = workers.value,
            transactions = transactions.value,
            profile = userProfile.value
        )
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
                    "Complete backup exported to CSV (${file.name}, $sizeKb KB).\nIncludes ${workers.value.size} workers, $totalAtt attendance logs, and ${transactions.value.size} cash entries.",
                    file
                )
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to export CSV backup.", null)
            }
        }
    }

    fun restoreLatestAvailableBackup(context: android.content.Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val backups = getGoogleDriveBackups(context)
                if (backups.isEmpty()) {
                    onComplete(false, "No previous Google Drive or local backups found.")
                    return@launch
                }
                // Find latest backup that contains data or the newest snapshot
                val target = backups.firstOrNull { it.workerCount > 0 && it.file != null } ?: backups.firstOrNull { it.file != null }
                if (target == null || target.file == null) {
                    onComplete(false, "No valid backup file found.")
                    return@launch
                }

                val content = target.file.readText()
                val result = GoogleDriveBackupService.parseBackupUniversal(content)
                result.onSuccess { backupData ->
                    repository.restoreData(backupData)
                    repository.updateDriveBackupInfo(backupData.backupTimestamp, target.fileName)
                    _syncMessage.value = "Restored ${backupData.totalWorkers} workers from ${target.dateString}"
                    onComplete(true, "Successfully restored ${backupData.totalWorkers} workers and ${backupData.totalTransactions} transactions from ${target.dateString}!")
                }.onFailure { err ->
                    onComplete(false, err.message ?: "Failed to read backup.")
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to restore latest backup.")
            }
        }
    }

    fun restoreFromLocalBackup(backupFile: java.io.File, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val content = backupFile.readText()
                val result = GoogleDriveBackupService.parseBackupUniversal(content)
                result.onSuccess { backupData ->
                    repository.restoreData(backupData)
                    repository.updateDriveBackupInfo(backupData.backupTimestamp, backupFile.name)
                    _syncMessage.value = "Data restored (${backupData.totalWorkers} workers, ${backupData.totalTransactions} transactions)."
                    onComplete(true, "Restored ${backupData.totalWorkers} workers, all attendance logs, and ${backupData.totalTransactions} transactions from ${backupFile.name}.")
                }.onFailure { err ->
                    onComplete(false, err.message ?: "Failed to read backup file.")
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Error reading backup file.")
            }
        }
    }

    fun shareWorkerReport(worker: LaborWorker) {
        val report = PdfReportGenerator.generateWorkerReportText(worker, selectedMonth.value)
        PdfReportGenerator.shareToWhatsAppOrSystem(getApplication(), report, "Share ${worker.name}'s Attendance Slip")
    }

    fun shareCashBookReport(startDate: String = "Sat, 01 Aug 26", endDate: String = "Mon, 31 Aug 26") {
        val report = PdfReportGenerator.generateCashBookReportText(transactions.value, startDate, endDate)
        PdfReportGenerator.shareToWhatsAppOrSystem(getApplication(), report, "Share Cash Book Statement")
    }

    fun shareBatchRoster() {
        val report = PdfReportGenerator.generateBatchWorkersReportText(workers.value, selectedMonth.value)
        PdfReportGenerator.shareToWhatsAppOrSystem(getApplication(), report, "Share Consolidated Wage Roster")
    }
}
