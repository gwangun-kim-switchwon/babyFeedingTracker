package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class CleaningRepository(private val dataSource: CleaningDataSource) {
    val allRecords: Flow<List<CleaningRecord>> = dataSource.getAll()
    val latestRecord: Flow<CleaningRecord?> = dataSource.getLatest()

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
}
