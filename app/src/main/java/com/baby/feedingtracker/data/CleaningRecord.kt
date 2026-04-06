package com.baby.feedingtracker.data

data class CleaningRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val itemType: String? = null,  // "bottle" | "pot" | "pump" | "other"
    val note: String? = null
)
