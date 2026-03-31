package com.baby.feedingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeding_records")
data class FeedingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String? = null,     // "breast" | "formula" | null
    val amountMl: Int? = null      // 분유일 때만: 60, 80, 100, 120, 140, 160
)
