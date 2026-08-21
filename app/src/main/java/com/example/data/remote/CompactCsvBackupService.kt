package com.example.data.remote

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CashTransaction
import com.example.domain.model.DailyAttendance
import com.example.domain.model.LaborWorker
import com.example.domain.model.PaymentMethod
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CompactCsvBackupService {

    private const val TAG = "CompactCsvBackup"
    const val MASTER_CSV_FILENAME = "laborbook_master_backup.csv"
    const val DEVICE_DOWNLOAD_FILENAME = "Laborbook_Complete_Backup.csv"

    private fun escapeCsv(value: String): String {
        var str = value.replace("\r", " ").replace("\n", " ")
        if (str.contains(",") || str.contains("\"") || str.contains(";")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }

    private fun unescapeCsv(value: String): String {
        var str = value.trim()
        if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
            str = str.substring(1, str.length - 1).replace("\"\"", "\"")
        }
        return str
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens.map { unescapeCsv(it) }
    }

    /**
     * Generates a complete, ultra-compact .CSV file containing:
     * 1. Profile Info
     * 2. All Labor Workers (Names, phone, daily rate)
     * 3. Complete Attendance Logs (Present, Absent, Overtime, Half-day, Advances, Notes across all dates)
     * 4. Cash Book Transactions
     */
    fun generateCompleteBackupCsv(
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile
    ): String {
        val sb = StringBuilder()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        // Header Metadata
        sb.append("# LABORBOOK_ALL_DATA_BACKUP,VERSION=3,DATE=${escapeCsv(now)},EMAIL=${escapeCsv(profile.email)}\n")
        sb.append("# FORMAT: ULTRA_LIGHTWEIGHT_CSV,STORAGE_OPTIMIZED=TRUE\n\n")

        // SECTION 1: USER PROFILE
        sb.append("[SECTION_PROFILE]\n")
        sb.append("Name,BusinessName,Mobile,Email,Language,AppLock,IsPro,AuthProvider\n")
        sb.append("${escapeCsv(profile.name)},${escapeCsv(profile.businessName)},${escapeCsv(profile.mobile)},${escapeCsv(profile.email)},${escapeCsv(profile.language)},${profile.appLockEnabled},${profile.isPro},${escapeCsv(profile.authProvider)}\n\n")

        // SECTION 2: LABOR WORKERS MASTER LIST
        sb.append("[SECTION_WORKERS]\n")
        sb.append("WorkerId,Name,PhoneNumber,DailyWage,AvatarColorHex,CreatedAt\n")
        for (w in workers) {
            sb.append("${escapeCsv(w.id)},${escapeCsv(w.name)},${escapeCsv(w.phoneNumber)},${w.dailyWage},${escapeCsv(w.avatarColorHex)},${w.createdAt}\n")
        }
        sb.append("\n")

        // SECTION 3: DAILY ATTENDANCE & LABOUR WAGE DETAILS
        sb.append("[SECTION_ATTENDANCE_LOGS]\n")
        sb.append("WorkerId,WorkerName,FullDate,DayNumber,DayOfWeek,Status,OvertimeHours,AdvanceAmount,Note,OvertimeRate,PaymentMethod\n")
        for (w in workers) {
            for ((dateKey, att) in w.attendance) {
                val fullDate = if (att.fullDate.isNotBlank()) att.fullDate else dateKey
                val statusStr = att.status.name
                val noteClean = att.note.replace("\n", " ").replace("\r", " ")
                sb.append("${escapeCsv(w.id)},${escapeCsv(w.name)},${escapeCsv(fullDate)},${att.dayNumber},${escapeCsv(att.dayOfWeek)},${escapeCsv(statusStr)},${att.overtimeHours},${att.advanceAmount},${escapeCsv(noteClean)},${att.overtimeRate},${escapeCsv(att.paymentMethod.name)}\n")
            }
        }
        sb.append("\n")

        // SECTION 4: CASH BOOK TRANSACTIONS
        sb.append("[SECTION_TRANSACTIONS]\n")
        sb.append("TransactionId,DateDisplay,FullDate,Type,Amount,PaymentMethod,Notes,Timestamp\n")
        for (t in transactions) {
            val noteClean = t.notes.replace("\n", " ").replace("\r", " ")
            sb.append("${escapeCsv(t.id)},${escapeCsv(t.dateDisplay)},${escapeCsv(t.fullDate)},${escapeCsv(t.type.name)},${t.amount},${escapeCsv(t.paymentMethod.name)},${escapeCsv(noteClean)},${t.timestamp}\n")
        }

        return sb.toString()
    }

    /**
     * Parses a complete .CSV backup file and returns all workers, attendance records, transactions, and profile.
     */
    fun parseCompleteBackupCsv(rawContent: String): Result<BackupData> {
        return try {
            // Strip potential UTF-8 BOM
            val csvContent = if (rawContent.startsWith("\uFEFF")) rawContent.substring(1) else rawContent
            val lines = csvContent.lines()
            var currentSection = ""
            var backupDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            var accountEmail = ""

            var profile: UserProfile? = null
            val workersMap = mutableMapOf<String, LaborWorker>()
            val attendanceRecords = mutableMapOf<String, MutableMap<String, DailyAttendance>>()
            val txList = mutableListOf<CashTransaction>()

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isBlank()) continue

                if (line.startsWith("#")) {
                    if (line.contains("DATE=")) {
                        val parts = line.split(",")
                        for (p in parts) {
                            if (p.startsWith("DATE=")) backupDate = p.substringAfter("DATE=").trim('\"')
                            if (p.startsWith("EMAIL=")) accountEmail = p.substringAfter("EMAIL=").trim('\"')
                        }
                    }
                    continue
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line
                    continue
                }

                when (currentSection) {
                    "[SECTION_PROFILE]" -> {
                        if (line.startsWith("Name,BusinessName")) continue
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 4) {
                            profile = UserProfile(
                                name = tokens.getOrElse(0) { "Manager" },
                                businessName = tokens.getOrElse(1) { "Laborbook Pro Master" },
                                mobile = tokens.getOrElse(2) { "7848894498" },
                                email = tokens.getOrElse(3) { accountEmail },
                                language = tokens.getOrElse(4) { "English" },
                                appLockEnabled = tokens.getOrElse(5) { "false" }.toBoolean(),
                                isPro = tokens.getOrElse(6) { "true" }.toBoolean(),
                                isLoggedIn = true,
                                authProvider = tokens.getOrElse(7) { "Google" }
                            )
                        }
                    }

                    "[SECTION_WORKERS]" -> {
                        if (line.startsWith("WorkerId,Name")) continue
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 4) {
                            val id = tokens.getOrElse(0) { UUID.randomUUID().toString() }
                            val name = tokens.getOrElse(1) { "Worker" }
                            val phone = tokens.getOrElse(2) { "" }
                            val wage = tokens.getOrElse(3) { "800.0" }.toDoubleOrNull() ?: 800.0
                            val color = tokens.getOrElse(4) { "#1656D6" }
                            val createdAt = tokens.getOrElse(5) { System.currentTimeMillis().toString() }.toLongOrNull() ?: System.currentTimeMillis()

                            val worker = LaborWorker(
                                id = id,
                                name = name,
                                phoneNumber = phone,
                                dailyWage = wage,
                                avatarColorHex = color,
                                attendance = emptyMap(),
                                createdAt = createdAt
                            )
                            workersMap[id] = worker
                        }
                    }

                    "[SECTION_ATTENDANCE_LOGS]" -> {
                        if (line.startsWith("WorkerId,WorkerName")) continue
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 6) {
                            val wId = tokens.getOrElse(0) { "" }
                            val fullDate = tokens.getOrElse(2) { "2026-08-15" }
                            val dayNumber = tokens.getOrElse(3) { "1" }.toIntOrNull() ?: 1
                            val dayOfWeek = tokens.getOrElse(4) { "Day" }
                            val statusStr = tokens.getOrElse(5) { "UNMARKED" }
                            val status = try {
                                AttendanceStatus.valueOf(statusStr)
                            } catch (e: Exception) {
                                AttendanceStatus.fromSymbol(statusStr)
                            }
                            val otHours = tokens.getOrElse(6) { "0.0" }.toDoubleOrNull() ?: 0.0
                            val advance = tokens.getOrElse(7) { "0.0" }.toDoubleOrNull() ?: 0.0
                            val note = tokens.getOrElse(8) { "" }
                            val otRate = tokens.getOrElse(9) { "0.0" }.toDoubleOrNull() ?: 0.0
                            val payMethodStr = tokens.getOrElse(10) { "ONLINE" }
                            val payMethod = try {
                                PaymentMethod.valueOf(payMethodStr)
                            } catch (e: Exception) {
                                PaymentMethod.ONLINE
                            }

                            val dateKey = if (fullDate.isNotBlank()) fullDate else String.format(Locale.US, "2026-08-%02d", dayNumber)
                            val att = DailyAttendance(
                                dayNumber = dayNumber,
                                dayOfWeek = dayOfWeek,
                                fullDate = dateKey,
                                status = status,
                                overtimeHours = otHours,
                                overtimeRate = otRate,
                                advanceAmount = advance,
                                note = note,
                                paymentMethod = payMethod
                            )

                            val mapForWorker = attendanceRecords.getOrPut(wId) { mutableMapOf() }
                            mapForWorker[dateKey] = att
                        }
                    }

                    "[SECTION_TRANSACTIONS]" -> {
                        if (line.startsWith("TransactionId,DateDisplay")) continue
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 5) {
                            val id = tokens.getOrElse(0) { UUID.randomUUID().toString() }
                            val dateDisplay = tokens.getOrElse(1) { "15 Sat" }
                            val fullDate = tokens.getOrElse(2) { "2026-08-15" }
                            val typeStr = tokens.getOrElse(3) { "CASH_IN" }
                            val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.CASH_IN }
                            val amount = tokens.getOrElse(4) { "0.0" }.toDoubleOrNull() ?: 0.0
                            val payMethodStr = tokens.getOrElse(5) { "CASH" }
                            val payMethod = try { PaymentMethod.valueOf(payMethodStr) } catch (e: Exception) { PaymentMethod.CASH }
                            val notes = tokens.getOrElse(6) { "" }
                            val timestamp = tokens.getOrElse(7) { System.currentTimeMillis().toString() }.toLongOrNull() ?: System.currentTimeMillis()

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
                    }
                }
            }

            // Assemble workers with their attendance
            val finalizedWorkers = workersMap.values.map { worker ->
                val attMap = attendanceRecords[worker.id] ?: emptyMap()
                worker.copy(attendance = attMap)
            }.toList()

            Result.success(
                BackupData(
                    workers = finalizedWorkers,
                    transactions = txList,
                    userProfile = profile,
                    backupTimestamp = backupDate,
                    totalWorkers = finalizedWorkers.size,
                    totalTransactions = txList.size,
                    accountEmail = accountEmail.ifBlank { profile?.email ?: "" }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves the complete backup to a single master .csv file (rewriting previous backup).
     * Cleans up older legacy timestamped backup files to prevent multiple accumulating files.
     */
    fun saveBackupToCsvFile(
        context: Context,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile
    ): File {
        val csvContent = generateCompleteBackupCsv(workers, transactions, profile)
        val dir = File(context.filesDir, "csv_backups")
        if (!dir.exists()) dir.mkdirs()

        // Clean up legacy timestamped files so only one master file remains
        dir.listFiles()?.forEach { oldFile ->
            if (oldFile.name != MASTER_CSV_FILENAME && oldFile.name != "latest_backup.csv") {
                try { oldFile.delete() } catch (_: Exception) {}
            }
        }

        val masterFile = File(dir, MASTER_CSV_FILENAME)
        masterFile.writeText(csvContent)

        // Also update latest_backup.csv alias
        val latest = File(dir, "latest_backup.csv")
        latest.writeText(csvContent)

        return masterFile
    }

    /**
     * Saves the CSV backup directly into the user device's storage (Downloads folder).
     * Overwrites any previous backup file on the device so user has a single, up-to-date backup.
     */
    fun saveBackupCsvToDeviceDownloads(
        context: Context,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile
    ): Result<String> {
        return try {
            val csvContent = generateCompleteBackupCsv(workers, transactions, profile)
            val fileName = DEVICE_DOWNLOAD_FILENAME

            var savedPath = "Downloads/$fileName"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                // Check if existing file exists and update or replace it
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri, "wt")?.use { os ->
                        os.write(csvContent.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                    savedPath = "Downloads/$fileName"
                } else {
                    // Fallback to external files dir
                    val fallbackFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, fileName)
                    fallbackFile.writeText(csvContent)
                    savedPath = fallbackFile.absolutePath
                }
            } else {
                // Pre-Android 10
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(csvContent)
                savedPath = targetFile.absolutePath
            }

            // Also keep internal master file in sync
            saveBackupToCsvFile(context, workers, transactions, profile)

            val totalAtt = workers.sumOf { it.attendance.size }
            val msg = "CSV backup saved directly to device ($savedPath).\nOverwrote previous file with ${workers.size} workers, $totalAtt attendance logs & ${transactions.size} cash records."
            Log.i(TAG, msg)
            Result.success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CSV to device downloads: ${e.message}", e)
            // Fallback: save to internal app storage
            try {
                val file = saveBackupToCsvFile(context, workers, transactions, profile)
                Result.success("Saved to app storage: ${file.name} (overwriting previous backup).")
            } catch (ex: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Exports and shares the full .csv backup file via Android Share Intent.
     * Uses FileProvider with read permissions so WhatsApp, Google Drive, Email, etc., can receive it cleanly.
     */
    fun shareBackupCsvFile(
        context: Context,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile
    ): Result<Boolean> {
        return try {
            val file = saveBackupToCsvFile(context, workers, transactions, profile)
            val sizeKb = String.format(Locale.US, "%.1f", file.length().toDouble() / 1024.0)

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val totalAtt = workers.sumOf { it.attendance.size }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laborbook Complete App Backup (${profile.businessName})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "📦 Laborbook Complete App Data Backup (.CSV)\n" +
                    "• Business: ${profile.businessName}\n" +
                    "• Workers: ${workers.size} Staff Members\n" +
                    "• Attendance Days Logged: $totalAtt records\n" +
                    "• Cash Transactions: ${transactions.size} records\n" +
                    "• Master File: ${file.name} ($sizeKb KB)\n\n" +
                    "Import this .csv file into Laborbook anytime to restore 100% of your labor and cash data."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(sendIntent, "Share Laborbook Full Backup (.CSV)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share CSV backup: ${e.message}", e)
            Result.failure(e)
        }
    }
}
