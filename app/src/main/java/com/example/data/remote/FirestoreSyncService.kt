package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.domain.model.LaborWorker
import com.example.domain.model.CashTransaction
import com.example.domain.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.tasks.await

object FirestoreSyncService {
    private const val TAG = "FirestoreSyncService"
    private var isFirestoreConfigured = false

    private fun getFirestoreInstance(context: Context? = null): FirebaseFirestore? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (_: Throwable) {}
            }
            if (FirebaseApp.getApps(context ?: FirebaseApp.getInstance().applicationContext).isEmpty()) {
                return null
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
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: profile.email
            
            var userDoc = if (uid != null) db.collection("users").document(uid) else null
            
            if (userDoc == null && userEmail.isNotBlank()) {
                userDoc = db.collection("users").document(userEmail.lowercase())
            }

            if (userDoc == null) {
                return Result.failure(Exception("No user logged in and no email provided"))
            }

            // 1. Save Profile
            userDoc.collection("profile").document("settings").set(profile, SetOptions.merge())

            // 2. Save Workers
            var batch = db.batch()
            var operationCount = 0

            for (worker in workers) {
                val docRef = userDoc.collection("workers").document(worker.id)
                batch.set(docRef, worker, SetOptions.merge())
                operationCount++

                if (operationCount >= 450) {
                    batch.commit()
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
                    batch.commit()
                    batch = db.batch()
                    operationCount = 0
                }
            }

            if (operationCount > 0) {
                batch.commit()
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
                    userDoc = legacyUserDoc
                    profileDoc = userDoc.collection("profile").document("settings").get().await()
                    workersRes = legacyWorkersRes
                    txRes = userDoc.collection("payments").get().await()
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
            Result.success(backup)
        } catch (e: Exception) {
            Log.d(TAG, "Download note: ${e.message}")
            Result.failure(e)
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
}
