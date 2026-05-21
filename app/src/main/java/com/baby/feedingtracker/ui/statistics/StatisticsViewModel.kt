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

    // 일일 통계용 선택 날짜 (해당 날 자정 epoch ms)
    private val _selectedDayStart = MutableStateFlow(todayStartMs())
    val selectedDayStart: StateFlow<Long> = _selectedDayStart.asStateFlow()

    // 일일 통계 항목 리스트
    private val _dailyEntries = MutableStateFlow<List<DailyEntry>>(emptyList())
    val dailyEntries: StateFlow<List<DailyEntry>> = _dailyEntries.asStateFlow()

    private val _isDailyLoading = MutableStateFlow(false)
    val isDailyLoading: StateFlow<Boolean> = _isDailyLoading.asStateFlow()

    init {
        loadStatistics()
        viewModelScope.launch {
            _selectedDayStart.collect { dayStart ->
                loadDailyEntries(dayStart)
            }
        }
    }

    fun refresh() {
        loadStatistics()
    }

    fun previousDay() { _selectedDayStart.value -= 86_400_000L }
    fun nextDay()     { _selectedDayStart.value += 86_400_000L }

    private fun todayStartMs(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private suspend fun loadDailyEntries(dayStart: Long) {
        val dayEnd = dayStart + 86_400_000L
        _isDailyLoading.value = true
        _dailyEntries.value = emptyList()
        try {
            val feedings = statisticsRepository.getFeedingRecords(dayStart, dayEnd)
            val diapers  = statisticsRepository.getDiaperRecords(dayStart, dayEnd)
            val sleeps   = statisticsRepository.getSleepRecords(dayStart, dayEnd)

            val entries = mutableListOf<DailyEntry>()

            feedings.forEach { r ->
                val typeLabel = when (r.type) {
                    "breast"  -> "모유"
                    "formula" -> "분유"
                    "pumped"  -> "유축"
                    else      -> "수유"
                }
                val detail = buildString {
                    append(typeLabel)
                    r.amountMl?.let { append(" ${it}ml") }
                }
                entries += DailyEntry(r.timestamp, DailyEntryType.FEEDING, detail, "🍼")
            }

            diapers.forEach { r ->
                val typeLabel = when (r.type) {
                    "urine"  -> "소변"
                    "stool"  -> "대변"
                    "diaper" -> "기저귀"
                    else     -> "기저귀"
                }
                entries += DailyEntry(r.timestamp, DailyEntryType.DIAPER, typeLabel, "💧")
            }

            sleeps.forEach { r ->
                val durationLabel = r.endTimestamp?.let { end ->
                    val mins = ((end - r.timestamp) / 60_000L).toInt()
                    if (mins >= 60) "${mins / 60}시간 ${mins % 60}분" else "${mins}분"
                }
                val detail = buildString {
                    append(if (r.type == "nap") "낮잠" else "밤잠")
                    durationLabel?.let { append(" · $it") }
                }
                entries += DailyEntry(r.timestamp, DailyEntryType.SLEEP, detail, "💤")
            }

            _dailyEntries.value = entries.sortedBy { it.timestamp }
        } catch (_: Exception) {
            _dailyEntries.value = emptyList()
        } finally {
            _isDailyLoading.value = false
        }
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
