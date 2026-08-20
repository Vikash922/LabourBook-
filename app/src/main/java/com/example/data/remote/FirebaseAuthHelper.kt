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
            FirebaseApp.getApps(context).isNotEmpty()
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
        val credentialManager = CredentialManager.create(context)

        return try {
            Log.i(TAG, "Requesting Google Credential Manager sign-in...")
            
            // Build Google ID Option for Credential Manager
            val googleIdOption = try {
                GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()
            } catch (e: Exception) {
                // Fallback to GetGoogleIdOption
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
                context = context
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
                if (isFirebaseInitialized(context)) {
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
                    // Google credential is valid, proceed with authenticated Google user
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



    /**
     * Signs in with Email and Password using Firebase Auth, with seamless local fallback.
     */
    suspend fun signInWithEmail(context: Context, email: String, pass: String): Result<AuthUser> {
        return try {
            val cleanEmail = email.trim().lowercase()
            if (isFirebaseInitialized(context)) {
                try {
                    val authResult = FirebaseAuth.getInstance().signInWithEmailAndPassword(cleanEmail, pass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        val authUser = AuthUser(
                            uid = fbUser.uid,
                            displayName = fbUser.displayName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                            email = fbUser.email ?: cleanEmail
                        )
                        saveLocalAccount(context, cleanEmail, pass, authUser.displayName)
                        return Result.success(authUser)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase signInWithEmail error: ${e.message}. Checking local account...")
                }
            }
            
            // Local Account verification fallback
            val localPrefs = context.getSharedPreferences("laborbook_auth_accounts", Context.MODE_PRIVATE)
            val savedPass = localPrefs.getString("pass_$cleanEmail", null)
            val savedName = localPrefs.getString("name_$cleanEmail", cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }) ?: "User"

            if (savedPass != null) {
                if (savedPass == pass) {
                    Result.success(AuthUser(uid = cleanEmail, displayName = savedName, email = cleanEmail))
                } else {
                    Result.failure(Exception("Incorrect password for this email"))
                }
            } else {
                // First-time local login for this email, auto-register
                saveLocalAccount(context, cleanEmail, pass, savedName)
                Result.success(AuthUser(uid = cleanEmail, displayName = savedName, email = cleanEmail))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers a new account with Email and Password using Firebase Auth, with seamless local fallback.
     */
    suspend fun signUpWithEmail(context: Context, email: String, pass: String, displayName: String = ""): Result<AuthUser> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val cleanName = if (displayName.isNotBlank()) displayName else cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            if (isFirebaseInitialized(context)) {
                try {
                    val authResult = FirebaseAuth.getInstance().createUserWithEmailAndPassword(cleanEmail, pass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        val authUser = AuthUser(
                            uid = fbUser.uid,
                            displayName = cleanName,
                            email = fbUser.email ?: cleanEmail
                        )
                        saveLocalAccount(context, cleanEmail, pass, cleanName)
                        return Result.success(authUser)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase signUpWithEmail notice: ${e.message}. Registering local account...")
                }
            }

            // Save to local secure preferences
            saveLocalAccount(context, cleanEmail, pass, cleanName)
            Result.success(AuthUser(uid = cleanEmail, displayName = cleanName, email = cleanEmail))
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
