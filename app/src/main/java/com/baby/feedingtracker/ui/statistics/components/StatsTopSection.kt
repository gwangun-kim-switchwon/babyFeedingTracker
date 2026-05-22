package com.baby.feedingtracker.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

// ──────────────────────────────────────────────
// Stats 상단 섹션 — 디자이너 HTML 원안 충실 이식
//   - 텍스트 헤더 (Hero 없음)
//   - 오늘의 요약 Coral 그라데이션 카드
// ──────────────────────────────────────────────

/**
 * 통계 탭 상단 텍스트 헤더.
 * "통계" (18sp, ExtraBold) + 날짜 범위 (12sp, subtleText)
 */
@Composable
fun StatsTextHeader(
    dateRangeText: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp)
    ) {
        Text(
            text = "통계",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = dateRangeText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = LocalExtendedColors.current.subtleText,
        )
    }
}

/**
 * 주간 탭 상단 "오늘의 요약" Coral 그라데이션 카드.
 * 가로 3분할: 수유 / 수면 / 기저귀 큰 숫자 + 작은 라벨
 */
@Composable
fun TodaySummaryCoralCard(
    feedingCount: Int,
    sleepMinutes: Long,
    diaperCount: Int,
    todayDateText: String,
) {
    val ext = LocalExtendedColors.current
    val sleepHours = sleepMinutes / 60
    val sleepMins = sleepMinutes % 60
    val sleepText = when {
        sleepHours > 0 && sleepMins > 0 -> "${sleepHours}시간 ${sleepMins}분"
        sleepHours > 0 -> "${sleepHours}시간"
        else -> "${sleepMins}분"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ext.categoryFeeding, ext.coralAccent),
                )
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "오늘의 요약 · $todayDateText",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SummaryMetric(
                    value = "${feedingCount}회",
                    label = "수유",
                )
                SummaryMetric(
                    value = sleepText,
                    label = "수면",
                )
                SummaryMetric(
                    value = "${diaperCount}회",
                    label = "기저귀",
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.80f),
        )
    }
}

// ──────────────────────────────────────────────
// Stat Num Card Row (3개 가로 미니 카드)
// 디자이너 안: 화이트 / 16dp rounded / 1dp shadow / 이모지 + 큰 숫자 + 작은 라벨
// ──────────────────────────────────────────────

data class StatNumItem(
    val emoji: String,
    val value: String,
    val label: String,
)

@Composable
fun StatNumCardRow(
    items: List<StatNumItem>,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { item ->
            StatNumCard(
                modifier = Modifier.weight(1f),
                emoji = item.emoji,
                value = item.value,
                label = item.label,
            )
        }
    }
}

@Composable
private fun StatNumCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalExtendedColors.current.subtleText,
            )
        }
    }
}
