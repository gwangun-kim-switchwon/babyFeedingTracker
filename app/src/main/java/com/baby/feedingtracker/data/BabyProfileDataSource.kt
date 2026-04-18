package com.baby.feedingtracker.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BabyProfileDataSource(
    private val firestore: FirebaseFirestore,
    private val uid: String
) {
    private val docRef get() = firestore.collection("users").document(uid)
        .collection("profile").document("baby")

    fun getProfile(): Flow<BabyProfile?> = callbackFlow {
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = BabyProfile(
                    name = snapshot.getString("name") ?: "",
                    birthDate = snapshot.getLong("birthDate") ?: 0L,
                    gender = snapshot.getString("gender")
                )
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveProfile(profile: BabyProfile) {
        val data = hashMapOf<String, Any?>(
            "name" to profile.name,
            "birthDate" to profile.birthDate,
            "gender" to profile.gender
        )
        docRef.set(data).await()
    }
}
