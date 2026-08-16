package com.example.data.cloud

import android.content.Context
import android.net.Uri
import com.example.data.model.AttendanceStatus
import com.example.data.model.CashTransaction
import com.example.data.model.DailyAttendance
import com.example.data.model.LaborWorker
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val workers: List<LaborWorker>,
    val transactions: List<CashTransaction>,
    val userProfile: UserProfile?,
    val backupTimestamp: String,
    val totalWorkers: Int,
    val totalTransactions: Int,
    val accountEmail: String = ""
)

data class BackupMetadata(
    val fileName: String,
    val dateString: String,
    val fileSizeKb: Double,
    val workerCount: Int,
    val transactionCount: Int,
    val accountEmail: String = "",
    val file: File? = null
)

object GoogleDriveBackupService {

    const val MASTER_BACKUP_FILENAME = "drive_backup_master.json"

    private fun sanitizeUserKey(email: String): String {
        return if (email.isBlank()) "default_user" 
        else email.lowercase().trim().replace("@", "_at_").replace(".", "_")
    }

    fun getUserBackupDir(context: Context, email: String): File {
        val key = sanitizeUserKey(email)
        val dir = File(context.filesDir, "google_drive_backups/$key")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun cleanExtraBackupFiles(context: Context, email: String) {
        try {
            val userDir = getUserBackupDir(context, email)
            userDir.listFiles()?.forEach { file ->
                if (file.name != MASTER_BACKUP_FILENAME) {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun generateBackupJson(
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile
    ): String {
        val root = JSONObject()
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        root.put("appName", "Laborbook")
        root.put("version", 3)
        root.put("backupDate", nowFormatted)
        root.put("accountEmail", profile.email)

        // User Profile
        val profileObj = JSONObject().apply {
            put("name", profile.name)
            put("businessName", profile.businessName)
            put("mobile", profile.mobile)
            put("email", profile.email)
            put("language", profile.language)
            put("isPro", profile.isPro)
            put("authProvider", "Google")
        }
        root.put("userProfile", profileObj)

        // Workers Array
        val workersArray = JSONArray()
        for (w in workers) {
            val wObj = JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("phoneNumber", w.phoneNumber)
                put("dailyWage", w.dailyWage)
                put("avatarColorHex", w.avatarColorHex)
                put("createdAt", w.createdAt)

                // Skills
                val skillsArr = JSONArray()
                w.skills.forEach { skillsArr.put(it) }
                put("skills", skillsArr)

                // Attendance Map (keyed by date string "yyyy-MM-dd")
                val attObj = JSONObject()
                for ((dateKey, att) in w.attendance) {
                    val dObj = JSONObject().apply {
                        put("dayNumber", att.dayNumber)
                        put("dayOfWeek", att.dayOfWeek)
                        put("fullDate", att.fullDate.ifBlank { dateKey })
                        put("status", att.status.name)
                        put("overtimeHours", att.overtimeHours)
                        put("advanceAmount", att.advanceAmount)
                        put("note", att.note)
                    }
                    attObj.put(dateKey, dObj)
                }
                put("attendance", attObj)
            }
            workersArray.put(wObj)
        }
        root.put("workers", workersArray)

        // Transactions Array
        val txArray = JSONArray()
        for (t in transactions) {
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("dateDisplay", t.dateDisplay)
                put("fullDate", t.fullDate)
                put("type", t.type.name)
                put("amount", t.amount)
                put("paymentMethod", t.paymentMethod.name)
                put("notes", t.notes)
                put("timestamp", t.timestamp)
            }
            txArray.put(tObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)
            val backupDate = root.optString("backupDate", "Unknown")
            val accountEmail = root.optString("accountEmail", "")

            // Profile
            var profile: UserProfile? = null
            if (root.has("userProfile")) {
                val pObj = root.getJSONObject("userProfile")
                profile = UserProfile(
                    name = pObj.optString("name", "Manager"),
                    businessName = pObj.optString("businessName", "Laborbook Pro Master"),
                    mobile = pObj.optString("mobile", "7848894498"),
                    email = pObj.optString("email", accountEmail),
                    language = pObj.optString("language", "English"),
                    isPro = pObj.optBoolean("isPro", true),
                    isLoggedIn = true,
                    authProvider = "Google"
                )
            }

            // Workers
            val workersList = mutableListOf<LaborWorker>()
            val workersArray = root.optJSONArray("workers") ?: JSONArray()
            for (i in 0 until workersArray.length()) {
                val wObj = workersArray.getJSONObject(i)
                val id = wObj.optString("id", java.util.UUID.randomUUID().toString())
                val name = wObj.optString("name", "Worker")
                val phone = wObj.optString("phoneNumber", "")
                val wage = wObj.optDouble("dailyWage", 800.0)
                val avatarColor = wObj.optString("avatarColorHex", "#1656D6")
                val createdAt = wObj.optLong("createdAt", System.currentTimeMillis())

                val skillsList = mutableListOf<String>()
                val sArr = wObj.optJSONArray("skills")
                if (sArr != null) {
                    for (s in 0 until sArr.length()) {
                        skillsList.add(sArr.getString(s))
                    }
                }

                val attMap = mutableMapOf<String, DailyAttendance>()
                val attObj = wObj.optJSONObject("attendance")
                if (attObj != null) {
                    val keys = attObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val dObj = attObj.getJSONObject(key)
                        val rawFullDate = dObj.optString("fullDate", "")
                        val dateKey = if (key.contains("-")) key
                                      else if (rawFullDate.contains("-")) rawFullDate
                                      else {
                                          val dInt = key.toIntOrNull() ?: 1
                                          String.format(Locale.US, "2026-08-%02d", dInt)
                                      }
                        val dayInt = dObj.optInt("dayNumber", dateKey.substringAfterLast("-").toIntOrNull() ?: 1)
                        val dow = dObj.optString("dayOfWeek", "Day")
                        val statusStr = dObj.optString("status", AttendanceStatus.UNMARKED.name)
                        val status = try {
                            AttendanceStatus.valueOf(statusStr)
                        } catch (e: Exception) {
                            AttendanceStatus.fromSymbol(statusStr)
                        }

                        attMap[dateKey] = DailyAttendance(
                            dayNumber = dayInt,
                            dayOfWeek = dow,
                            fullDate = dateKey,
                            status = status,
                            overtimeHours = dObj.optDouble("overtimeHours", 0.0),
                            advanceAmount = dObj.optDouble("advanceAmount", 0.0),
                            note = dObj.optString("note", "")
                        )
                    }
                }

                workersList.add(
                    LaborWorker(
                        id = id,
                        name = name,
                        phoneNumber = phone,
                        dailyWage = wage,
                        skills = if (skillsList.isEmpty()) listOf("Staff", "Labor") else skillsList,
                        avatarColorHex = avatarColor,
                        attendance = attMap,
                        createdAt = createdAt
                    )
                )
            }

            // Transactions
            val txList = mutableListOf<CashTransaction>()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArray.length()) {
                val tObj = txArray.getJSONObject(i)
                val id = tObj.optString("id", java.util.UUID.randomUUID().toString())
                val dateDisplay = tObj.optString("dateDisplay", "15 Sat")
                val fullDate = tObj.optString("fullDate", "2026-08-15")
                val typeStr = tObj.optString("type", TransactionType.CASH_IN.name)
                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.CASH_IN }
                val amount = tObj.optDouble("amount", 0.0)
                val payMethodStr = tObj.optString("paymentMethod", PaymentMethod.CASH.name)
                val payMethod = try { PaymentMethod.valueOf(payMethodStr) } catch (e: Exception) { PaymentMethod.CASH }
                val notes = tObj.optString("notes", "")
                val timestamp = tObj.optLong("timestamp", System.currentTimeMillis())

                txList.add(
                    CashTransaction(
                        id = id,
                        dateDisplay = dateDisplay,
                        fullDate = fullDate,
                        type = type,
                        amount = amount,
                        paymentMethod = payMethod,
                        notes = notes,
                        timestamp = timestamp
                    )
                )
            }

            Result.success(
                BackupData(
                    workers = workersList,
                    transactions = txList,
                    userProfile = profile,
                    backupTimestamp = backupDate,
                    totalWorkers = workersList.size,
                    totalTransactions = txList.size,
                    accountEmail = accountEmail
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves or overwrites the single Google Drive master backup file for the active Google account.
     * Overwrites this same single file in place so no extra backup files ever accumulate in Google Drive / storage.
     */
    fun saveBackupToUserDrive(
        context: Context,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile,
        isManual: Boolean = false
    ): Result<BackupMetadata> {
        return try {
            val userDir = getUserBackupDir(context, profile.email)
            val masterFile = File(userDir, MASTER_BACKUP_FILENAME)

            // If auto-sync and workers are empty, preserve existing master backup that has workers
            if (!isManual && workers.isEmpty() && masterFile.exists()) {
                val existing = parseBackupJson(masterFile.readText()).getOrNull()
                if (existing != null && existing.totalWorkers > 0) {
                    val displayDate = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(masterFile.lastModified()))
                    val sizeKb = (masterFile.length().toDouble() / 1024.0)
                    return Result.success(
                        BackupMetadata(
                            fileName = "Google Drive Master Backup (.JSON)",
                            dateString = displayDate,
                            fileSizeKb = String.format(Locale.US, "%.1f", sizeKb).toDoubleOrNull() ?: 1.0,
                            workerCount = existing.totalWorkers,
                            transactionCount = existing.totalTransactions,
                            accountEmail = profile.email,
                            file = masterFile
                        )
                    )
                }
            }

            val json = generateBackupJson(workers, transactions, profile)

            // Overwrite single master file in place
            masterFile.writeText(json)

            // Purge any extraneous legacy snapshot files to keep strictly 1 file in storage
            cleanExtraBackupFiles(context, profile.email)

            val displayDate = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())
            val sizeKb = (masterFile.length().toDouble() / 1024.0)

            Result.success(
                BackupMetadata(
                    fileName = "Google Drive Master Backup (.JSON)",
                    dateString = displayDate,
                    fileSizeKb = String.format(Locale.US, "%.1f", sizeKb).toDoubleOrNull() ?: 1.0,
                    workerCount = workers.size,
                    transactionCount = transactions.size,
                    accountEmail = profile.email,
                    file = masterFile
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks Google Drive / local storage for the master backup file.
     */
    fun getLatestBackupForUser(context: Context, email: String): BackupData? {
        return try {
            val userDir = getUserBackupDir(context, email)
            val masterFile = File(userDir, MASTER_BACKUP_FILENAME)

            if (masterFile.exists()) {
                parseBackupJson(masterFile.readText()).getOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parseBackupUniversal(content: String): Result<BackupData> {
        val trimmed = content.trim()
        val cleanContent = if (trimmed.startsWith("\uFEFF")) trimmed.substring(1) else trimmed
        return if (cleanContent.startsWith("{")) {
            parseBackupJson(cleanContent)
        } else if (cleanContent.contains("[SECTION_") || cleanContent.contains("LABORBOOK") || cleanContent.contains("WorkerId,")) {
            CompactCsvBackupService.parseCompleteBackupCsv(cleanContent)
        } else {
            val jsonRes = parseBackupJson(cleanContent)
            if (jsonRes.isSuccess) jsonRes else CompactCsvBackupService.parseCompleteBackupCsv(cleanContent)
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): Result<BackupData> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            if (content.isBlank()) {
                return Result.failure(Exception("File is empty or could not be read."))
            }
            parseBackupUniversal(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists the single master Google Drive backup and CSV backup for this account.
     * Exactly 1 single master file is maintained to save Drive and device storage.
     */
    fun getAvailableBackupsForUser(context: Context, email: String): List<BackupMetadata> {
        val userDir = getUserBackupDir(context, email)
        val csvDir = File(context.filesDir, "csv_backups")

        // Clean up any extra/stale files so only 1 master file remains
        cleanExtraBackupFiles(context, email)

        val resultList = mutableListOf<BackupMetadata>()

        // 1. Single Master Google Drive JSON Backup
        val masterJsonFile = File(userDir, MASTER_BACKUP_FILENAME)
        if (masterJsonFile.exists()) {
            val dateStr = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(masterJsonFile.lastModified()))
            val sizeKb = (masterJsonFile.length().toDouble() / 1024.0)
            var wCount = 0
            var tCount = 0
            try {
                val parsed = parseBackupJson(masterJsonFile.readText())
                parsed.onSuccess { data ->
                    wCount = data.totalWorkers
                    tCount = data.totalTransactions
                }
            } catch (_: Exception) {}

            resultList.add(
                BackupMetadata(
                    fileName = "Google Drive Master Backup (.JSON)",
                    dateString = dateStr,
                    fileSizeKb = String.format(Locale.US, "%.1f", sizeKb).toDoubleOrNull() ?: 1.0,
                    workerCount = wCount,
                    transactionCount = tCount,
                    accountEmail = email,
                    file = masterJsonFile
                )
            )
        }

        // 2. Master CSV Backup (if present)
        val masterCsvFile = File(csvDir, CompactCsvBackupService.MASTER_CSV_FILENAME)
        if (masterCsvFile.exists()) {
            val dateStr = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(masterCsvFile.lastModified()))
            val sizeKb = (masterCsvFile.length().toDouble() / 1024.0)
            var wCount = 0
            var tCount = 0
            try {
                val parsed = CompactCsvBackupService.parseCompleteBackupCsv(masterCsvFile.readText())
                parsed.onSuccess { data ->
                    wCount = data.totalWorkers
                    tCount = data.totalTransactions
                }
            } catch (_: Exception) {}

            resultList.add(
                BackupMetadata(
                    fileName = "Device Master Backup (.CSV)",
                    dateString = dateStr,
                    fileSizeKb = String.format(Locale.US, "%.1f", sizeKb).toDoubleOrNull() ?: 1.0,
                    workerCount = wCount,
                    transactionCount = tCount,
                    accountEmail = email,
                    file = masterCsvFile
                )
            )
        }

        return resultList
    }
}
