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

    fun getRecent(sinceTimestamp: Long): Flow<List<DiaperRecord>> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereGreaterThanOrEqualTo("timestamp", sinceTimestamp)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.toDiaperRecord()
                }
            }
    }

    suspend fun loadOlderRecords(beforeTimestamp: Long, limit: Long = 20): List<DiaperRecord> {
        val snapshot = recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereLessThan("timestamp", beforeTimestamp)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.map { it.toDiaperRecord() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toDiaperRecord(): DiaperRecord {
        return DiaperRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            type = getString("type"),
            note = getString("note")
        )
    }

    suspend fun insert(record: DiaperRecord): String {
        val data = hashMapOf(
            "timestamp" to record.timestamp,
            "type" to record.type,
            "note" to record.note,
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

    suspend fun updateNote(recordId: String, note: String?) {
        recordsCollection.document(recordId).update("note", note).await()
    }
}
