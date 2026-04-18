package com.baby.feedingtracker.data

class DiaperRepository(
    private val diaperDataSource: DiaperDataSource
) : BaseRepository<DiaperRecord>(diaperDataSource) {

    suspend fun addRecord(): DataResult<DiaperRecord> {
        val timestamp = System.currentTimeMillis()
        val record = DiaperRecord(timestamp = timestamp)
        return when (val result = diaperDataSource.insert(record)) {
            is DataResult.Success -> DataResult.Success(record.copy(id = result.data))
            is DataResult.Error -> DataResult.Error(result.message)
        }
    }

    suspend fun updateType(id: String, type: String?): DataResult<Unit> {
        return diaperDataSource.updateType(id, type)
    }
}
