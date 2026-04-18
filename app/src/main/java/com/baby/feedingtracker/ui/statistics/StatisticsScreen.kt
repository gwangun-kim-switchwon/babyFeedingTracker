package com.baby.feedingtracker.ui.statistics

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.BabyProfile
import com.baby.feedingtracker.data.DailyStats
import com.baby.feedingtracker.data.PartnerContribution
import com.baby.feedingtracker.data.WeeklyStats
import com.baby.feedingtracker.ui.profile.BabyProfileBanner
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
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
    val babyProfile by babyProfileViewModel.profile.collectAsStateWithLifecycle()
    val daysOld by babyProfileViewModel.daysOld.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientTop,
                        extendedColors.gradientBottom
                    )
                )
            )
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
                // 3-1. Baby Profile Banner
                item {
                    BabyProfileBanner(
                        profile = babyProfile,
                        daysOld = daysOld,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }

                // 3-2. Milestone Card
                val latestMilestone = uiState.milestones.lastOrNull()
                if (latestMilestone != null) {
                    item {
                        MilestoneCard(
                            milestoneTitle = latestMilestone.title,
                            milestoneDescription = latestMilestone.description,
                            nextMilestoneTitle = uiState.nextMilestone?.title,
                            nextMilestoneRemaining = uiState.nextMilestoneRemaining
                        )
                    }
                }

                // 3-3. Today's Summary Card
                val todayStats = uiState.todayStats
                if (todayStats != null) {
                    item {
                        TodaySummaryCard(
                            stats = todayStats,
                            babyName = uiState.babyName,
                            daysOld = uiState.daysOld,
                            context = context
                        )
                    }
                } else {
                    item {
                        EmptyCard(message = "아직 기록이 없어요")
                    }
                }

                // 3-4. Weekly Stats Card
                val weeklyStats = uiState.weeklyStats
                if (weeklyStats != null) {
                    item {
                        WeeklyStatsCard(weeklyStats = weeklyStats)
                    }
                }

                // 3-5. Hourly Feeding Heatmap
                if (weeklyStats != null && weeklyStats.feedingByHour.isNotEmpty()) {
                    item {
                        FeedingHeatmapCard(feedingByHour = weeklyStats.feedingByHour)
                    }
                }

                // 3-6. Partner Contribution
                val contribution = uiState.partnerContribution
                if (contribution != null && contribution.user2Uid != null) {
                    item {
                        PartnerContributionCard(contribution = contribution)
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    milestoneTitle: String,
    milestoneDescription: String,
    nextMilestoneTitle: String?,
    nextMilestoneRemaining: Int?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "\uD83C\uDF89 $milestoneTitle!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = milestoneDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (nextMilestoneTitle != null && nextMilestoneRemaining != null && nextMilestoneRemaining > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "다음 목표: $nextMilestoneTitle (${nextMilestoneRemaining}${if (nextMilestoneTitle.contains("생후") || nextMilestoneTitle.contains("1년")) "일" else "회"} 남음)",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.subtleText
                )
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(
    stats: DailyStats,
    babyName: String?,
    daysOld: Int?,
    context: Context
) {
    val sleepHours = stats.sleepTotalMinutes / 60
    val sleepMins = stats.sleepTotalMinutes % 60

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "오늘의 기록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        shareTodayStats(context, stats, babyName, daysOld)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "공유",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatRow(emoji = "\uD83C\uDF7C", label = "수유", value = "${stats.feedingCount}회")
            Spacer(modifier = Modifier.height(4.dp))
            StatRow(emoji = "\uD83E\uDDF7", label = "기저귀", value = "${stats.diaperCount}회")
            Spacer(modifier = Modifier.height(4.dp))
            StatRow(
                emoji = "\uD83D\uDE34",
                label = "수면",
                value = if (sleepHours > 0) "${sleepHours}시간 ${sleepMins}분" else "${sleepMins}분"
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatRow(emoji = "\uD83E\uDDF9", label = "세척", value = "${stats.cleaningCount}회")
        }
    }
}

@Composable
private fun StatRow(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$emoji $label",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

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
        |\uD83C\uDF7C 수유 ${stats.feedingCount}회
        |\uD83E\uDDF7 기저귀 ${stats.diaperCount}회
        |\uD83D\uDE34 수면 $sleepText
        |\uD83E\uDDF9 세척 ${stats.cleaningCount}회
    """.trimMargin()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "오늘의 기록 공유")
    context.startActivity(shareIntent)
}

@Composable
private fun WeeklyStatsCard(weeklyStats: WeeklyStats) {
    val dailyStats = weeklyStats.dailyStats
    val maxFeeding = dailyStats.maxOfOrNull { it.feedingCount } ?: 1
    val avgFeeding = if (dailyStats.isNotEmpty()) weeklyStats.totalFeedings.toFloat() / dailyStats.size else 0f
    val avgDiapers = if (dailyStats.isNotEmpty()) weeklyStats.totalDiapers.toFloat() / dailyStats.size else 0f
    val totalSleepHours = weeklyStats.totalSleepMinutes / 60f
    val avgSleepHours = if (dailyStats.isNotEmpty()) totalSleepHours / dailyStats.size else 0f
    val avgIntervalText = weeklyStats.averageFeedingInterval?.let {
        val hours = it / 60
        val mins = it % 60
        if (hours > 0) "${hours}시간 ${mins}분" else "${mins}분"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "이번 주 통계",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Feeding bar chart
            Text(
                text = "수유 ${weeklyStats.totalFeedings}회 (일 평균 ${"%.1f".format(avgFeeding)}회)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            dailyStats.forEach { daily ->
                val cal = Calendar.getInstance().apply { timeInMillis = daily.date }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val dayLabel = when (dayOfWeek) {
                    Calendar.MONDAY -> "월"
                    Calendar.TUESDAY -> "화"
                    Calendar.WEDNESDAY -> "수"
                    Calendar.THURSDAY -> "목"
                    Calendar.FRIDAY -> "금"
                    Calendar.SATURDAY -> "토"
                    Calendar.SUNDAY -> "일"
                    else -> ""
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalExtendedColors.current.subtleText,
                        modifier = Modifier.width(24.dp)
                    )
                    LinearProgressIndicator(
                        progress = { if (maxFeeding > 0) daily.feedingCount.toFloat() / maxFeeding else 0f },
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${daily.feedingCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(20.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "기저귀 ${weeklyStats.totalDiapers}회 (일 평균 ${"%.1f".format(avgDiapers)}회)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "수면 총 ${"%.1f".format(totalSleepHours)}시간 (일 평균 ${"%.1f".format(avgSleepHours)}시간)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (avgIntervalText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "평균 수유 간격 $avgIntervalText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FeedingHeatmapCard(feedingByHour: Map<Int, Int>) {
    val timeSlots = listOf(0, 3, 6, 9, 12, 15, 18, 21)
    val slotCounts = timeSlots.map { startHour ->
        val count = (startHour until startHour + 3).sumOf { hour ->
            feedingByHour[hour] ?: 0
        }
        Pair(startHour, count)
    }
    val maxCount = slotCounts.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "시간대별 수유 패턴",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            slotCounts.forEach { (startHour, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d시".format(startHour),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalExtendedColors.current.subtleText,
                        modifier = Modifier.width(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Heatmap blocks (7 blocks)
                    val filledBlocks = if (maxCount > 0) {
                        ((count.toFloat() / maxCount) * 7).toInt().coerceIn(0, 7)
                    } else 0

                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(7) { blockIndex ->
                            val alpha = if (blockIndex < filledBlocks) {
                                0.3f + (0.7f * (blockIndex + 1) / filledBlocks.coerceAtLeast(1))
                            } else {
                                0.1f
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(primaryColor.copy(alpha = alpha.coerceIn(0.1f, 1f)))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(20.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun PartnerContributionCard(contribution: PartnerContribution) {
    val total = (contribution.user1Count + contribution.user2Count).coerceAtLeast(1)
    val user1Progress = contribution.user1Count.toFloat() / total
    val user2Progress = contribution.user2Count.toFloat() / total

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "이번 주 활동 분담",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Overall bar
            Text(
                text = "나 ${"%.0f".format(contribution.user1Percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { user1Progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "배우자 ${"%.0f".format(contribution.user2Percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { user2Progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // By category
            val categoryLabels = mapOf(
                "feeding" to "수유",
                "diaper" to "기저귀",
                "sleep" to "수면",
                "cleaning" to "세척"
            )
            contribution.byCategory.forEach { (category, pair) ->
                val catTotal = (pair.first + pair.second).coerceAtLeast(1)
                val u1Pct = pair.first * 100 / catTotal
                val u2Pct = pair.second * 100 / catTotal
                val label = categoryLabels[category] ?: category
                Text(
                    text = "$label  나 ${u1Pct}% / 배우자 ${u2Pct}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.subtleText,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.subtleText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        )
    }
}
