package com.baby.feedingtracker.data

class CleaningRepository(
    private val cleaningDataSource: CleaningDataSource
) : BaseRepository<CleaningRecord>(cleaningDataSource) {

    suspend fun addRecord(): DataResult<CleaningRecord> {
        val timestamp = System.currentTimeMillis()
        val record = CleaningRecord(timestamp = timestamp)
        return when (val result = cleaningDataSource.insert(record)) {
            is DataResult.Success -> DataResult.Success(record.copy(id = result.data))
            is DataResult.Error -> DataResult.Error(result.message)
        }
    }

    suspend fun updateItemType(id: String, itemType: String?): DataResult<Unit> {
        return cleaningDataSource.updateItemType(id, itemType)
    }
}
