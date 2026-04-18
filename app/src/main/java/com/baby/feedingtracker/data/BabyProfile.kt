package com.baby.feedingtracker.data

data class BabyProfile(
    val name: String = "",
    val birthDate: Long = 0L,  // epoch millis
    val gender: String? = null  // "male" / "female" / null
)
