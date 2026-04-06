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

    fun getRecent(sinceTimestamp: Long): Flow<List<CleaningRecord>> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereGreaterThanOrEqualTo("timestamp", sinceTimestamp)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.toCleaningRecord()
                }
            }
    }

    suspend fun loadOlderRecords(beforeTimestamp: Long, limit: Long = 20): List<CleaningRecord> {
        val snapshot = recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereLessThan("timestamp", beforeTimestamp)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.map { it.toCleaningRecord() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCleaningRecord(): CleaningRecord {
        return CleaningRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            itemType = getString("itemType"),
            note = getString("note")
        )
    }

    suspend fun insert(record: CleaningRecord): String {
        val data = hashMapOf(
            "timestamp" to record.timestamp,
            "itemType" to record.itemType,
            "note" to record.note,
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

    suspend fun updateNote(recordId: String, note: String?) {
        recordsCollection.document(recordId).update("note", note).await()
    }
}
