package com.baby.feedingtracker.data

data class SleepRecord(
    val id: String = "",
    val timestamp: Long = 0L,          // 수면 시작
    val endTimestamp: Long? = null,     // 수면 종료 (null = 진행 중)
    val type: String? = null,           // "nap" | "night"
    val note: String? = null
)
