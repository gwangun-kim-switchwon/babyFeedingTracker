package com.baby.feedingtracker.data

data class DiaperRecord(
    override val id: String = "",
    override val timestamp: Long = 0L,
    val type: String? = null,  // "diaper" (기저귀) | "urine" (소변) | "stool" (대변)
    val detail: String? = null, // 소변: 보통/많음/적음, 대변: 노란색·묽음/노란색·보통/녹색/갈색
    override val note: String? = null,
    override val recordedBy: String? = null
) : BaseRecord
