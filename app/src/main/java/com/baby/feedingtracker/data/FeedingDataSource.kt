package com.baby.feedingtracker.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class FeedingDataSource(
    firestore: FirebaseFirestore,
    uid: String,
    currentUserUid: String = uid
) : BaseDataSource<FeedingRecord>(firestore, uid, currentUserUid) {

    override val collectionName = "feeding_records"

    override fun DocumentSnapshot.toRecord(): FeedingRecord {
        return FeedingRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            type = getString("type"),
            amountMl = getLong("amountMl")?.toInt(),
            leftMin = getLong("leftMin")?.toInt(),
            rightMin = getLong("rightMin")?.toInt(),
            note = getString("note"),
            recordedBy = getString("recordedBy")
        )
    }

    suspend fun insert(record: FeedingRecord): DataResult<String> {
        val data = hashMapOf<String, Any?>(
            "timestamp" to record.timestamp,
            "type" to record.type,
            "amountMl" to record.amountMl,
            "leftMin" to record.leftMin,
            "rightMin" to record.rightMin,
            "note" to record.note,
            "recordedBy" to currentUserUid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        return insert(data)
    }

    suspend fun updateRecord(recordId: String, type: String?, amountMl: Int?, leftMin: Int?, rightMin: Int?): DataResult<Unit> {
        return updateFields(recordId, mapOf(
            "type" to type,
            "amountMl" to amountMl,
            "leftMin" to leftMin,
            "rightMin" to rightMin
        ))
    }
}
