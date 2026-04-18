package com.baby.feedingtracker.data

data class DiaperRecord(
    override val id: String = "",
    override val timestamp: Long = 0L,
    val type: String? = null,  // "diaper" (기저귀) | "urine" (소변) | "stool" (대변)
    override val note: String? = null,
    override val recordedBy: String? = null
) : BaseRecord
