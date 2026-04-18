package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class BabyProfileRepository(private val dataSource: BabyProfileDataSource) {

    fun getProfile(): Flow<BabyProfile?> = dataSource.getProfile()

    fun getDaysOld(): Flow<Int?> = dataSource.getProfile().map { profile ->
        if (profile != null && profile.birthDate > 0L) {
            val now = System.currentTimeMillis()
            val diffMillis = now - profile.birthDate
            TimeUnit.MILLISECONDS.toDays(diffMillis).toInt() + 1  // 생후 1일부터 시작
        } else {
            null
        }
    }

    suspend fun saveProfile(profile: BabyProfile) {
        dataSource.saveProfile(profile)
    }
}
