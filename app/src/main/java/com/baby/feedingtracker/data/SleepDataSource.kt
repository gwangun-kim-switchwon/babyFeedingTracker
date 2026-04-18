package com.baby.feedingtracker.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SleepDataSource(
    firestore: FirebaseFirestore,
    uid: String,
    currentUserUid: String = uid
) : BaseDataSource<SleepRecord>(firestore, uid, currentUserUid) {

    override val collectionName = "sleep_records"

    override fun DocumentSnapshot.toRecord(): SleepRecord {
        return SleepRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            endTimestamp = getLong("endTimestamp"),
            type = getString("type"),
            note = getString("note"),
            recordedBy = getString("recordedBy")
        )
    }

    suspend fun insert(record: SleepRecord): DataResult<String> {
        val data = hashMapOf<String, Any?>(
            "timestamp" to record.timestamp,
            "endTimestamp" to record.endTimestamp,
            "type" to record.type,
            "note" to record.note,
            "recordedBy" to currentUserUid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        return insert(data)
    }

    suspend fun updateEndTimestamp(recordId: String, endTimestamp: Long): DataResult<Unit> {
        return updateFields(recordId, mapOf("endTimestamp" to endTimestamp))
    }

    suspend fun updateType(recordId: String, type: String?): DataResult<Unit> {
        return updateFields(recordId, mapOf("type" to type))
    }
}
