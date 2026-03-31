package com.baby.feedingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeding_records")
data class FeedingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long
)
