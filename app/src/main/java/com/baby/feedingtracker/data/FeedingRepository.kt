package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class FeedingRepository(private val dao: FeedingDao) {
    val allRecords: Flow<List<FeedingRecord>> = dao.getAll()
    val latestRecord: Flow<FeedingRecord?> = dao.getLatest()

    suspend fun addRecord(): FeedingRecord {
        val timestamp = System.currentTimeMillis()
        val record = FeedingRecord(timestamp = timestamp)
        val id = dao.insert(record)
        return record.copy(id = id)
    }

    suspend fun deleteRecord(record: FeedingRecord) {
        dao.delete(record)
    }

    suspend fun updateRecord(id: Long, type: String?, amountMl: Int?) {
        dao.updateTypeAndAmount(id, type, amountMl)
    }
}
