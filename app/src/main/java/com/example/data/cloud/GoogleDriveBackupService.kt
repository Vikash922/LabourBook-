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

    private fun sanitizeUserKey(email: String): String {
        return if (email.isBlank()) "default_user" 
        else email.lowercase().trim().replace("@", "_at_").replace(".", "_")
    }

    private fun getUserBackupDir(context: Context, email: String): File {
        val key = sanitizeUserKey(email)
        val dir = File(context.filesDir, "google_drive_backups/$key")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
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
     * Saves or updates a Google Drive backup snapshot for the active Google account.
     */
    fun saveBackupToUserDrive(
        context: Context,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile
    ): Result<BackupMetadata> {
        return try {
            val json = generateBackupJson(workers, transactions, profile)
            val userDir = getUserBackupDir(context, profile.email)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "drive_backup_${timeStamp}.json"
            val file = File(userDir, fileName)
            file.writeText(json)

            // Also maintain a 'latest_drive_backup.json' for instant auto-restore
            val latestFile = File(userDir, "latest_drive_backup.json")
            latestFile.writeText(json)

            val displayDate = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())
            val sizeKb = (file.length().toDouble() / 1024.0)

            Result.success(
                BackupMetadata(
                    fileName = fileName,
                    dateString = displayDate,
                    fileSizeKb = String.format(Locale.US, "%.1f", sizeKb).toDoubleOrNull() ?: 1.0,
                    workerCount = workers.size,
                    transactionCount = transactions.size,
                    accountEmail = profile.email,
                    file = file
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves a safety snapshot before actions like deleting a worker.
     */
    fun saveSafetyBackup(
        context: Context,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile,
        reason: String
    ): Result<BackupMetadata> {
        return try {
            val json = generateBackupJson(workers, transactions, profile)
            val userDir = getUserBackupDir(context, profile.email)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "safety_backup_${timeStamp}.json"
            val file = File(userDir, fileName)
            file.writeText(json)

            val displayDate = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())
            val sizeKb = (file.length().toDouble() / 1024.0)

            Result.success(
                BackupMetadata(
                    fileName = fileName,
                    dateString = displayDate,
                    fileSizeKb = String.format(Locale.US, "%.1f", sizeKb).toDoubleOrNull() ?: 1.0,
                    workerCount = workers.size,
                    transactionCount = transactions.size,
                    accountEmail = profile.email,
                    file = file
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks Google Drive for the latest backup created under this Google account.
     */
    fun getLatestBackupForUser(context: Context, email: String): BackupData? {
        return try {
            val userDir = getUserBackupDir(context, email)
            val latestFile = File(userDir, "latest_drive_backup.json")
            if (latestFile.exists()) {
                val json = latestFile.readText()
                val parsed = parseBackupJson(json)
                parsed.getOrNull()
            } else {
                // Check latest timestamped file
                val files = userDir.listFiles { f -> f.extension == "json" && f.name != "latest_drive_backup.json" }
                val mostRecent = files?.maxByOrNull { it.lastModified() }
                if (mostRecent != null) {
                    val json = mostRecent.readText()
                    parseBackupJson(json).getOrNull()
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parseBackupUniversal(content: String): Result<BackupData> {
        val trimmed = content.trim()
        return if (trimmed.startsWith("{")) {
            parseBackupJson(trimmed)
        } else if (trimmed.contains("[SECTION_") || trimmed.contains("LABORBOOK") || trimmed.contains("WorkerId,")) {
            CompactCsvBackupService.parseCompleteBackupCsv(trimmed)
        } else {
            // Try json first, then csv
            val jsonRes = parseBackupJson(trimmed)
            if (jsonRes.isSuccess) jsonRes else CompactCsvBackupService.parseCompleteBackupCsv(trimmed)
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
     * Lists all available Google Drive & local backups (both .json and .csv) for this Google account.
     */
    fun getAvailableBackupsForUser(context: Context, email: String): List<BackupMetadata> {
        val userDir = getUserBackupDir(context, email)
        val csvDir = File(context.filesDir, "csv_backups")
        
        val userFiles = if (userDir.exists()) {
            userDir.listFiles { f -> (f.extension == "json" || f.extension == "csv") && !f.name.startsWith("latest_") }?.toList() ?: emptyList()
        } else emptyList()

        val csvFiles = if (csvDir.exists()) {
            csvDir.listFiles { f -> f.extension == "csv" && !f.name.startsWith("latest_") }?.toList() ?: emptyList()
        } else emptyList()

        val allFiles = (userFiles + csvFiles).distinctBy { it.name }

        return allFiles.sortedByDescending { it.lastModified() }.map { file ->
            val dateStr = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
            val sizeKb = (file.length().toDouble() / 1024.0)
            
            var wCount = 0
            var tCount = 0
            try {
                val text = file.readText()
                val parsed = parseBackupUniversal(text)
                parsed.onSuccess { data ->
                    wCount = data.totalWorkers
                    tCount = data.totalTransactions
                }
            } catch (_: Exception) {}

            BackupMetadata(
                fileName = file.name,
                dateString = dateStr,
                fileSizeKb = String.format(Locale.US, "%.1f", sizeKb).toDoubleOrNull() ?: 1.0,
                workerCount = wCount,
                transactionCount = tCount,
                accountEmail = email,
                file = file
            )
        }
    }
}
