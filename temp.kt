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
                ?: return Result.failure(Exception("No user logged in"))
            val userDoc = db.collection("users").document(uid)

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
    suspend fun deleteWorker(workerId: String, context: Context? = null) {
        val db = getFirestoreInstance(context) ?: return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection("users").document(uid).collection("workers").document(workerId).delete()
        } catch (e: Exception) { }
    }
    
    suspend fun deleteTransaction(txId: String, context: Context? = null) {
        val db = getFirestoreInstance(context) ?: return
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection("users").document(uid).collection("payments").document(txId).delete()
        } catch (e: Exception) { }
    }
}
