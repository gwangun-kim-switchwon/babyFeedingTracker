package com.baby.feedingtracker.data

data class FeedingRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val type: String? = null,      // "breast" | "formula" | null
    val amountMl: Int? = null       // 분유일 때만: 60, 80, 100, 120, 140, 160
)
