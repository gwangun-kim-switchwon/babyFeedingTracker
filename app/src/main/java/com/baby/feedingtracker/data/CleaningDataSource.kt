package com.baby.feedingtracker.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CleaningDataSource(
    private val firestore: FirebaseFirestore,
    private val uid: String
) {
    private val recordsCollection
        get() = firestore.collection("users").document(uid).collection("cleaning_records")

    fun getAll(): Flow<List<CleaningRecord>> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.toCleaningRecord()
                }
            }
    }

    fun getLatest(): Flow<CleaningRecord?> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.toCleaningRecord()
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCleaningRecord(): CleaningRecord {
        return CleaningRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            itemType = getString("itemType")
        )
    }

    suspend fun insert(record: CleaningRecord): String {
        val data = hashMapOf(
            "timestamp" to record.timestamp,
            "itemType" to record.itemType,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        val docRef = recordsCollection.add(data).await()
        return docRef.id
    }

    suspend fun delete(recordId: String) {
        recordsCollection.document(recordId).delete().await()
    }

    suspend fun updateItemType(recordId: String, itemType: String?) {
        recordsCollection.document(recordId).update(
            mapOf("itemType" to itemType)
        ).await()
    }

    suspend fun updateTimestamp(recordId: String, timestamp: Long) {
        recordsCollection.document(recordId).update(
            mapOf("timestamp" to timestamp)
        ).await()
    }
}
