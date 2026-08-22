package com.example.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudSyncService {
    private const val TAG = "CloudSyncService"
    var isSyncing: Boolean = false
    var lastSyncTime: String = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())
    var isCloudEncrypted: Boolean = true

    /**
     * Mandatory schema initialization during authentication/sync handshake.
     * Enforces that userEmail, totalAdvanceGiven, cashbookBalance, lastAppOpened,
     * and workerSummary are always present in the root Firestore user document.
     */
    suspend fun enforceMandatoryMetadata(
        userEmail: String? = null,
        uid: String? = null,
        workerCount: Int = 0,
        transactionCount: Int = 0,
        totalAdvanceGiven: Double = 0.0,
        cashbookBalance: Double = 0.0,
        totalCashIn: Double = 0.0,
        totalCashOut: Double = 0.0,
        workerSummary: String = "No workers registered"
    ): Result<String> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val currentUid = uid ?: auth.currentUser?.uid
            val currentEmail = (if (!userEmail.isNullOrBlank()) userEmail else auth.currentUser?.email ?: "").trim().lowercase()

            val docRef = if (currentUid != null) {
                db.collection("users").document(currentUid)
            } else if (currentEmail.isNotBlank()) {
                db.collection("users").document(currentEmail)
            } else {
                return Result.failure(Exception("No UID or userEmail available for authentication handshake"))
            }

            val timeStr = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())
            lastSyncTime = timeStr

            val enforcedWorkerSummary = if (workerSummary.isBlank()) "No workers registered" else workerSummary

            // Explicitly build the mandatory fields map
            val mandatoryMetadata = hashMapOf<String, Any?>(
                // Mandatory required schema keys
                "userEmail" to currentEmail,
                "email" to currentEmail,
                "totalAdvanceGiven" to totalAdvanceGiven,
                "cashbookBalance" to cashbookBalance,
                "lastAppOpened" to timeStr,
                "workerSummary" to enforcedWorkerSummary,
                "workersSummary" to enforcedWorkerSummary,

                // Core identifiers and timestamps
                "uid" to (currentUid ?: currentEmail),
                "lastActive" to timeStr,
                "lastSyncTime" to timeStr,
                "lastBackupTime" to timeStr,
                "workerCount" to workerCount,
                "transactionCount" to transactionCount,
                "totalCashIn" to totalCashIn,
                "totalCashOut" to totalCashOut,
                "encryption" to "AES-256-GCM / Cloud Firestore Encrypted",

                // Clean up legacy unused keys to prevent database clutter
                "accountRole" to FieldValue.delete(),
                "accountStatus" to FieldValue.delete(),
                "appVersion" to FieldValue.delete(),
                "currency" to FieldValue.delete(),
                "dataIntegrity" to FieldValue.delete(),
                "devicePlatform" to FieldValue.delete(),
                "locale" to FieldValue.delete(),
                "offlineChangesPending" to FieldValue.delete(),
                "subscriptionPlan" to FieldValue.delete(),
                "syncDevice" to FieldValue.delete(),
                "syncStatus" to FieldValue.delete(),
                "timezone" to FieldValue.delete()
            )

            // Perform transactional write to ensure consistency across the entire database
            db.runTransaction { transaction ->
                transaction.set(docRef, mandatoryMetadata, SetOptions.merge())
            }.await()

            Log.d(TAG, "Mandatory metadata schema initialized for: $currentEmail")
            Result.success("Mandatory metadata initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize mandatory schema: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Initializes or updates the complete user metadata schema in Firestore.
     */
    suspend fun initializeOrUpdateUserSchema(
        userEmail: String? = null,
        uid: String? = null,
        workerCount: Int = 0,
        transactionCount: Int = 0,
        totalAdvanceGiven: Double = 0.0,
        cashbookBalance: Double = 0.0,
        totalCashIn: Double = 0.0,
        totalCashOut: Double = 0.0,
        workerSummary: String = "No workers registered"
    ): Result<String> {
        return enforceMandatoryMetadata(
            userEmail = userEmail,
            uid = uid,
            workerCount = workerCount,
            transactionCount = transactionCount,
            totalAdvanceGiven = totalAdvanceGiven,
            cashbookBalance = cashbookBalance,
            totalCashIn = totalCashIn,
            totalCashOut = totalCashOut,
            workerSummary = workerSummary
        )
    }

    /**
     * Performs cloud sync with mandatory schema enforcement.
     */
    suspend fun syncDataToCloud(
        workerCount: Int = 0,
        transactionCount: Int = 0,
        totalAdvanceGiven: Double = 0.0,
        cashbookBalance: Double = 0.0,
        workerSummary: String = "No workers registered"
    ): Result<String> {
        return try {
            isSyncing = true
            val result = enforceMandatoryMetadata(
                workerCount = workerCount,
                transactionCount = transactionCount,
                totalAdvanceGiven = totalAdvanceGiven,
                cashbookBalance = cashbookBalance,
                workerSummary = workerSummary
            )
            isSyncing = false
            result
        } catch (e: Exception) {
            isSyncing = false
            Result.failure(e)
        }
    }
}
