package com.baby.feedingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedingDao {
    @Query("SELECT * FROM feeding_records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FeedingRecord>>

    @Query("SELECT * FROM feeding_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<FeedingRecord?>

    @Insert
    suspend fun insert(record: FeedingRecord)

    @Delete
    suspend fun delete(record: FeedingRecord)

    @Query("UPDATE feeding_records SET type = :type, amountMl = :amountMl WHERE id = :id")
    suspend fun updateTypeAndAmount(id: Long, type: String?, amountMl: Int?)
}
