package com.baby.feedingtracker.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class CleaningDataSource(
    firestore: FirebaseFirestore,
    uid: String,
    currentUserUid: String = uid
) : BaseDataSource<CleaningRecord>(firestore, uid, currentUserUid) {

    override val collectionName = "cleaning_records"

    override fun DocumentSnapshot.toRecord(): CleaningRecord {
        return CleaningRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            itemType = getString("itemType"),
            note = getString("note"),
            recordedBy = getString("recordedBy")
        )
    }

    suspend fun insert(record: CleaningRecord): DataResult<String> {
        val data = hashMapOf<String, Any?>(
            "timestamp" to record.timestamp,
            "itemType" to record.itemType,
            "note" to record.note,
            "recordedBy" to currentUserUid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        return insert(data)
    }

    suspend fun updateItemType(recordId: String, itemType: String?): DataResult<Unit> {
        return updateFields(recordId, mapOf("itemType" to itemType))
    }
}
