package com.baby.feedingtracker.data

data class GrowthRecord(
    override val id: String = "",
    override val timestamp: Long = System.currentTimeMillis(),
    override val note: String? = null,
    override val recordedBy: String? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val headCm: Float? = null,
) : BaseRecord {
    val hasAnyMeasurement: Boolean
        get() = heightCm != null || weightKg != null || headCm != null
}
