package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

import com.google.firebase.FirebaseOptions

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false
)

object FirebaseAuthHelper {
    private const val TAG = "FirebaseAuthHelper"

    // Default Web Client ID (From google-services.json)
    private val DEFAULT_SERVER_CLIENT_ID = try {
        if (com.example.BuildConfig.WEB_CLIENT_ID.isNotBlank()) {
            com.example.BuildConfig.WEB_CLIENT_ID
        } else {
            "1027179208222-2hhdrgohaaa7ed068smm0tekptejq4k8.apps.googleusercontent.com"
        }
    } catch (e: Throwable) {
        "1027179208222-2hhdrgohaaa7ed068smm0tekptejq4k8.apps.googleusercontent.com"
    }

    fun isFirebaseInitialized(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    // Ignored
                }
            }
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
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize Firebase with options: ${e.message}")
                }
            }
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun isOnline(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    if (capabilities != null) {
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                    } else false
                } else false
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentFirebaseUser(context: Context? = null): FirebaseUser? {
        return try {
            if (context != null && !isFirebaseInitialized(context)) return null
            FirebaseAuth.getInstance().currentUser
        } catch (e: Throwable) {
            null
        }
    }

    fun getCurrentAuthUser(): AuthUser? {
        val fbUser = getCurrentFirebaseUser()
        return if (fbUser != null) {
            AuthUser(
                uid = fbUser.uid,
                displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                email = fbUser.email ?: "",
                photoUrl = fbUser.photoUrl?.toString(),
                isAnonymous = fbUser.isAnonymous
            )
        } else null
    }

    /**
     * Signs in with Google Credential Manager and links to Firebase Authentication.
     */
    suspend fun signInWithGoogleCredentialManager(
        context: Context,
        serverClientId: String = DEFAULT_SERVER_CLIENT_ID
    ): Result<AuthUser> {
        val targetContext = findActivity(context) ?: context
        val credentialManager = CredentialManager.create(targetContext)

        return try {
            Log.i(TAG, "Requesting Google Credential Manager sign-in with serverClientId: $serverClientId")
            
            // Build Google ID Option for Credential Manager
            val googleIdOption = try {
                GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()
            } catch (e: Throwable) {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = targetContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                Log.i(TAG, "Successfully received Google ID Token for: $email")

                // Authenticate with Firebase if Firebase is available
                if (isFirebaseInitialized(targetContext)) {
                    try {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                        val firebaseUser = authResult.user
                        if (firebaseUser != null) {
                            val authUser = AuthUser(
                                uid = firebaseUser.uid,
                                displayName = displayName,
                                email = email,
                                photoUrl = photoUrl
                            )
                            Result.success(authUser)
                        } else {
                            Result.success(AuthUser(uid = email, displayName = displayName, email = email, photoUrl = photoUrl))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase Auth signInWithCredential notice: ${e.message}. Proceeding with verified Google user.")
                        Result.success(AuthUser(uid = email, displayName = displayName, email = email, photoUrl = photoUrl))
                    }
                } else {
                    Result.success(AuthUser(uid = email, displayName = displayName, email = email, photoUrl = photoUrl))
                }
            } else {
                Log.w(TAG, "Received unexpected credential type: ${credential.type}")
                Result.failure(Exception("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Google sign-in cancelled by user.")
            Result.failure(Exception("Sign-in cancelled by user."))
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager API exception: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }



    /**
     * Signs in with Email and Password using Firebase Auth, with seamless local & cloud account recovery.
     */
    /**
     * Signs in with Email and Password using Firebase Auth strictly. No local-only account fallback.
     */
    suspend fun signInWithEmail(context: Context, email: String, pass: String): Result<AuthUser> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val cleanPass = pass.trim()
            if (cleanEmail.isBlank() || cleanPass.isBlank()) return Result.failure(Exception("Email and password cannot be empty."))
            
            val localName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            val localPrefs = context.getSharedPreferences("laborbook_auth_accounts", Context.MODE_PRIVATE)
            val savedName = localPrefs.getString("name_$cleanEmail", localName) ?: "User"

            if (isFirebaseInitialized(context)) {
                try {
                    val authResult = FirebaseAuth.getInstance().signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        val authUser = AuthUser(
                            uid = fbUser.uid,
                            displayName = fbUser.displayName ?: savedName,
                            email = fbUser.email ?: cleanEmail
                        )
                        saveLocalAccount(context, cleanEmail, cleanPass, authUser.displayName)
                        return Result.success(authUser)
                    } else {
                        return Result.failure(Exception("Failed to retrieve user profile from server."))
                    }
                } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                    return Result.failure(Exception("Incorrect password for $cleanEmail. Please enter the correct password."))
                } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                    return Result.failure(Exception("Account does not exist for $cleanEmail. Please select 'Create Account' to register first."))
                } catch (e: com.google.firebase.auth.FirebaseAuthException) {
                    val errorCode = e.errorCode
                    Log.e(TAG, "Firebase Auth Exception code: $errorCode, message: ${e.message}")
                    if (errorCode == "ERROR_USER_NOT_FOUND" || errorCode == "ERROR_INVALID_USER" || errorCode == "ERROR_USER_DISABLED") {
                        return Result.failure(Exception("Account does not exist for $cleanEmail. Please select 'Create Account' to register first."))
                    } else if (errorCode == "ERROR_WRONG_PASSWORD") {
                        return Result.failure(Exception("Incorrect password for $cleanEmail. Please enter the correct password."))
                    }
                    return Result.failure(Exception("Authentication error: ${e.localizedMessage}"))
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase Sign In generic exception: ${e.message}", e)
                    return Result.failure(Exception("Could not authenticate with server. Error: ${e.localizedMessage}"))
                }
            } else {
                return Result.failure(Exception("Firebase Authentication service is not initialized on this device."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers a new account with Email and Password using Firebase Auth strictly. No local-only mock account creation.
     */
    suspend fun signUpWithEmail(context: Context, email: String, pass: String, displayName: String = ""): Result<AuthUser> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val cleanPass = pass.trim()
            if (cleanEmail.isBlank() || cleanPass.isBlank()) return Result.failure(Exception("Email and password cannot be empty."))
            if (cleanPass.length < 6) return Result.failure(Exception("Password must be at least 6 characters long."))
            val cleanName = if (displayName.isNotBlank()) displayName else cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            if (isFirebaseInitialized(context)) {
                try {
                    val authResult = FirebaseAuth.getInstance().createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        try {
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(cleanName)
                                .build()
                            fbUser.updateProfile(profileUpdates).await()
                        } catch (pe: Exception) {
                            Log.w(TAG, "Failed to update Firebase profile display name: ${pe.message}")
                        }

                        val authUser = AuthUser(
                            uid = fbUser.uid,
                            displayName = cleanName,
                            email = fbUser.email ?: cleanEmail
                        )
                        saveLocalAccount(context, cleanEmail, cleanPass, cleanName)
                        return Result.success(authUser)
                    } else {
                        return Result.failure(Exception("Failed to register account on server."))
                    }
                } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    return Result.failure(Exception("An account already exists with $cleanEmail. Please switch to 'Sign In' instead."))
                } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
                    return Result.failure(Exception("Password is too weak. Please use at least 6 characters."))
                } catch (e: com.google.firebase.auth.FirebaseAuthException) {
                    return Result.failure(Exception("Registration failed: ${e.localizedMessage}"))
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase Sign Up generic exception: ${e.message}", e)
                    return Result.failure(Exception("Could not connect to registration server. Please try again later."))
                }
            } else {
                return Result.failure(Exception("Firebase Authentication service is not initialized on this device."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveLocalAccount(context: Context, email: String, pass: String, displayName: String) {
        try {
            val localPrefs = context.getSharedPreferences("laborbook_auth_accounts", Context.MODE_PRIVATE)
            localPrefs.edit()
                .putString("pass_$email", pass)
                .putString("name_$email", displayName)
                .apply()
        } catch (_: Exception) {}
    }

    /**
     * Sends a password reset email using Firebase Auth.
     */
    suspend fun resetPassword(context: Context, email: String): Result<Unit> {
        return try {
            if (isFirebaseInitialized(context)) {
                try {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                    return Result.success(Unit)
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase reset password notice: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Attempts to query device accounts registered on Android
     */
    fun getDeviceAccounts(context: Context): List<String> {
        return try {
            val am = android.accounts.AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            accounts.mapNotNull { it.name }.filter { it.contains("@") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Signs out from Firebase Authentication and clears credentials.
     */
    suspend fun signOut(context: Context) {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign out notice: ${e.message}")
        }
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "CredentialManager clear state notice: ${e.message}")
        }
    }
}
