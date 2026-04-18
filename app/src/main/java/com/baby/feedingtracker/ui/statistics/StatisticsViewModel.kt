package com.baby.feedingtracker.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.BabyProfileRepository
import com.baby.feedingtracker.data.DailyStats
import com.baby.feedingtracker.data.Milestone
import com.baby.feedingtracker.data.MilestoneManager
import com.baby.feedingtracker.data.PartnerContribution
import com.baby.feedingtracker.data.StatisticsRepository
import com.baby.feedingtracker.data.WeeklyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val todayStats: DailyStats? = null,
    val weeklyStats: WeeklyStats? = null,
    val partnerContribution: PartnerContribution? = null,
    val milestones: List<Milestone> = emptyList(),
    val nextMilestone: Milestone? = null,
    val nextMilestoneRemaining: Int? = null,
    val daysOld: Int? = null,
    val babyName: String? = null
)

class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository,
    private val milestoneManager: MilestoneManager,
    private val babyProfileRepository: BabyProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun refresh() {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val profile = babyProfileRepository.getProfile().firstOrNull()
                val daysOld = if (profile != null && profile.birthDate > 0L) {
                    val diffMillis = System.currentTimeMillis() - profile.birthDate
                    TimeUnit.MILLISECONDS.toDays(diffMillis).toInt() + 1
                } else null
                val babyName = profile?.name?.takeIf { it.isNotBlank() }

                val todayStats = statisticsRepository.getTodayStats()
                val weeklyStats = statisticsRepository.getWeeklyStats()
                val milestones = milestoneManager.checkMilestones()
                val nextMilestone = milestoneManager.getNextMilestone()

                // Calculate remaining for next milestone
                val nextMilestoneRemaining = calculateRemaining(nextMilestone)

                // Partner contribution for this week
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endDate = calendar.timeInMillis + 24 * 60 * 60 * 1000L
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val startDate = calendar.timeInMillis

                val partnerContribution = statisticsRepository.getPartnerContribution(startDate, endDate)

                _uiState.value = StatisticsUiState(
                    isLoading = false,
                    todayStats = todayStats,
                    weeklyStats = weeklyStats,
                    partnerContribution = partnerContribution,
                    milestones = milestones,
                    nextMilestone = nextMilestone,
                    nextMilestoneRemaining = nextMilestoneRemaining,
                    daysOld = daysOld,
                    babyName = babyName
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun calculateRemaining(nextMilestone: Milestone?): Int? {
        if (nextMilestone == null) return null
        val feedingCount = statisticsRepository.getTotalFeedingCount()
        val diaperCount = statisticsRepository.getTotalDiaperCount()
        val profile = babyProfileRepository.getProfile().firstOrNull()
        val daysOld = if (profile != null && profile.birthDate > 0L) {
            val diffMillis = System.currentTimeMillis() - profile.birthDate
            TimeUnit.MILLISECONDS.toDays(diffMillis).toInt() + 1
        } else null

        return when {
            nextMilestone.title.contains("수유") -> {
                val threshold = nextMilestone.title.filter { it.isDigit() }.toIntOrNull() ?: return null
                threshold - feedingCount
            }
            nextMilestone.title.contains("기저귀") -> {
                val threshold = nextMilestone.title.filter { it.isDigit() }.toIntOrNull() ?: return null
                threshold - diaperCount
            }
            nextMilestone.title.contains("생후") || nextMilestone.title.contains("1년") -> {
                val threshold = when {
                    nextMilestone.title.contains("1년") -> 365
                    else -> nextMilestone.title.filter { it.isDigit() }.toIntOrNull() ?: return null
                }
                if (daysOld != null) threshold - daysOld else null
            }
            else -> null
        }
    }

    companion object {
        fun factory(
            statisticsRepository: StatisticsRepository,
            milestoneManager: MilestoneManager,
            babyProfileRepository: BabyProfileRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatisticsViewModel(
                        statisticsRepository,
                        milestoneManager,
                        babyProfileRepository
                    ) as T
                }
            }
        }
    }
}
