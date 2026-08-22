package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.domain.model.LaborWorker
import com.example.domain.model.CashTransaction
import com.example.domain.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

object FirestoreSyncService {
    private const val TAG = "FirestoreSyncService"
    private var isFirestoreConfigured = false

    private fun getFirestoreInstance(context: Context? = null): FirebaseFirestore? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (_: Throwable) {}
                if (FirebaseApp.getApps(context).isEmpty()) {
                    try {
                        val options = FirebaseOptions.Builder()
                            .setApplicationId("1:1027179208222:android:ac3483799fc5ed6c6a580f")
                            .setApiKey("AIzaSyAMeOVp4gfkmBrOv_uMfOUuokXHQLFwFZY")
                            .setProjectId("laborbook-4c47e")
                            .setGcmSenderId("1027179208222")
                            .setStorageBucket("laborbook-4c47e.firebasestorage.app")
                            .build()
                        FirebaseApp.initializeApp(context.applicationContext, options)
                    } catch (_: Throwable) {}
                }
            }

            val db = FirebaseFirestore.getInstance()
            if (!isFirestoreConfigured) {
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build()
                    db.firestoreSettings = settings
                    isFirestoreConfigured = true
                } catch (_: Throwable) {
                    // Settings might already be locked if instance was accessed earlier
                }
            }
            db
        } catch (e: Throwable) {
            Log.e(TAG, "Error getting Firestore instance: ${e.message}")
            null
        }
    }

    private fun isFirebaseAvailable(context: Context? = null): Boolean {
        return getFirestoreInstance(context) != null
    }

    suspend fun syncDataToCloud(
        profile: UserProfile,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        context: Context? = null
    ): Result<String> {
        val db = getFirestoreInstance(context)
            ?: return Result.failure(Exception("Cloud sync unavailable: Firebase not initialized"))

        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val userEmail = (if (!profile.email.isNullOrBlank()) profile.email else FirebaseAuth.getInstance().currentUser?.email ?: "").trim().lowercase()
            
            val userDoc = if (uid != null) {
                db.collection("users").document(uid)
            } else if (userEmail.isNotBlank()) {
                db.collection("users").document(userEmail.lowercase())
            } else {
                return Result.failure(Exception("No user logged in and no email provided"))
            }

            val currentMonthStr = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            val currentDateTimeStr = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            val totalAdvances = workers.sumOf { w -> w.attendance.values.sumOf { it.advanceAmount } }
            val cashInSum = transactions.filter { it.type == com.example.domain.model.TransactionType.CASH_IN }.sumOf { it.amount }
            val cashOutSum = transactions.filter { it.type == com.example.domain.model.TransactionType.CASH_OUT }.sumOf { it.amount }
            val cashbookNetBalance = cashInSum - cashOutSum
            val wagesSum = workers.sumOf { it.getEstimatedEarnings(currentMonthStr) }
            val outstandingBalance = wagesSum - totalAdvances

            val workersDetailList = workers.map { w ->
                mapOf(
                    "name" to w.name,
                    "phoneNumber" to w.phoneNumber,
                    "dailyWage" to w.dailyWage,
                    "workerId" to w.id
                )
            }
            val workersDetailSummary = if (workers.isEmpty()) {
                "No workers registered"
            } else {
                workers.joinToString("; ") {
                    val wageFormatted = if (it.dailyWage % 1.0 == 0.0) it.dailyWage.toInt().toString() else it.dailyWage.toString()
                    val phoneFormatted = if (it.phoneNumber.isNotBlank()) it.phoneNumber else "No Phone"
                    "${it.name} ($phoneFormatted): ₹$wageFormatted/day"
                }
            }

            var creationDate = "Unknown"
            try {
                FirebaseAuth.getInstance().currentUser?.metadata?.creationTimestamp?.let {
                    creationDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                }
            } catch (e: Exception) {}

            // Save comprehensive metadata to the root user document
            val metadata: HashMap<String, Any?> = hashMapOf(
                "uid" to (uid ?: userEmail),
                "userEmail" to userEmail,
                "email" to userEmail,
                "encryption" to "AES-256-GCM / Cloud Firestore Encrypted",
                "lastSyncTime" to currentDateTimeStr,
                "lastBackupTime" to currentDateTimeStr,
                "lastAppOpened" to currentDateTimeStr,
                "lastActive" to currentDateTimeStr,
                "workerCount" to workers.size,
                "transactionCount" to transactions.size,
                "totalAdvanceGiven" to totalAdvances,
                "cashbookBalance" to cashbookNetBalance,
                "totalCashIn" to cashInSum,
                "totalCashOut" to cashOutSum,
                "workersList" to workersDetailList,
                "workerSummary" to workersDetailSummary,
                "workersSummary" to workersDetailSummary,
                "totalOutstandingBalance" to outstandingBalance,
                "totalPendingWages" to wagesSum,
                "accountCreationDate" to (if (creationDate == "Unknown") currentDateTimeStr else creationDate),
                // Explicitly delete unwanted fields from existing documents in Firestore
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
            // Transactional update on root document
            db.runTransaction { txn ->
                txn.set(userDoc, metadata, SetOptions.merge())
            }.await()

            // 1. Save Profile
            userDoc.collection("profile").document("settings").set(profile, SetOptions.merge()).await()

            // 2. Save Workers
            var batch = db.batch()
            var operationCount = 0

            for (worker in workers) {
                val docRef = userDoc.collection("workers").document(worker.id)
                batch.set(docRef, worker, SetOptions.merge())
                operationCount++

                if (operationCount >= 450) {
                    batch.commit().await()
                    batch = db.batch()
                    operationCount = 0
                }
            }
            
            // 3. Save Transactions
            for (tx in transactions) {
                val docRef = userDoc.collection("payments").document(tx.id)
                batch.set(docRef, tx, SetOptions.merge())
                operationCount++

                if (operationCount >= 450) {
                    batch.commit().await()
                    batch = db.batch()
                    operationCount = 0
                }
            }

            if (operationCount > 0) {
                batch.commit().await()
            }

            Result.success("Synced successfully (${workers.size} workers, ${transactions.size} transactions).")
        } catch (e: Exception) {
            Log.d(TAG, "Sync note: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun downloadDataFromCloud(context: Context? = null, fallbackEmail: String? = null): Result<BackupData> {
        val db = getFirestoreInstance(context)
            ?: return Result.failure(Exception("Cloud sync unavailable: Firebase not initialized"))

        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: fallbackEmail
            
            var userDoc = if (uid != null) db.collection("users").document(uid) else null
            
            if (userDoc == null && userEmail != null) {
                userDoc = db.collection("users").document(userEmail.lowercase())
            }

            if (userDoc == null) {
                return Result.failure(Exception("No user logged in and no fallback email provided"))
            }

            // Fetch Profile
            var profileDoc = userDoc.collection("profile").document("settings").get().await()
            var workersRes = userDoc.collection("workers").get().await()
            var txRes = userDoc.collection("payments").get().await()
            
            // IF EMPTY AND WE HAVE AN EMAIL, TRY TO FETCH BY LEGACY EMAIL ID!
            if (workersRes.documents.isEmpty() && txRes.documents.isEmpty() && userEmail != null && uid != null) {
                val legacyUserDoc = db.collection("users").document(userEmail.lowercase())
                val legacyWorkersRes = legacyUserDoc.collection("workers").get().await()
                if (legacyWorkersRes.documents.isNotEmpty()) {
                    userDoc = db.collection("users").document(uid) // Switch to new UID doc
                    profileDoc = legacyUserDoc.collection("profile").document("settings").get().await()
                    workersRes = legacyWorkersRes
                    txRes = legacyUserDoc.collection("payments").get().await()
                    
                    // Auto-migrate in background
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val profileToSave = profileDoc.toObject(UserProfile::class.java) ?: UserProfile(isLoggedIn = true, email = userEmail)
                            val workersToSave = workersRes.documents.mapNotNull { it.toObject(LaborWorker::class.java) }
                            val txToSave = txRes.documents.mapNotNull { it.toObject(CashTransaction::class.java) }
                            syncDataToCloud(profileToSave, workersToSave, txToSave, context)
                        } catch (_: Exception) {}
                    }
                }
            }

            val profile = profileDoc.toObject(UserProfile::class.java) ?: UserProfile(isLoggedIn = true, email = userEmail ?: "")

            // Fetch Workers
            val rawWorkers = workersRes.documents.mapNotNull { it.toObject(LaborWorker::class.java) }
            val workers = rawWorkers

            // Fetch Transactions
            val transactions = txRes.documents.mapNotNull { it.toObject(CashTransaction::class.java) }

            val backup = BackupData(
                userProfile = profile,
                workers = workers,
                transactions = transactions,
                totalWorkers = workers.size,
                totalTransactions = transactions.size,
                backupTimestamp = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                accountEmail = profile.email
            )

            // Immediately update the root document with full metadata so old users get new fields automatically
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    syncDataToCloud(profile, workers, transactions, context)
                } catch (_: Exception) {}
            }

            Result.success(backup)
        } catch (e: Exception) {
            Log.d(TAG, "Download note: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun saveWorker(worker: LaborWorker, context: Context? = null) {
        val db = getFirestoreInstance(context) ?: return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val userEmail = FirebaseAuth.getInstance().currentUser?.email
            
            var userDoc = if (uid != null) db.collection("users").document(uid) else null
            if (userDoc == null && userEmail != null) {
                userDoc = db.collection("users").document(userEmail.lowercase())
            }
            if (userDoc != null) {
                userDoc.collection("workers").document(worker.id).set(worker, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(TAG, "saveWorker cloud sync note: ${e.message}")
        }
    }

    suspend fun saveTransaction(tx: CashTransaction, context: Context? = null) {
        val db = getFirestoreInstance(context) ?: return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val userEmail = FirebaseAuth.getInstance().currentUser?.email
            
            var userDoc = if (uid != null) db.collection("users").document(uid) else null
            if (userDoc == null && userEmail != null) {
                userDoc = db.collection("users").document(userEmail.lowercase())
            }
            if (userDoc != null) {
                userDoc.collection("payments").document(tx.id).set(tx, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(TAG, "saveTransaction cloud sync note: ${e.message}")
        }
    }

    suspend fun deleteWorker(workerId: String, context: Context? = null) {
        val db = getFirestoreInstance(context) ?: return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val userEmail = FirebaseAuth.getInstance().currentUser?.email
            
            var userDoc = if (uid != null) db.collection("users").document(uid) else null
            if (userDoc == null && userEmail != null) {
                userDoc = db.collection("users").document(userEmail.lowercase())
            }
            if (userDoc != null) {
                userDoc.collection("workers").document(workerId).delete()
            }
        } catch (e: Exception) { }
    }
    
    suspend fun deleteTransaction(txId: String, context: Context? = null) {
        val db = getFirestoreInstance(context) ?: return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val userEmail = FirebaseAuth.getInstance().currentUser?.email
            
            var userDoc = if (uid != null) db.collection("users").document(uid) else null
            if (userDoc == null && userEmail != null) {
                userDoc = db.collection("users").document(userEmail.lowercase())
            }
            if (userDoc != null) {
                userDoc.collection("payments").document(txId).delete()
            }
        } catch (e: Exception) { }
    }

    fun recordAppOpened(context: Context? = null) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val db = getFirestoreInstance(context) ?: return@launch
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val userEmail = FirebaseAuth.getInstance().currentUser?.email
                val userDoc = if (uid != null) {
                    db.collection("users").document(uid)
                } else if (!userEmail.isNullOrBlank()) {
                    db.collection("users").document(userEmail.lowercase())
                } else null

                if (userDoc != null) {
                    val currentDateTimeStr = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                    userDoc.set(
                        mapOf(
                            "lastAppOpened" to currentDateTimeStr,
                            "lastActive" to currentDateTimeStr
                        ),
                        SetOptions.merge()
                    )
                }
            } catch (_: Exception) {}
        }
    }
}
