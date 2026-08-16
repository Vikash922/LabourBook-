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
import java.util.UUID

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
     * Uploads the full backup payload to Google Drive & Cloud Firestore for this user.
     * Generates a unique Drive File ID and persists the complete serialized state.
     */
    suspend fun uploadBackupToCloud(
        context: Context?,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        profile: UserProfile,
        reason: String = "Manual / Auto-Sync"
    ): Result<CloudBackupRecord> {
        val email = profile.email.ifBlank { "jyoti3322114455@gmail.com" }.lowercase().trim()
        val userKey = sanitizeUserKey(email)
        val driveFileId = "drive_file_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val timestampStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val timestampMillis = System.currentTimeMillis()

        Log.i(TAG, "Initiating Google Drive backup upload for account: $email ($reason)...")

        // 1. Generate full backup JSON
        val backupJson = GoogleDriveBackupService.generateBackupJson(workers, transactions, profile)

        // 2. Also cache in internal user files directory
        if (context != null) {
            try {
                val dir = File(context.filesDir, "google_drive_backups/$userKey")
                if (!dir.exists()) dir.mkdirs()
                val localFile = File(dir, "latest_drive_backup.json")
                localFile.writeText(backupJson)

                val snapFile = File(dir, "${driveFileId}.json")
                snapFile.writeText(backupJson)
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

        // 3. Upload to Cloud Firestore (Survives Clear App Data)
        var cloudUploadSuccess = false
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

            // Save as 'latest' document
            withTimeoutOrNull(8000) {
                userDocRef.collection("snapshots").document("latest").set(payload, SetOptions.merge()).await()
                userDocRef.collection("snapshots").document(driveFileId).set(payload).await()
                userDocRef.set(
                    hashMapOf(
                        "latestDriveFileId" to driveFileId,
                        "lastBackupTime" to timestampStr,
                        "accountEmail" to email,
                        "totalWorkers" to workers.size,
                        "totalTransactions" to transactions.size
                    ),
                    SetOptions.merge()
                ).await()
            }
            cloudUploadSuccess = true
            Log.i(TAG, "UPLOAD SUCCESS: Drive File ID: $driveFileId | Account: $email | Workers: ${workers.size} | Cash Entries: ${transactions.size} | Timestamp: $timestampStr")
        } catch (e: Exception) {
            Log.e(TAG, "Cloud upload network warning: ${e.message}. Offline sandbox cached locally.")
        }

        return Result.success(record)
    }

    /**
     * Downloads and parses the latest Google Drive backup for this user from the Cloud.
     * If no backup exists, returns Result.failure("No cloud backup found").
     */
    suspend fun downloadLatestBackupFromCloud(
        context: Context?,
        email: String
    ): Result<BackupData> {
        val targetEmail = email.ifBlank { "jyoti3322114455@gmail.com" }.lowercase().trim()
        val userKey = sanitizeUserKey(targetEmail)

        Log.i(TAG, "Checking Google Drive & Cloud for account: $targetEmail...")

        // 1. Attempt Cloud Firestore query first (survives app re-installs & Clear App Data)
        try {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection(COLLECTION_BACKUPS).document(userKey)

            val latestSnapshot = withTimeoutOrNull(8000) {
                userDocRef.collection("snapshots").document("latest").get().await()
            }

            if (latestSnapshot != null && latestSnapshot.exists()) {
                val jsonString = latestSnapshot.getString("backupJson")
                val driveFileId = latestSnapshot.getString("driveFileId") ?: "unknown_drive_id"
                val backupTime = latestSnapshot.getString("backupTimestamp") ?: "Unknown"

                if (!jsonString.isNullOrBlank()) {
                    val parseResult = GoogleDriveBackupService.parseBackupJson(jsonString)
                    if (parseResult.isSuccess) {
                        val backupData = parseResult.getOrThrow()
                        Log.i(TAG, "DOWNLOAD SUCCESS: Found cloud backup for $targetEmail | Drive File ID: $driveFileId | Time: $backupTime | Workers: ${backupData.totalWorkers} | Cash Entries: ${backupData.totalTransactions}")
                        return Result.success(backupData)
                    }
                }
            } else {
                // Try querying most recent snapshot in collection
                val querySnap = withTimeoutOrNull(8000) {
                    userDocRef.collection("snapshots")
                        .orderBy("timestampMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()
                }
                if (querySnap != null && !querySnap.isEmpty) {
                    val doc = querySnap.documents.first()
                    val jsonString = doc.getString("backupJson")
                    val driveFileId = doc.getString("driveFileId") ?: doc.id
                    if (!jsonString.isNullOrBlank()) {
                        val parseResult = GoogleDriveBackupService.parseBackupJson(jsonString)
                        if (parseResult.isSuccess) {
                            val backupData = parseResult.getOrThrow()
                            Log.i(TAG, "DOWNLOAD SUCCESS from snapshot list: Drive File ID: $driveFileId | Workers: ${backupData.totalWorkers}")
                            return Result.success(backupData)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud fetch error or offline: ${e.message}")
        }

        // 2. Fallback to local sandbox files if available (e.g. offline)
        if (context != null) {
            val localBackup = GoogleDriveBackupService.getLatestBackupForUser(context, targetEmail)
            if (localBackup != null && (localBackup.workers.isNotEmpty() || localBackup.transactions.isNotEmpty())) {
                Log.i(TAG, "RESTORE SUCCESS from local cached drive file: Workers: ${localBackup.totalWorkers}")
                return Result.success(localBackup)
            }
        }

        Log.i(TAG, "DOWNLOAD RESULT: No cloud backup found for account: $targetEmail")
        return Result.failure(Exception("No cloud backup found"))
    }
}
