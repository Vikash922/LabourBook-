package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.data.model.LaborWorker
import com.example.data.model.CashTransaction
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirestoreSyncService {
    private const val TAG = "FirestoreSyncService"

    private fun isFirebaseAvailable(context: Context? = null): Boolean {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Throwable) {
                    // Ignored
                }
            }
            FirebaseApp.getApps(context ?: FirebaseApp.getInstance().applicationContext).isNotEmpty()
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun syncDataToCloud(
        profile: UserProfile,
        workers: List<LaborWorker>,
        transactions: List<CashTransaction>,
        context: Context? = null
    ): Result<String> {
        if (!isFirebaseAvailable(context)) {
            Log.d(TAG, "Sync skipped: FirebaseApp is not initialized on this device.")
            return Result.failure(Exception("Cloud sync unavailable: Firebase not initialized"))
        }
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid 
                ?: return Result.failure(Exception("No user logged in"))
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(uid)

            // 1. Save Profile
            userDoc.collection("profile").document("settings").set(profile, SetOptions.merge()).await()

            // 2. Save Workers
            val batch = db.batch()
            for (worker in workers) {
                val docRef = userDoc.collection("workers").document(worker.id)
                batch.set(docRef, worker, SetOptions.merge())
            }
            
            // 3. Save Transactions
            for (tx in transactions) {
                val docRef = userDoc.collection("payments").document(tx.id)
                batch.set(docRef, tx, SetOptions.merge())
            }
            batch.commit().await()

            Result.success("Synced successfully (${workers.size} workers, ${transactions.size} transactions).")
        } catch (e: Exception) {
            Log.d(TAG, "Sync note: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun downloadDataFromCloud(context: Context? = null): Result<BackupData> {
        if (!isFirebaseAvailable(context)) {
            Log.d(TAG, "Download skipped: FirebaseApp is not initialized on this device.")
            return Result.failure(Exception("Cloud sync unavailable: Firebase not initialized"))
        }
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid 
                ?: return Result.failure(Exception("No user logged in"))
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(uid)

            // Fetch Profile
            val profileDoc = userDoc.collection("profile").document("settings").get().await()
            val profile = profileDoc.toObject(UserProfile::class.java) ?: UserProfile(isLoggedIn = true)

            // Fetch Workers
            val workersRes = userDoc.collection("workers").get().await()
            val workers = workersRes.documents.mapNotNull { it.toObject(LaborWorker::class.java) }

            // Fetch Transactions
            val txRes = userDoc.collection("payments").get().await()
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
        if (!isFirebaseAvailable(context)) return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("workers").document(workerId).delete().await()
        } catch (e: Exception) { }
    }
    
    suspend fun deleteTransaction(txId: String, context: Context? = null) {
        if (!isFirebaseAvailable(context)) return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("payments").document(txId).delete().await()
        } catch (e: Exception) { }
    }
}
