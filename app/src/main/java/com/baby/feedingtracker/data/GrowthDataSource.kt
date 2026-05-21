package com.baby.feedingtracker.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class GrowthDataSource(
    firestore: FirebaseFirestore,
    uid: String,
    currentUserUid: String = uid
) : BaseDataSource<GrowthRecord>(firestore, uid, currentUserUid) {

    override val collectionName = "growth_records"

    override fun DocumentSnapshot.toRecord(): GrowthRecord {
        return GrowthRecord(
            id = id,
            timestamp = getLong("timestamp") ?: 0L,
            note = getString("note"),
            recordedBy = getString("recordedBy"),
            heightCm = getDouble("heightCm")?.toFloat(),
            weightKg = getDouble("weightKg")?.toFloat(),
            headCm = getDouble("headCm")?.toFloat(),
        )
    }

    suspend fun insert(record: GrowthRecord): DataResult<String> {
        val data = hashMapOf<String, Any?>(
            "timestamp" to record.timestamp,
            "note" to record.note,
            "recordedBy" to currentUserUid,
            "heightCm" to record.heightCm,
            "weightKg" to record.weightKg,
            "headCm" to record.headCm,
            "createdAt" to Timestamp.now()
        )
        return insert(data)
    }
}
