package com.baby.feedingtracker.data

data class SleepRecord(
    override val id: String = "",
    override val timestamp: Long = 0L,          // 수면 시작
    val endTimestamp: Long? = null,     // 수면 종료 (null = 진행 중)
    val type: String? = null,           // "nap" | "night"
    override val note: String? = null,
    override val recordedBy: String? = null
) : BaseRecord
