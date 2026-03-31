package com.baby.feedingtracker.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

// ──────────────────────────────────────────────
// Data classes
// ──────────────────────────────────────────────

data class UserProfile(
    val linkedTo: String? = null,
    val dataOwnerUid: String = "",
    val email: String? = null
)

sealed class SharingState {
    object NotConnected : SharingState()
    data class Connected(val partnerEmail: String?) : SharingState()
}

// ──────────────────────────────────────────────
// Repository
// ──────────────────────────────────────────────

class UserRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // ── Profile operations ──────────────────────

    private fun profileDocRef(uid: String) =
        firestore.collection("users").document(uid).collection("profile").document("linked")

    fun getProfile(uid: String): Flow<UserProfile?> {
        return profileDocRef(uid).snapshots().map { snapshot ->
            if (snapshot.exists()) {
                UserProfile(
                    linkedTo = snapshot.getString("linkedTo"),
                    dataOwnerUid = snapshot.getString("dataOwnerUid") ?: uid,
                    email = snapshot.getString("email")
                )
            } else {
                null
            }
        }
    }

    suspend fun createProfile(uid: String, email: String?) {
        val data = hashMapOf(
            "dataOwnerUid" to uid,
            "linkedTo" to null,
            "email" to email
        )
        profileDocRef(uid).set(data).await()
    }

    suspend fun getDataOwnerUid(uid: String): String {
        val snapshot = profileDocRef(uid).get().await()
        return if (snapshot.exists()) {
            snapshot.getString("dataOwnerUid") ?: uid
        } else {
            uid
        }
    }

    // ── Invite code operations ──────────────────

    private fun inviteCodeDocRef(code: String) =
        firestore.collection("invite_codes").document(code)

    suspend fun generateInviteCode(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

        val code = buildString {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            repeat(6) {
                append(chars.random())
            }
        }

        val now = Timestamp.now()
        val expiresAt = Timestamp(Date(now.toDate().time + 10 * 60 * 1000)) // +10 minutes

        val data = hashMapOf(
            "hostUid" to uid,
            "createdAt" to now,
            "expiresAt" to expiresAt
        )
        inviteCodeDocRef(code).set(data).await()

        return code
    }

    suspend fun redeemInviteCode(code: String): Result<String> {
        val currentUid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val codeSnapshot = inviteCodeDocRef(code).get().await()

            if (!codeSnapshot.exists()) {
                return Result.failure(Exception("Invalid invite code"))
            }

            val hostUid = codeSnapshot.getString("hostUid")
                ?: return Result.failure(Exception("Invalid invite code data"))

            val expiresAt = codeSnapshot.getTimestamp("expiresAt")
                ?: return Result.failure(Exception("Invalid invite code data"))

            if (expiresAt.toDate().before(Date())) {
                // Clean up expired code
                inviteCodeDocRef(code).delete().await()
                return Result.failure(Exception("Invite code has expired"))
            }

            if (hostUid == currentUid) {
                return Result.failure(Exception("Cannot use your own invite code"))
            }

            // Read host profile to get host's email
            val hostProfileSnapshot = profileDocRef(hostUid).get().await()
            val hostEmail = hostProfileSnapshot.getString("email")

            // Read guest (current user) profile to get guest's email
            val guestProfileSnapshot = profileDocRef(currentUid).get().await()
            val guestEmail = guestProfileSnapshot.getString("email")

            // Update host profile: linkedTo = guest uid
            profileDocRef(hostUid).update(
                mapOf("linkedTo" to currentUid)
            ).await()

            // Update guest profile: linkedTo = host uid, dataOwnerUid = host uid
            profileDocRef(currentUid).update(
                mapOf(
                    "linkedTo" to hostUid,
                    "dataOwnerUid" to hostUid
                )
            ).await()

            // Delete the used invite code
            inviteCodeDocRef(code).delete().await()

            Result.success(hostUid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Sharing state ───────────────────────────

    fun sharingState(uid: String): Flow<SharingState> {
        return getProfile(uid).map { profile ->
            if (profile == null || profile.linkedTo == null) {
                SharingState.NotConnected
            } else {
                // Read partner's profile to get their email
                val partnerUid = profile.linkedTo
                val partnerSnapshot = profileDocRef(partnerUid).get().await()
                val partnerEmail = partnerSnapshot.getString("email")
                SharingState.Connected(partnerEmail = partnerEmail)
            }
        }
    }
}
