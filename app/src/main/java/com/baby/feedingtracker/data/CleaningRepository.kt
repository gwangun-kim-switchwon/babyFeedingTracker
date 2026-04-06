package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class CleaningRepository(private val dataSource: CleaningDataSource) {
    private val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

    val recentRecords: Flow<List<CleaningRecord>> = dataSource.getRecent(sevenDaysAgo)

    suspend fun loadMore(beforeTimestamp: Long, limit: Long = 20): List<CleaningRecord> {
        return dataSource.loadOlderRecords(beforeTimestamp, limit)
    }

    suspend fun addRecord(): CleaningRecord {
        val timestamp = System.currentTimeMillis()
        val record = CleaningRecord(timestamp = timestamp)
        val id = dataSource.insert(record)
        return record.copy(id = id)
    }

    suspend fun deleteRecord(record: CleaningRecord) {
        dataSource.delete(record.id)
    }

    suspend fun updateItemType(id: String, itemType: String?) {
        dataSource.updateItemType(id, itemType)
    }

    suspend fun updateTimestamp(id: String, timestamp: Long) {
        dataSource.updateTimestamp(id, timestamp)
    }

    suspend fun updateNote(id: String, note: String?) = dataSource.updateNote(id, note)
}
