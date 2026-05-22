package com.baby.feedingtracker.ui.statistics

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.DailyStats
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.statistics.components.DailyDateNavCard
import com.baby.feedingtracker.ui.statistics.components.DailyEntryItem
import com.baby.feedingtracker.ui.statistics.components.EmptyTodayCard
import com.baby.feedingtracker.ui.statistics.components.HourlyHeatmapCard
import com.baby.feedingtracker.ui.statistics.components.MilestoneHighlightCard
import com.baby.feedingtracker.ui.statistics.components.PartnerContributionCard
import com.baby.feedingtracker.ui.statistics.components.StatsHeroCard
import com.baby.feedingtracker.ui.statistics.components.TodayStatsGrid
import com.baby.feedingtracker.ui.statistics.components.WeeklyStatsCard
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    babyProfileViewModel: BabyProfileViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val babyProfile by babyProfileViewModel.profile.collectAsStateWithLifecycle()
    val daysOld by babyProfileViewModel.daysOld.collectAsStateWithLifecycle()
    val selectedDayStart by viewModel.selectedDayStart.collectAsStateWithLifecycle()
    val dailyEntries by viewModel.dailyEntries.collectAsStateWithLifecycle()
    val isDailyLoading by viewModel.isDailyLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }  // 0=주간, 1=일일

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Hero (코럴 그라데이션 — 통계 탭 아이덴티티)
                item {
                    StatsHeroCard(
                        profile = babyProfile,
                        daysOld = daysOld,
                        onNavigateToProfile = onNavigateToProfile,
                    )
                }

                // Tab switcher
                item {
                    StatTabSwitcher(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                if (selectedTab == 0) {
                    // Milestone
                    val latestMilestone = uiState.milestones.lastOrNull()
                    if (latestMilestone != null) {
                        item {
                            MilestoneHighlightCard(
                                milestoneTitle = latestMilestone.title,
                                milestoneDescription = latestMilestone.description,
                                nextMilestoneTitle = uiState.nextMilestone?.title,
                                nextMilestoneRemaining = uiState.nextMilestoneRemaining
                            )
                        }
                    }

                    // Today (2x2 그리드)
                    val todayStats = uiState.todayStats
                    if (todayStats != null) {
                        item {
                            TodayStatsGrid(
                                feedingCount = todayStats.feedingCount,
                                diaperCount = todayStats.diaperCount,
                                sleepMinutes = todayStats.sleepTotalMinutes,
                                cleaningCount = todayStats.cleaningCount,
                                onShareClick = {
                                    shareTodayStats(
                                        context,
                                        todayStats,
                                        uiState.babyName,
                                        uiState.daysOld,
                                    )
                                },
                            )
                        }
                    } else {
                        item { EmptyTodayCard() }
                    }

                    // Weekly Stats
                    val weeklyStats = uiState.weeklyStats
                    if (weeklyStats != null) {
                        item { WeeklyStatsCard(weeklyStats = weeklyStats) }
                    }

                    // Hourly Heatmap
                    if (weeklyStats != null && weeklyStats.feedingByHour.isNotEmpty()) {
                        item { HourlyHeatmapCard(feedingByHour = weeklyStats.feedingByHour) }
                    }

                    // Partner Contribution
                    val contribution = uiState.partnerContribution
                    if (contribution != null && contribution.user2Uid != null) {
                        item { PartnerContributionCard(contribution = contribution) }
                    }
                } else {
                    // 일일 탭
                    item {
                        DailyDateNavCard(
                            dayStartMs = selectedDayStart,
                            onPrevious = { viewModel.previousDay() },
                            onNext = { viewModel.nextDay() },
                        )
                    }

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
                                    .padding(top = 48.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        items(dailyEntries, key = { it.timestamp.toString() + it.type }) { entry ->
                            DailyEntryItem(entry)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Statistics Tab Switcher
// ──────────────────────────────────────────────

@Composable
private fun StatTabSwitcher(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        listOf("주간", "일일").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selectedTab == index)
                            Modifier.shadow(elevation = 1.dp, shape = RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == index) Color.White else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == index)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Share helper (오늘의 기록 텍스트 공유)
// ──────────────────────────────────────────────

private fun shareTodayStats(
    context: Context,
    stats: DailyStats,
    babyName: String?,
    daysOld: Int?
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = dateFormat.format(Date())
    val sleepHours = stats.sleepTotalMinutes / 60
    val sleepMins = stats.sleepTotalMinutes % 60
    val sleepText = if (sleepHours > 0) "${sleepHours}시간 ${sleepMins}분" else "${sleepMins}분"

    val header = if (babyName != null && daysOld != null) {
        "[맘마미아] $babyName 생후 ${daysOld}일"
    } else {
        "[맘마미아]"
    }

    val text = """
        |$header
        |오늘의 기록 ($todayStr)
        |🍼 수유 ${stats.feedingCount}회
        |🧷 기저귀 ${stats.diaperCount}회
        |😴 수면 $sleepText
        |🧹 세척 ${stats.cleaningCount}회
    """.trimMargin()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "오늘의 기록 공유")
    context.startActivity(shareIntent)
}
