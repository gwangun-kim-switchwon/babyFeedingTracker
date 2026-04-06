package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class FeedingRepository(private val dataSource: FirestoreDataSource) {
    private val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

    val recentRecords: Flow<List<FeedingRecord>> = dataSource.getRecent(sevenDaysAgo)

    suspend fun loadMore(beforeTimestamp: Long, limit: Long = 20): List<FeedingRecord> {
        return dataSource.loadOlderRecords(beforeTimestamp, limit)
    }

    suspend fun addRecord(): FeedingRecord {
        val timestamp = System.currentTimeMillis()
        val record = FeedingRecord(timestamp = timestamp)
        val id = dataSource.insert(record)
        return record.copy(id = id)
    }

    suspend fun deleteRecord(record: FeedingRecord) {
        dataSource.delete(record.id)
    }

    suspend fun updateTimestamp(id: String, timestamp: Long) {
        dataSource.updateTimestamp(id, timestamp)
    }

    suspend fun updateRecord(id: String, type: String?, amountMl: Int?, leftMin: Int? = null, rightMin: Int? = null) {
        dataSource.updateRecord(id, type, amountMl, leftMin, rightMin)
    }

    suspend fun updateNote(id: String, note: String?) = dataSource.updateNote(id, note)
}
