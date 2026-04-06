package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class SleepRepository(private val dataSource: SleepDataSource) {
    private val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

    val recentRecords: Flow<List<SleepRecord>> = dataSource.getRecent(sevenDaysAgo)

    suspend fun loadMore(beforeTimestamp: Long, limit: Long = 20): List<SleepRecord> {
        return dataSource.loadOlderRecords(beforeTimestamp, limit)
    }

    suspend fun addSleepRecord(): SleepRecord {
        val timestamp = System.currentTimeMillis()
        val record = SleepRecord(timestamp = timestamp)
        val id = dataSource.insert(record)
        return record.copy(id = id)
    }

    suspend fun endSleep(recordId: String) {
        val endTimestamp = System.currentTimeMillis()
        dataSource.updateEndTimestamp(recordId, endTimestamp)
    }

    suspend fun deleteRecord(record: SleepRecord) {
        dataSource.delete(record.id)
    }

    suspend fun updateType(id: String, type: String?) {
        dataSource.updateType(id, type)
    }

    suspend fun updateTimestamp(id: String, timestamp: Long) {
        dataSource.updateTimestamp(id, timestamp)
    }

    suspend fun updateNote(id: String, note: String?) = dataSource.updateNote(id, note)
}
