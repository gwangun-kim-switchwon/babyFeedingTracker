package com.baby.feedingtracker.data

class GrowthRepository(
    private val growthDataSource: GrowthDataSource
) : BaseRepository<GrowthRecord>(growthDataSource) {

    suspend fun addGrowthRecord(record: GrowthRecord): DataResult<GrowthRecord> {
        return when (val result = growthDataSource.insert(record)) {
            is DataResult.Success -> DataResult.Success(record.copy(id = result.data))
            is DataResult.Error   -> DataResult.Error(result.message)
        }
    }
}
