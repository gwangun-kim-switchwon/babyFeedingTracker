package com.baby.feedingtracker.data

class SleepRepository(
    private val sleepDataSource: SleepDataSource
) : BaseRepository<SleepRecord>(sleepDataSource) {

    suspend fun addSleepRecord(): DataResult<SleepRecord> {
        val timestamp = System.currentTimeMillis()
        val record = SleepRecord(timestamp = timestamp)
        return when (val result = sleepDataSource.insert(record)) {
            is DataResult.Success -> DataResult.Success(record.copy(id = result.data))
            is DataResult.Error -> DataResult.Error(result.message)
        }
    }

    suspend fun endSleep(recordId: String): DataResult<Unit> {
        val endTimestamp = System.currentTimeMillis()
        return sleepDataSource.updateEndTimestamp(recordId, endTimestamp)
    }

    suspend fun updateType(id: String, type: String?): DataResult<Unit> {
        return sleepDataSource.updateType(id, type)
    }
}
