package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.LaborWorker
import com.example.domain.model.CashTransaction
import com.example.domain.model.UserProfile
import com.example.domain.model.DailyAttendance
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.PaymentMethod
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirestoreSyncService {
    private const val TAG = "FirestoreSyncService"
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
                            .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                            .setApiKey(BuildConfig.FIREBASE_API_KEY)
                            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                            .setGcmSenderId(BuildConfig.FIREBASE_GCM_SENDER_ID)
                            .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
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
                    "salaryType" to w.salaryType,
                    "workerId" to w.id
                )
            }
            val workersDetailSummary = if (workers.isEmpty()) {
                "No workers registered"
            } else {
                workers.joinToString("; ") {
                    val wageFormatted = if (it.dailyWage % 1.0 == 0.0) it.dailyWage.toInt().toString() else it.dailyWage.toString()
                    val phoneFormatted = if (it.phoneNumber.isNotBlank()) it.phoneNumber else "No Phone"
                    val rateUnit = if (it.salaryType.equals("Monthly", ignoreCase = true)) "month" else "day"
                    "${it.name} ($phoneFormatted): ₹$wageFormatted/$rateUnit"
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
                    syncScope.launch {
                        try {
                            val profileToSave = profileDoc.toObject(UserProfile::class.java) ?: UserProfile(isLoggedIn = true, email = userEmail)
                            val workersToSave = legacyWorkersRes.documents.mapNotNull { parseWorkerDocument(it) }
                            val txToSave = txRes.documents.mapNotNull { it.toObject(CashTransaction::class.java) }
                            syncDataToCloud(profileToSave, workersToSave, txToSave, context)
                        } catch (e: Exception) {
                            Log.w(TAG, "Legacy data auto-migration background sync failed: ${e.message}", e)
                        }
                    }
                }
            }

            val profile = profileDoc.toObject(UserProfile::class.java) ?: UserProfile(isLoggedIn = true, email = userEmail ?: "")

            // Fetch Workers (with exact property & overtimeRate extraction)
            val workers = workersRes.documents.mapNotNull { parseWorkerDocument(it) }

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
            syncScope.launch {
                try {
                    syncDataToCloud(profile, workers, transactions, context)
                } catch (e: Exception) {
                    Log.w(TAG, "Post-download metadata background sync failed: ${e.message}", e)
                }
            }

            Result.success(backup)
        } catch (e: Exception) {
            Log.d(TAG, "Download note: ${e.message}")
            Result.failure(e)
        }
    }

    fun parseWorkerDocument(doc: DocumentSnapshot): LaborWorker? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val name = doc.getString("name") ?: ""
            val phoneNumber = doc.getString("phoneNumber") ?: ""
            val dailyWage = doc.getDouble("dailyWage") ?: (doc.get("dailyWage") as? Number)?.toDouble() ?: 0.0
            val salaryType = doc.getString("salaryType") ?: "Daily"
            val avatarColorHex = doc.getString("avatarColorHex") ?: "#1656D6"
            val createdAt = doc.getLong("createdAt") ?: (doc.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()
            
            val rawAttendance = doc.get("attendance") as? Map<*, *>
            val attendanceMap = mutableMapOf<String, DailyAttendance>()
            
            if (rawAttendance != null) {
                for ((key, value) in rawAttendance) {
                    val dateKey = key?.toString() ?: continue
                    if (value is Map<*, *>) {
                        val dayNum = (value["dayNumber"] as? Number)?.toInt() ?: 1
                        val dayOfWeek = value["dayOfWeek"]?.toString() ?: "Mon"
                        val fullDate = value["fullDate"]?.toString() ?: dateKey
                        val statusStr = value["status"]?.toString() ?: "UNMARKED"
                        val status = try {
                            AttendanceStatus.valueOf(statusStr)
                        } catch (_: Exception) {
                            AttendanceStatus.fromSymbol(statusStr)
                        }
                        val otHours = (value["overtimeHours"] as? Number)?.toDouble() ?: 0.0
                        val otRate = (value["overtimeRate"] as? Number)?.toDouble() ?: 0.0
                        val adv = (value["advanceAmount"] as? Number)?.toDouble() ?: 0.0
                        val note = value["note"]?.toString() ?: ""
                        val pmStr = value["paymentMethod"]?.toString() ?: "ONLINE"
                        val pm = try { PaymentMethod.valueOf(pmStr) } catch (_: Exception) { PaymentMethod.ONLINE }
                        
                        attendanceMap[dateKey] = DailyAttendance(
                            dayNumber = dayNum,
                            dayOfWeek = dayOfWeek,
                            fullDate = fullDate,
                            status = status,
                            overtimeHours = otHours,
                            overtimeRate = otRate,
                            advanceAmount = adv,
                            note = note,
                            paymentMethod = pm
                        )
                    }
                }
            } else {
                val directWorker = doc.toObject(LaborWorker::class.java)
                if (directWorker != null) {
                    attendanceMap.putAll(directWorker.attendance)
                }
            }
            
            LaborWorker(
                id = id,
                name = name,
                phoneNumber = phoneNumber,
                dailyWage = dailyWage,
                salaryType = salaryType,
                avatarColorHex = avatarColorHex,
                attendance = attendanceMap,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            doc.toObject(LaborWorker::class.java)
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
        } catch (e: Exception) {
            Log.w(TAG, "deleteWorker cloud sync note: ${e.message}")
        }
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
        } catch (e: Exception) {
            Log.w(TAG, "deleteTransaction cloud sync note: ${e.message}")
        }
    }

    fun recordAppOpened(context: Context? = null) {
        syncScope.launch {
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
            } catch (e: Exception) {
                Log.w(TAG, "recordAppOpened background sync failed: ${e.message}", e)
            }
        }
    }
}
