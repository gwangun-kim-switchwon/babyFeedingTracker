package com.baby.feedingtracker.data

class FeedingRepository(
    private val feedingDataSource: FeedingDataSource
) : BaseRepository<FeedingRecord>(feedingDataSource) {

    suspend fun addRecord(): DataResult<FeedingRecord> {
        val timestamp = System.currentTimeMillis()
        val record = FeedingRecord(timestamp = timestamp)
        return when (val result = feedingDataSource.insert(record)) {
            is DataResult.Success -> DataResult.Success(record.copy(id = result.data))
            is DataResult.Error -> DataResult.Error(result.message)
        }
    }

    suspend fun updateRecord(id: String, type: String?, amountMl: Int?, leftMin: Int? = null, rightMin: Int? = null): DataResult<Unit> {
        return feedingDataSource.updateRecord(id, type, amountMl, leftMin, rightMin)
    }
}
