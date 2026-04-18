package com.baby.feedingtracker.data

data class FeedingRecord(
    override val id: String = "",
    override val timestamp: Long = 0L,
    val type: String? = null,      // "breast" | "formula" | null
    val amountMl: Int? = null,      // 분유일 때만: 30~160 (10ml 단위)
    val leftMin: Int? = null,       // 모유 왼쪽: 5~15분
    val rightMin: Int? = null,      // 모유 오른쪽: 5~15분
    override val note: String? = null,
    override val recordedBy: String? = null
) : BaseRecord
