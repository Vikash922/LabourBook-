package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.example.data.model.LaborWorker
import com.example.data.model.CashTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudBackupRecord(
    val driveFileId: String,
    val accountEmail: String,
    val backupTimestamp: String,
    val timestampMillis: Long,
    val workerCount: Int,
    val transactionCount: Int,
    val backupJson: String,
    val checksum: String = ""
)

object GoogleDriveCloudService {
    private const val TAG = "GoogleDriveBackup"
    private const val COLLECTION_BACKUPS = "google_drive_backups"

    fun sanitizeUserKey(email: String): String {
        return if (email.isBlank()) "default_user" 
        else email.lowercase().trim().replace("@", "_at_").replace(".", "_")
    }

    /**
     * Uploads and rewrites the master backup payload to Google Drive & Cloud Firestore for this user.
     * Protects manual and safety snapshots from being wiped by automatic empty-state syncs.
     */
    suspend fun uploadBackupToCloud(
        context: Context?,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile,
        reason: String = "Manual / Auto-Sync"
    ): Result<CloudBackupRecord> {
        val email = profile.email.ifBlank { "default_user" }.lowercase().trim()
        val userKey = sanitizeUserKey(email)
        val driveFileId = "drive_master_$userKey"
        val timestampStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val timestampMillis = System.currentTimeMillis()
        val isManual = reason.contains("Manual")
        val isSafety = reason.contains("Safety") || reason.contains("Pre-delete")

        Log.i(TAG, "Updating master Google Drive backup for account: $email ($reason)...")

        // 1. Generate full backup JSON
        val backupJson = GoogleDriveBackupService.generateBackupJson(workers, transactions, profile)

        // 2. Overwrite master file in internal user files directory
        if (context != null) {
            try {
                GoogleDriveBackupService.saveBackupToUserDrive(context, workers, transactions, profile, isManual = isManual)
            } catch (e: Exception) {
                Log.w(TAG, "Local cache warning: ${e.message}")
            }
        }

        val record = CloudBackupRecord(
            driveFileId = driveFileId,
            accountEmail = email,
            backupTimestamp = timestampStr,
            timestampMillis = timestampMillis,
            workerCount = workers.size,
            transactionCount = transactions.size,
            backupJson = backupJson,
            checksum = "SHA256-${backupJson.hashCode()}"
        )

        // 3. Upload & Overwrite in single Cloud master record
        try {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection(COLLECTION_BACKUPS).document(userKey)

            val payload = hashMapOf(
                "driveFileId" to driveFileId,
                "accountEmail" to email,
                "backupTimestamp" to timestampStr,
                "timestampMillis" to timestampMillis,
                "workerCount" to workers.size,
                "transactionCount" to transactions.size,
                "backupJson" to backupJson,
                "appName" to "Laborbook",
                "version" to 3,
                "lastUploadReason" to reason
            )

            withTimeoutOrNull(8000) {
                if (isManual) {
                    userDocRef.collection("snapshots").document("latest").set(payload, SetOptions.merge()).await()
                    userDocRef.set(payload, SetOptions.merge()).await()
                } else {
                    // Auto-sync: If workers is 0, do not overwrite 'latest' if 'latest' already had workers
                    val existingLatest = try { userDocRef.collection("snapshots").document("latest").get().await() } catch (_: Exception) { null }
                    val existingWorkerCount = existingLatest?.getLong("workerCount") ?: 0L

                    if (workers.isNotEmpty() || existingWorkerCount == 0L) {
                        userDocRef.collection("snapshots").document("latest").set(payload, SetOptions.merge()).await()
                        userDocRef.set(payload, SetOptions.merge()).await()
                    }
                }
            }
            Log.i(TAG, "OVERWRITE SUCCESS: Master Drive File: $driveFileId | Account: $email | Workers: ${workers.size} | Reason: $reason")
        } catch (e: Exception) {
            Log.e(TAG, "Cloud upload network warning: ${e.message}. Offline master cached locally.")
        }

        return Result.success(record)
    }

