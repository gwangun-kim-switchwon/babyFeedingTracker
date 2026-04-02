package com.baby.feedingtracker.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DiaperDataSource(
    private val firestore: FirebaseFirestore,
    private val uid: String
) {
    private val recordsCollection
        get() = firestore.collection("users").document(uid).collection("diaper_records")

    fun getAll(): Flow<List<DiaperRecord>> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.toDiaperRecord()
                }
            }
    }

    fun getLatest(): Flow<DiaperRecord?> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.toDiaperRecord()
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toDiaperRecord(): DiaperRecord {
        return DiaperRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            type = getString("type")
        )
    }

    suspend fun insert(record: DiaperRecord): String {
        val data = hashMapOf(
            "timestamp" to record.timestamp,
            "type" to record.type,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        val docRef = recordsCollection.add(data).await()
        return docRef.id
    }

    suspend fun delete(recordId: String) {
        recordsCollection.document(recordId).delete().await()
    }

    suspend fun updateType(recordId: String, type: String?) {
        recordsCollection.document(recordId).update(
            mapOf("type" to type)
        ).await()
    }

    suspend fun updateTimestamp(recordId: String, timestamp: Long) {
        recordsCollection.document(recordId).update(
            mapOf("timestamp" to timestamp)
        ).await()
    }
}
