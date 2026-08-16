package com.example.data.cloud

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

    // Default Web Client ID (Can be injected or fallback)
    private const val DEFAULT_SERVER_CLIENT_ID = "279343468785-web-applet.apps.googleusercontent.com"

    fun isFirebaseInitialized(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
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
                val firebaseUser = try {
                    if (isFirebaseInitialized(context)) {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                        authResult.user
                    } else null
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase Auth signInWithCredential skipped or failed: ${e.message}")
                    null
                }

                val authUser = AuthUser(
                    uid = firebaseUser?.uid ?: "goog_${email.hashCode()}",
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl
                )
                Result.success(authUser)
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
