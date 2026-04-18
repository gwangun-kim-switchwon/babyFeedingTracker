package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

data class Milestone(
    val type: MilestoneType,
    val title: String,
    val description: String,
    val achievedAt: Long? = null
)

enum class MilestoneType {
    DAYS_50, DAYS_100, DAYS_200, DAYS_365,
    FEEDING_100, FEEDING_500, FEEDING_1000,
    DIAPER_100, DIAPER_500
}

class MilestoneManager(
    private val statisticsRepository: StatisticsRepository,
    private val babyProfileRepository: BabyProfileRepository
) {
    private val milestoneDefinitions = listOf(
        MilestoneDefinition(MilestoneType.DAYS_50, 50, "생후 50일", "우리 아기 생후 50일을 맞이했어요!", MilestoneCategory.DAYS),
        MilestoneDefinition(MilestoneType.DAYS_100, 100, "생후 100일", "우리 아기 생후 100일을 맞이했어요!", MilestoneCategory.DAYS),
        MilestoneDefinition(MilestoneType.DAYS_200, 200, "생후 200일", "우리 아기 생후 200일을 맞이했어요!", MilestoneCategory.DAYS),
        MilestoneDefinition(MilestoneType.DAYS_365, 365, "생후 1년", "우리 아기 첫 번째 생일이에요!", MilestoneCategory.DAYS),
        MilestoneDefinition(MilestoneType.FEEDING_100, 100, "수유 100회", "수유 100회를 달성했어요!", MilestoneCategory.FEEDING),
        MilestoneDefinition(MilestoneType.FEEDING_500, 500, "수유 500회", "수유 500회를 달성했어요!", MilestoneCategory.FEEDING),
        MilestoneDefinition(MilestoneType.FEEDING_1000, 1000, "수유 1000회", "수유 1000회를 달성했어요!", MilestoneCategory.FEEDING),
        MilestoneDefinition(MilestoneType.DIAPER_100, 100, "기저귀 100회", "기저귀 교체 100회를 달성했어요!", MilestoneCategory.DIAPER),
        MilestoneDefinition(MilestoneType.DIAPER_500, 500, "기저귀 500회", "기저귀 교체 500회를 달성했어요!", MilestoneCategory.DIAPER)
    )

    suspend fun checkMilestones(): List<Milestone> {
        val daysOld = getDaysOld()
        val feedingCount = statisticsRepository.getTotalFeedingCount()
        val diaperCount = statisticsRepository.getTotalDiaperCount()
        val now = System.currentTimeMillis()

        return milestoneDefinitions.mapNotNull { def ->
            val currentValue = when (def.category) {
                MilestoneCategory.DAYS -> daysOld
                MilestoneCategory.FEEDING -> feedingCount
                MilestoneCategory.DIAPER -> diaperCount
            }
            if (currentValue != null && currentValue >= def.threshold) {
                Milestone(
                    type = def.type,
                    title = def.title,
                    description = def.description,
                    achievedAt = now
                )
            } else {
                null
            }
        }
    }

    suspend fun getNextMilestone(): Milestone? {
        val daysOld = getDaysOld()
        val feedingCount = statisticsRepository.getTotalFeedingCount()
        val diaperCount = statisticsRepository.getTotalDiaperCount()

        return milestoneDefinitions.firstOrNull { def ->
            val currentValue = when (def.category) {
                MilestoneCategory.DAYS -> daysOld
                MilestoneCategory.FEEDING -> feedingCount
                MilestoneCategory.DIAPER -> diaperCount
            }
            currentValue == null || currentValue < def.threshold
        }?.let { def ->
            Milestone(
                type = def.type,
                title = def.title,
                description = def.description,
                achievedAt = null
            )
        }
    }

    private suspend fun getDaysOld(): Int? {
        val profile = babyProfileRepository.getProfile().firstOrNull()
        return if (profile != null && profile.birthDate > 0L) {
            val diffMillis = System.currentTimeMillis() - profile.birthDate
            TimeUnit.MILLISECONDS.toDays(diffMillis).toInt() + 1
        } else {
            null
        }
    }

    private data class MilestoneDefinition(
        val type: MilestoneType,
        val threshold: Int,
        val title: String,
        val description: String,
        val category: MilestoneCategory
    )

    private enum class MilestoneCategory {
        DAYS, FEEDING, DIAPER
    }
}