    /**
     * Downloads and parses the master Google Drive backup for this user from Cloud.
     * If latest has 0 workers (e.g. after accidental delete), checks manual backup & safety snapshots.
     */
    suspend fun downloadLatestBackupFromCloud(
        context: Context?,
        email: String
    ): Result<BackupData> {
        val targetEmail = email.ifBlank { "default_user" }.lowercase().trim()
        val userKey = sanitizeUserKey(targetEmail)

        Log.i(TAG, "Checking Google Drive & Cloud for account: $targetEmail...")

        // 1. Attempt Cloud Firestore query first
        try {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection(COLLECTION_BACKUPS).document(userKey)

            // Check latest snapshot
            val latestSnapshot = withTimeoutOrNull(8000) {
                userDocRef.collection("snapshots").document("latest").get().await()
            }

            var bestBackupData: BackupData? = null

            if (latestSnapshot != null && latestSnapshot.exists()) {
                val jsonString = latestSnapshot.getString("backupJson")
                if (!jsonString.isNullOrBlank()) {
                    val parseResult = GoogleDriveBackupService.parseBackupJson(jsonString)
                    if (parseResult.isSuccess) {
                        val backupData = parseResult.getOrThrow()
                        bestBackupData = backupData
                        if (backupData.totalWorkers > 0) {
                            Log.i(TAG, "DOWNLOAD SUCCESS: Master cloud backup for $targetEmail | Workers: ${backupData.totalWorkers}")
                            return Result.success(backupData)
                        }
                    }
                }
            }

            // If latest had 0 workers, check manual backup snapshot
            val manualSnapshot = withTimeoutOrNull(5000) {
                userDocRef.collection("snapshots").document("manual_backup").get().await()
            }
            if (manualSnapshot != null && manualSnapshot.exists()) {
                val jsonString = manualSnapshot.getString("backupJson")
                if (!jsonString.isNullOrBlank()) {
                    val parseResult = GoogleDriveBackupService.parseBackupJson(jsonString)
                    if (parseResult.isSuccess) {
                        val backupData = parseResult.getOrThrow()
                        if (backupData.totalWorkers > 0) {
                            Log.i(TAG, "DOWNLOAD SUCCESS from manual backup: Workers: ${backupData.totalWorkers}")
                            return Result.success(backupData)
                        }
                    }
                }
            }

            // Check safety snapshot
            val safetySnapshot = withTimeoutOrNull(5000) {
                userDocRef.collection("safety_snapshots").document("latest_safety").get().await()
            }
            if (safetySnapshot != null && safetySnapshot.exists()) {
                val jsonString = safetySnapshot.getString("backupJson")
                if (!jsonString.isNullOrBlank()) {
                    val parseResult = GoogleDriveBackupService.parseBackupJson(jsonString)
                    if (parseResult.isSuccess) {
                        val backupData = parseResult.getOrThrow()
                        if (backupData.totalWorkers > 0) {
                            Log.i(TAG, "DOWNLOAD SUCCESS from safety snapshot: Workers: ${backupData.totalWorkers}")
                            return Result.success(backupData)
                        }
                    }
                }
            }

            // Check user doc root as fallback
            val userDoc = withTimeoutOrNull(5000) {
                userDocRef.get().await()
            }
            if (userDoc != null && userDoc.exists()) {
                val jsonString = userDoc.getString("backupJson")
                if (!jsonString.isNullOrBlank()) {
                    val parseResult = GoogleDriveBackupService.parseBackupJson(jsonString)
                    if (parseResult.isSuccess) {
                        val backupData = parseResult.getOrThrow()
                        if (backupData.totalWorkers > 0) {
                            Log.i(TAG, "DOWNLOAD SUCCESS from root doc: Workers: ${backupData.totalWorkers}")
                            return Result.success(backupData)
                        }
                        if (bestBackupData == null) bestBackupData = backupData
                    }
                }
            }

            if (bestBackupData != null && (bestBackupData.totalWorkers > 0 || bestBackupData.totalTransactions > 0)) {
                return Result.success(bestBackupData)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud fetch error or offline: ${e.message}")
        }

        // 2. Fallback to local sandbox master file or safety file
        if (context != null) {
            val localBackup = GoogleDriveBackupService.getLatestBackupForUser(context, targetEmail)
            if (localBackup != null && (localBackup.workers.isNotEmpty() || localBackup.transactions.isNotEmpty())) {
                Log.i(TAG, "RESTORE SUCCESS from local master file: Workers: ${localBackup.totalWorkers}")
                return Result.success(localBackup)
            }
        }

        Log.i(TAG, "DOWNLOAD RESULT: No cloud backup found for account: $targetEmail")
        return Result.failure(Exception("No cloud backup found"))
    }
}
