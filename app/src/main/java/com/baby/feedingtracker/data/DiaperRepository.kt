package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class DiaperRepository(private val dataSource: DiaperDataSource) {
    val allRecords: Flow<List<DiaperRecord>> = dataSource.getAll()
    val latestRecord: Flow<DiaperRecord?> = dataSource.getLatest()

    suspend fun addRecord(): DiaperRecord {
        val timestamp = System.currentTimeMillis()
        val record = DiaperRecord(timestamp = timestamp)
        val id = dataSource.insert(record)
        return record.copy(id = id)
    }

    suspend fun deleteRecord(record: DiaperRecord) {
        dataSource.delete(record.id)
    }

    suspend fun updateType(id: String, type: String?) {
        dataSource.updateType(id, type)
    }

    suspend fun updateTimestamp(id: String, timestamp: Long) {
        dataSource.updateTimestamp(id, timestamp)
    }
}
