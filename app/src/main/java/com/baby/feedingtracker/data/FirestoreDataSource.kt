package com.baby.feedingtracker.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreDataSource(
    private val firestore: FirebaseFirestore,
    private val uid: String
) {
    private val recordsCollection
        get() = firestore.collection("users").document(uid).collection("feeding_records")

    fun getAll(): Flow<List<FeedingRecord>> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.toFeedingRecord()
                }
            }
    }

    fun getLatest(): Flow<FeedingRecord?> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.toFeedingRecord()
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFeedingRecord(): FeedingRecord {
        return FeedingRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            type = getString("type"),
            amountMl = getLong("amountMl")?.toInt(),
            leftMin = getLong("leftMin")?.toInt(),
            rightMin = getLong("rightMin")?.toInt()
        )
    }

    suspend fun insert(record: FeedingRecord): String {
        val data = hashMapOf(
            "timestamp" to record.timestamp,
            "type" to record.type,
            "amountMl" to record.amountMl,
            "leftMin" to record.leftMin,
            "rightMin" to record.rightMin,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        val docRef = recordsCollection.add(data).await()
        return docRef.id
    }

    suspend fun delete(recordId: String) {
        recordsCollection.document(recordId).delete().await()
    }

    suspend fun updateTimestamp(recordId: String, timestamp: Long) {
        recordsCollection.document(recordId).update("timestamp", timestamp).await()
    }

    suspend fun updateRecord(recordId: String, type: String?, amountMl: Int?, leftMin: Int?, rightMin: Int?) {
        recordsCollection.document(recordId).update(
            mapOf(
                "type" to type,
                "amountMl" to amountMl,
                "leftMin" to leftMin,
                "rightMin" to rightMin
            )
        ).await()
    }
}
