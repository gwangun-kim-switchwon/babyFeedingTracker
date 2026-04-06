package com.baby.feedingtracker.data

data class DiaperRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val type: String? = null,  // "diaper" (기저귀) | "urine" (소변) | "stool" (대변)
    val note: String? = null
)
