package com.baby.feedingtracker.data

data class CleaningRecord(
    override val id: String = "",
    override val timestamp: Long = 0L,
    val itemType: String? = null,  // "bottle" | "pot" | "pump" | "other"
    override val note: String? = null,
    override val recordedBy: String? = null
) : BaseRecord
