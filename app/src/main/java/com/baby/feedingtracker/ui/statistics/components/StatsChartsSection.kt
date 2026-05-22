package com.baby.feedingtracker.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.feedingtracker.ui.statistics.DailyEntry
import com.baby.feedingtracker.ui.statistics.DailyEntryType
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────
// Shared building blocks
// ──────────────────────────────────────────────

private val CardShape = RoundedCornerShape(20.dp)
private val CardElevation = 1.dp

@Composable
private fun ModernStatsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        content()
    }
}

@Composable
private fun CategoryDot(color: Color, size: Int = 6) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun SectionHeader(title: String) {
    val coral = LocalExtendedColors.current.coralAccent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryDot(color = coral, size = 8)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ──────────────────────────────────────────────
// 1) WeeklyStatsCard
// ──────────────────────────────────────────────

@Composable
fun WeeklyStatsCard(weeklyStats: com.baby.feedingtracker.data.WeeklyStats) {
    val ext = LocalExtendedColors.current
    val dailyStats = weeklyStats.dailyStats
    val maxFeeding = dailyStats.maxOfOrNull { it.feedingCount }?.coerceAtLeast(1) ?: 1
    val avgFeeding = if (dailyStats.isNotEmpty()) weeklyStats.totalFeedings.toFloat() / dailyStats.size else 0f
    val avgDiapers = if (dailyStats.isNotEmpty()) weeklyStats.totalDiapers.toFloat() / dailyStats.size else 0f
    val totalSleepHours = weeklyStats.totalSleepMinutes / 60f
    val avgSleepHours = if (dailyStats.isNotEmpty()) totalSleepHours / dailyStats.size else 0f

    ModernStatsCard {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "이번 주 통계")
            Spacer(Modifier.height(16.dp))

            Text(
                text = "수유 ${weeklyStats.totalFeedings}회 (일 평균 ${"%.1f".format(avgFeeding)}회)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))

            dailyStats.forEach { daily ->
                val cal = Calendar.getInstance().apply { timeInMillis = daily.date }
                val dayLabel = when (cal.get(Calendar.DAY_OF_WEEK)) {
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
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.subtleText,
                        modifier = Modifier.width(24.dp),
                    )
                    LinearProgressIndicator(
                        progress = { if (maxFeeding > 0) daily.feedingCount.toFloat() / maxFeeding else 0f },
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = ext.categoryFeeding,
                        trackColor = ext.categoryFeeding.copy(alpha = 0.18f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${daily.feedingCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(22.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Summary lines with category dots
            SummaryLine(
                color = ext.categoryFeeding,
                label = "수유",
                value = "${weeklyStats.totalFeedings}회 · 일 평균 ${"%.1f".format(avgFeeding)}회",
            )
            Spacer(Modifier.height(6.dp))
            SummaryLine(
                color = ext.categoryDiaper,
                label = "기저귀",
                value = "${weeklyStats.totalDiapers}회 · 일 평균 ${"%.1f".format(avgDiapers)}회",
            )
            Spacer(Modifier.height(6.dp))
            SummaryLine(
                color = ext.categorySleep,
                label = "수면",
                value = "${"%.1f".format(totalSleepHours)}시간 · 일 평균 ${"%.1f".format(avgSleepHours)}시간",
            )

            weeklyStats.averageFeedingInterval?.let { interval ->
                val hours = interval / 60
                val mins = interval % 60
                val intervalText = if (hours > 0) "${hours}시간 ${mins}분" else "${mins}분"
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "평균 수유 간격 $intervalText",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtleText,
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(color: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryDot(color = color, size = 6)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.subtleText,
        )
    }
}

// ──────────────────────────────────────────────
// 2) HourlyHeatmapCard
// ──────────────────────────────────────────────

@Composable
fun HourlyHeatmapCard(feedingByHour: Map<Int, Int>) {
    val ext = LocalExtendedColors.current
    val timeSlots = listOf(0, 3, 6, 9, 12, 15, 18, 21)
    val slotCounts = timeSlots.map { startHour ->
        val count = (startHour until startHour + 3).sumOf { feedingByHour[it] ?: 0 }
        startHour to count
    }
    val maxCount = slotCounts.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val baseColor = ext.categoryFeeding

    ModernStatsCard {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "시간대별 수유 패턴")
            Spacer(Modifier.height(16.dp))

            slotCounts.forEach { (startHour, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%02d시".format(startHour),
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.subtleText,
                        modifier = Modifier.width(36.dp),
                    )
                    Spacer(Modifier.width(8.dp))

                    val filledBlocks = if (maxCount > 0) {
                        ((count.toFloat() / maxCount) * 7).toInt().coerceIn(0, 7)
                    } else 0

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { blockIndex ->
                            val alpha = if (blockIndex < filledBlocks) {
                                // categoryFeeding alpha gradient: 0.12 → 1.0
                                val ratio = (blockIndex + 1).toFloat() / filledBlocks.coerceAtLeast(1)
                                (0.12f + 0.88f * ratio).coerceIn(0.12f, 1f)
                            } else {
                                0.12f
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(baseColor.copy(alpha = alpha))
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(22.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 3) PartnerContributionCard
// ──────────────────────────────────────────────

@Composable
fun PartnerContributionCard(contribution: com.baby.feedingtracker.data.PartnerContribution) {
    val ext = LocalExtendedColors.current
    val total = (contribution.user1Count + contribution.user2Count).coerceAtLeast(1)
    val user1Progress = contribution.user1Count.toFloat() / total
    val user2Progress = contribution.user2Count.toFloat() / total

    ModernStatsCard {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "이번 주 활동 분담")
            Spacer(Modifier.height(16.dp))

            // 나
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDot(color = ext.categoryFeeding, size = 8)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "나",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${"%.0f".format(contribution.user1Percentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { user1Progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
                color = ext.categoryFeeding,
                trackColor = ext.categoryFeeding.copy(alpha = 0.18f),
            )

            Spacer(Modifier.height(14.dp))

            // 배우자
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDot(color = ext.categorySleep, size = 8)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "배우자",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${"%.0f".format(contribution.user2Percentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { user2Progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
                color = ext.categorySleep,
                trackColor = ext.categorySleep.copy(alpha = 0.18f),
            )

            Spacer(Modifier.height(16.dp))

            // Per-category breakdown
            val categoryMeta = mapOf(
                "feeding" to ("수유" to ext.categoryFeeding),
                "diaper" to ("기저귀" to ext.categoryDiaper),
                "sleep" to ("수면" to ext.categorySleep),
                "cleaning" to ("세척" to ext.categoryBath),
            )
            contribution.byCategory.forEach { (category, pair) ->
                val catTotal = (pair.first + pair.second).coerceAtLeast(1)
                val u1Pct = pair.first * 100 / catTotal
                val u2Pct = pair.second * 100 / catTotal
                val meta = categoryMeta[category]
                val label = meta?.first ?: category
                val dotColor = meta?.second ?: ext.subtleText
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryDot(color = dotColor, size = 6)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(56.dp),
                    )
                    Text(
                        text = "나 ${u1Pct}% · 배우자 ${u2Pct}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.subtleText,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 4) DailyDateNavCard
// ──────────────────────────────────────────────

@Composable
fun DailyDateNavCard(
    dayStartMs: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val ext = LocalExtendedColors.current
    val formatter = remember { SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN) }
    val dateStr = formatter.format(Date(dayStartMs))
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val isToday = dayStartMs >= todayStart

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Outlined.ChevronLeft,
                    contentDescription = "이전 날",
                    tint = LocalContentColor.current,
                )
            }
            Text(
                text = dateStr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onNext, enabled = !isToday) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "다음 날",
                    tint = if (isToday) ext.subtleText else LocalContentColor.current,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 5) DailyEntryItem
// ──────────────────────────────────────────────

@Composable
fun DailyEntryItem(entry: com.baby.feedingtracker.ui.statistics.DailyEntry) {
    val ext = LocalExtendedColors.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.KOREAN) }
    val dotColor: Color = when (entry.type) {
        DailyEntryType.FEEDING -> ext.categoryFeeding
        DailyEntryType.DIAPER -> ext.categoryDiaper
        DailyEntryType.SLEEP -> ext.categorySleep
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = timeFmt.format(Date(entry.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = ext.subtleText,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(48.dp),
            )
            Spacer(Modifier.width(10.dp))
            CategoryDot(color = dotColor, size = 8)
            Spacer(Modifier.width(10.dp))
            Text(text = entry.icon, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
