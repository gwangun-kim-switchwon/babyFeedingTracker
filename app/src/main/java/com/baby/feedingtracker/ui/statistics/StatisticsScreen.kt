package com.baby.feedingtracker.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.DailyStats
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.statistics.components.DailyDateNavCard
import com.baby.feedingtracker.ui.statistics.components.DailyEntryRecordCard
import com.baby.feedingtracker.ui.statistics.components.DailySectionHeader
import com.baby.feedingtracker.ui.statistics.components.StatNumCardRow
import com.baby.feedingtracker.ui.statistics.components.StatNumItem
import com.baby.feedingtracker.ui.statistics.components.StatsTextHeader
import com.baby.feedingtracker.ui.statistics.components.TodaySummaryCoralCard
import com.baby.feedingtracker.ui.statistics.components.VerticalBarChartCard
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    babyProfileViewModel: BabyProfileViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDayStart by viewModel.selectedDayStart.collectAsStateWithLifecycle()
    val dailyEntries by viewModel.dailyEntries.collectAsStateWithLifecycle()
    val isDailyLoading by viewModel.isDailyLoading.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }  // 0=주간, 1=일일

    val ext = LocalExtendedColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // ── 텍스트 헤더 ──
                item {
                    StatsTextHeader(
                        dateRangeText = weeklyDateRangeText(uiState.weeklyStats?.startDate, uiState.weeklyStats?.endDate)
                    )
                }

                // ── 주간/일일 탭 스위처 ──
                item {
                    StatTabSwitcher(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                if (selectedTab == 0) {
                    // ── 주간 탭 ──
                    val todayStats = uiState.todayStats
                    val weeklyStats = uiState.weeklyStats

                    // 1) 오늘의 요약 Coral 카드
                    item {
                        TodaySummaryCoralCard(
                            feedingCount = todayStats?.feedingCount ?: 0,
                            sleepMinutes = todayStats?.sleepTotalMinutes ?: 0L,
                            diaperCount = todayStats?.diaperCount ?: 0,
                            todayDateText = todayDateText(),
                        )
                    }

                    if (weeklyStats != null && weeklyStats.dailyStats.isNotEmpty()) {
                        val dailyStats = weeklyStats.dailyStats
                        val todayIdx = todayIndex(dailyStats)
                        val dayLabels = dailyStats.map { dayLabelOf(it.date) }

                        // 2) 수유 주간 차트
                        item {
                            VerticalBarChartCard(
                                title = "수유 횟수 — 이번 주",
                                dailyValues = dailyStats.map { it.feedingCount.toFloat() },
                                dayLabels = dayLabels,
                                todayIndex = todayIdx,
                                barColor = ext.categoryFeeding,
                                barColorDark = ext.coralAccent,
                                valueFormatter = { v -> v.toInt().toString() },
                            )
                        }

                        // 3) 숫자 카드 Row (오늘 수면 / 오늘 기저귀 / 주간 평균 수유)
                        val avgFeeding = weeklyStats.totalFeedings.toFloat() / dailyStats.size
                        item {
                            StatNumCardRow(
                                items = listOf(
                                    StatNumItem(
                                        emoji = "🌙",
                                        value = formatSleep(todayStats?.sleepTotalMinutes ?: 0L),
                                        label = "오늘 수면",
                                    ),
                                    StatNumItem(
                                        emoji = "💧",
                                        value = "${todayStats?.diaperCount ?: 0}회",
                                        label = "오늘 기저귀",
                                    ),
                                    StatNumItem(
                                        emoji = "📊",
                                        value = "${"%.1f".format(avgFeeding)}회",
                                        label = "주간 평균 수유",
                                    ),
                                )
                            )
                        }

                        // 4) 수면 주간 차트 — 시간 단위
                        item {
                            VerticalBarChartCard(
                                title = "수면 시간 — 이번 주 (시간)",
                                dailyValues = dailyStats.map { it.sleepTotalMinutes / 60f },
                                dayLabels = dayLabels,
                                todayIndex = todayIdx,
                                barColor = ext.categorySleep,
                                barColorDark = ext.categorySleep.copy(alpha = 1f).darken(0.18f),
                                valueFormatter = { v -> "%.1f".format(v) },
                            )
                        }
                    }
                } else {
                    // ── 일일 탭 ──
                    item {
                        DailyDateNavCard(
                            dayStartMs = selectedDayStart,
                            onPrevious = { viewModel.previousDay() },
                            onNext = { viewModel.nextDay() },
                        )
                    }

                    // 일일 숫자 카드 Row — 그날의 통계 (selected day stats가 별도 없어 dailyEntries에서 집계)
                    item {
                        val feeds = dailyEntries.count { it.type == DailyEntryType.FEEDING }
                        val diapers = dailyEntries.count { it.type == DailyEntryType.DIAPER }
                        val sleepMins = dailySleepMinutes(dailyEntries)
                        StatNumCardRow(
                            items = listOf(
                                StatNumItem("🍼", "${feeds}회", "수유"),
                                StatNumItem("🌙", formatSleep(sleepMins), "수면"),
                                StatNumItem("💧", "${diapers}회", "기저귀"),
                            )
                        )
                    }

                    item { DailySectionHeader(title = "시간대별 기록") }

                    if (isDailyLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (dailyEntries.isEmpty()) {
                        item {
                            Text(
                                text = "선택한 날짜에 기록이 없어요",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalExtendedColors.current.subtleText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 24.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        // 시간 역순 (최신 위)
                        val sortedDesc = dailyEntries.sortedByDescending { it.timestamp }
                        items(sortedDesc, key = { it.timestamp.toString() + it.type }) { entry ->
                            DailyEntryRecordCard(entry)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Statistics Tab Switcher (디자이너 원안: #F4F0F8 배경 + active White pill + shadow)
// ──────────────────────────────────────────────

@Composable
private fun StatTabSwitcher(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        listOf("주간", "일일").forEachIndexed { index, label ->
            val isActive = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isActive)
                            Modifier.shadow(elevation = 1.dp, shape = RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        LocalExtendedColors.current.subtleText,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 헬퍼 함수들
// ──────────────────────────────────────────────

private fun weeklyDateRangeText(startMs: Long?, endMs: Long?): String {
    if (startMs == null || endMs == null) return ""
    val startFmt = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
    val endFmt = SimpleDateFormat("d일", Locale.KOREAN)
    // endDate는 마지막 날 자정 직전 + 1d 형태이므로 -1ms 보정
    val endDisplay = Date(endMs - 1)
    return "${startFmt.format(Date(startMs))} — ${endFmt.format(endDisplay)}"
}

private fun todayDateText(): String {
    val fmt = SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN)
    return fmt.format(Date())
}

private fun dayLabelOf(dateMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "월"
        Calendar.TUESDAY -> "화"
        Calendar.WEDNESDAY -> "수"
        Calendar.THURSDAY -> "목"
        Calendar.FRIDAY -> "금"
        Calendar.SATURDAY -> "토"
        Calendar.SUNDAY -> "일"
        else -> ""
    }
}

private fun todayIndex(daily: List<DailyStats>): Int {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val todayStart = todayCal.timeInMillis
    val matched = daily.indexOfFirst { isSameDay(it.date, todayStart) }
    return if (matched >= 0) matched else daily.lastIndex
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun formatSleep(sleepMinutes: Long): String {
    val h = sleepMinutes / 60
    val m = sleepMinutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun dailySleepMinutes(entries: List<DailyEntry>): Long {
    // DailyEntry에는 duration 정보가 라벨 안에 묻혀 있어 정확 집계는 ViewModel 책임이지만,
    // 일일탭 요약 미니카드에서는 표기상 0으로 두지 말고 라벨에서 파싱 시도.
    var totalMins = 0L
    entries.filter { it.type == DailyEntryType.SLEEP }.forEach { e ->
        // "낮잠 · 1시간 20분" or "낮잠 · 35분"
        val durationStr = e.label.substringAfter(" · ", "")
        if (durationStr.isNotEmpty()) {
            val hourMatch = Regex("(\\d+)시간").find(durationStr)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val minMatch = Regex("(\\d+)분").find(durationStr)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            totalMins += hourMatch * 60 + minMatch
        }
    }
    return totalMins
}

// 색상을 약간 어둡게 만드는 확장 (다크모드 자동 대응: 토큰 기반 색을 darken)
private fun Color.darken(factor: Float): Color {
    val f = (1f - factor).coerceIn(0f, 1f)
    return Color(
        red = (red * f).coerceIn(0f, 1f),
        green = (green * f).coerceIn(0f, 1f),
        blue = (blue * f).coerceIn(0f, 1f),
        alpha = alpha,
    )
}
