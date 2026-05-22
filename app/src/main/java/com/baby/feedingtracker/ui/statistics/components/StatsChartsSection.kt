package com.baby.feedingtracker.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
// 디자이너 HTML 원안 충실 이식
//   - VerticalBarChartCard: 세로 막대 그래프 (수유/수면 차트 공용)
//   - DailyDateNavCard: 날짜 이전/다음 네비 카드
//   - DailyEntryRecordCard: 시간 + pill + 메인 텍스트
// ──────────────────────────────────────────────

private val ChartCardShape = RoundedCornerShape(16.dp)
private val ChartCardElevation = 1.dp

// ──────────────────────────────────────────────
// VerticalBarChartCard
// ──────────────────────────────────────────────

@Composable
fun VerticalBarChartCard(
    title: String,
    dailyValues: List<Float>,
    dayLabels: List<String>,
    todayIndex: Int,
    barColor: Color,
    barColorDark: Color,
    valueFormatter: (Float) -> String,
) {
    require(dailyValues.size == dayLabels.size) { "dailyValues / dayLabels size mismatch" }
    val maxValue = dailyValues.maxOrNull()?.coerceAtLeast(0.0001f) ?: 0.0001f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = ChartCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ChartCardElevation),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                dailyValues.forEachIndexed { index, value ->
                    val isToday = index == todayIndex
                    BarColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        value = value,
                        maxValue = maxValue,
                        label = dayLabels[index],
                        isToday = isToday,
                        barColor = barColor,
                        barColorDark = barColorDark,
                        valueText = valueFormatter(value),
                    )
                }
            }
        }
    }
}

@Composable
private fun BarColumn(
    modifier: Modifier = Modifier,
    value: Float,
    maxValue: Float,
    label: String,
    isToday: Boolean,
    barColor: Color,
    barColorDark: Color,
    valueText: String,
) {
    val ext = LocalExtendedColors.current
    val ratio = (value / maxValue).coerceIn(0f, 1f)
    // 막대 영역 높이는 약 80dp 사용 (라벨/값 위아래 여유)
    val barAreaHeight = 80.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = valueText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ext.subtleText,
        )
        Spacer(Modifier.height(4.dp))

        // 막대: bottom-aligned
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barAreaHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val targetHeight = (barAreaHeight.value * ratio).coerceAtLeast(4f).dp
            val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            val barBrush = if (isToday) {
                Brush.verticalGradient(listOf(barColorDark, barColorDark))
            } else {
                Brush.verticalGradient(listOf(barColor, barColorDark))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(targetHeight)
                    .clip(barShape)
                    .background(barBrush)
                    .then(
                        if (isToday) Modifier.border(2.dp, barColorDark, barShape) else Modifier
                    )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isToday) "오늘" else label,
            fontSize = 10.sp,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isToday) barColorDark else ext.subtleText,
        )
    }
}

// ──────────────────────────────────────────────
// DailyDateNavCard
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
    val isNextEnabled = dayStartMs < todayStart

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ChartCardElevation),
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
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = dateStr,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onNext, enabled = isNextEnabled) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "다음 날",
                    tint = if (isNextEnabled) MaterialTheme.colorScheme.onSurface
                    else ext.subtleText.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// "시간대별 기록" 섹션 헤더
// ──────────────────────────────────────────────

@Composable
fun DailySectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = LocalExtendedColors.current.subtleText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

// ──────────────────────────────────────────────
// DailyEntryRecordCard — 시간 + pill + 메인 텍스트
// ──────────────────────────────────────────────

@Composable
fun DailyEntryRecordCard(entry: DailyEntry) {
    val ext = LocalExtendedColors.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.KOREAN) }
    val (pillColor: Color, pillLabel: String) = when (entry.type) {
        DailyEntryType.FEEDING -> ext.categoryFeeding to feedingPillLabel(entry.label)
        DailyEntryType.DIAPER -> ext.categoryDiaper to diaperPillLabel(entry.label)
        DailyEntryType.SLEEP -> ext.categorySleep to "수면"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ChartCardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = timeFmt.format(Date(entry.timestamp)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(48.dp),
            )
            Spacer(Modifier.width(10.dp))
            Pill(text = pillLabel, color = pillColor)
            Spacer(Modifier.width(10.dp))
            Text(
                text = mainText(entry),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

private fun feedingPillLabel(label: String): String = when {
    label.startsWith("모유") -> "모유"
    label.startsWith("분유") -> "분유"
    label.startsWith("유축") -> "유축"
    else -> "수유"
}

private fun diaperPillLabel(label: String): String = when {
    label.contains("소변") -> "소변"
    label.contains("대변") -> "대변"
    else -> "기저귀"
}

private fun mainText(entry: DailyEntry): String {
    // Pill로 카테고리/타입을 표시하므로 main text에는 디테일만 남긴다.
    return when (entry.type) {
        DailyEntryType.FEEDING -> {
            // "분유 120ml" → "120ml", "모유" → "모유"
            val parts = entry.label.split(" ", limit = 2)
            if (parts.size == 2) parts[1] else entry.label
        }
        DailyEntryType.DIAPER -> {
            // "소변", "대변" → "기록됨" 같은 간단 표시
            entry.label
        }
        DailyEntryType.SLEEP -> {
            // "낮잠 · 1시간 20분" → "1시간 20분", or "낮잠"
            val parts = entry.label.split(" · ", limit = 2)
            if (parts.size == 2) parts[1] else entry.label
        }
    }
}
