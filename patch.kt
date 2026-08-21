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
            
            // IF EMPTY AND WE HAVE AN EMAIL, TRY TO FETCH BY EMAIL INSTEAD!
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
