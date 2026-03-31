package com.baby.feedingtracker.data

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthHelper(
    private val auth: FirebaseAuth,
    context: Context
) {
    private val googleSignInClient: GoogleSignInClient

    init {
        val webClientId = context.getString(
            context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        )
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun isLoggedIn(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }

    fun currentUserEmail(): String? {
        val user = auth.currentUser ?: return null
        return user.providerData
            .firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            ?.email
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
                ?: return Result.failure(Exception("Google ID token is null"))

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val currentUser = auth.currentUser

            if (currentUser != null && currentUser.isAnonymous) {
                // Link anonymous account with Google credential (preserves uid)
                val result = currentUser.linkWithCredential(credential).await()
                Result.success(result.user!!.uid)
            } else if (currentUser != null) {
                // Already signed in with some provider, just return uid
                Result.success(currentUser.uid)
            } else {
                // No current user, sign in directly
                val result = auth.signInWithCredential(credential).await()
                Result.success(result.user!!.uid)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
