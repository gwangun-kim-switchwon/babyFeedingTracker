package com.baby.feedingtracker.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class DiaperDataSource(
    firestore: FirebaseFirestore,
    uid: String,
    currentUserUid: String = uid
) : BaseDataSource<DiaperRecord>(firestore, uid, currentUserUid) {

    override val collectionName = "diaper_records"

    override fun DocumentSnapshot.toRecord(): DiaperRecord {
        return DiaperRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            type = getString("type"),
            note = getString("note"),
            recordedBy = getString("recordedBy")
        )
    }

    suspend fun insert(record: DiaperRecord): DataResult<String> {
        val data = hashMapOf<String, Any?>(
            "timestamp" to record.timestamp,
            "type" to record.type,
            "note" to record.note,
            "recordedBy" to currentUserUid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        return insert(data)
    }

    suspend fun updateType(recordId: String, type: String?): DataResult<Unit> {
        return updateFields(recordId, mapOf("type" to type))
    }
}
