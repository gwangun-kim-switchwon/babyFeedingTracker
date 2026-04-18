package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

open class BaseRepository<T : BaseRecord>(
    protected val dataSource: BaseDataSource<T>
) {
    private val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

    val recentRecords: Flow<List<T>> = dataSource.getRecent(sevenDaysAgo)

    suspend fun loadMore(beforeTimestamp: Long, limit: Long = 20): List<T> {
        return dataSource.loadOlderRecords(beforeTimestamp, limit)
    }

    suspend fun deleteRecord(record: T): DataResult<Unit> {
        return dataSource.delete(record.id)
    }

    suspend fun updateTimestamp(id: String, timestamp: Long): DataResult<Unit> {
        return dataSource.updateTimestamp(id, timestamp)
    }

    suspend fun updateNote(id: String, note: String?): DataResult<Unit> {
        return dataSource.updateNote(id, note)
    }
}
