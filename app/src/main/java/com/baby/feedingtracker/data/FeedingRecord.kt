package com.baby.feedingtracker.data

data class FeedingRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val type: String? = null,      // "breast" | "formula" | null
    val amountMl: Int? = null,      // 분유일 때만: 30~160 (10ml 단위)
    val side: String? = null,       // 모유일 때: "left" | "right" | null
    val durationMin: Int? = null    // 모유일 때: 5~15분
)
