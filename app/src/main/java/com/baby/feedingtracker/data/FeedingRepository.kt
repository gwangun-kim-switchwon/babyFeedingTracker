package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class FeedingRepository(private val dataSource: FirestoreDataSource) {
    val allRecords: Flow<List<FeedingRecord>> = dataSource.getAll()
    val latestRecord: Flow<FeedingRecord?> = dataSource.getLatest()

    suspend fun addRecord(): FeedingRecord {
        val timestamp = System.currentTimeMillis()
        val record = FeedingRecord(timestamp = timestamp)
        val id = dataSource.insert(record)
        return record.copy(id = id)
    }

    suspend fun deleteRecord(record: FeedingRecord) {
        dataSource.delete(record.id)
    }

    suspend fun updateRecord(id: String, type: String?, amountMl: Int?, side: String? = null, durationMin: Int? = null) {
        dataSource.updateRecord(id, type, amountMl, side, durationMin)
    }
}
