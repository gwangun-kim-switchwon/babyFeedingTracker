package com.baby.feedingtracker.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.feedingtracker.data.BabyProfile
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

// ──────────────────────────────────────────────
// Stats Top Section Components (CEO 통합 단계에서 StatisticsScreen 연결)
// 카테고리 컬러 토큰 + coralAccent 강조 기반 모던 디자인
// 다크모드 자동 대응을 위해 모든 색상은 LocalExtendedColors / MaterialTheme 사용
// ──────────────────────────────────────────────

/**
 * 통계 탭 상단 Hero Card.
 * coralAccent 그라데이션 배경에 프로필 정보 + 통계 아이콘 표시.
 */
@Composable
fun StatsHeroCard(
    profile: BabyProfile?,
    daysOld: Int?,
    onNavigateToProfile: () -> Unit,
) {
    val coral = LocalExtendedColors.current.coralAccent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(coral, coral.copy(alpha = 0.78f))
                )
            )
            .clickable(onClick = onNavigateToProfile)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "📊", fontSize = 28.sp)
                Column {
                    if (profile != null && profile.name.isNotBlank()) {
                        Text(
                            text = profile.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (daysOld != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "생후 ${daysOld}일",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    } else {
                        Text(
                            text = "프로필을 설정하세요",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📈", fontSize = 22.sp)
            }
        }
    }
}

/**
 * 마일스톤 강조 카드 — Hero보다 살짝 작은 코럴 그라데이션.
 */
@Composable
fun MilestoneHighlightCard(
    milestoneTitle: String,
    milestoneDescription: String,
    nextMilestoneTitle: String?,
    nextMilestoneRemaining: Int?,
) {
    val coral = LocalExtendedColors.current.coralAccent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(coral, coral.copy(alpha = 0.78f))
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "🎉", fontSize = 36.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestoneTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = milestoneDescription,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.88f)
                )
                if (nextMilestoneTitle != null && nextMilestoneRemaining != null && nextMilestoneRemaining > 0) {
                    Spacer(Modifier.height(8.dp))
                    val unit = if (nextMilestoneTitle.contains("생후") || nextMilestoneTitle.contains("1년")) "일" else "회"
                    Text(
                        text = "다음: $nextMilestoneTitle (${nextMilestoneRemaining}${unit} 남음)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}

/**
 * 오늘의 기록 2x2 그리드.
 * 각 타일은 카테고리 컬러 soft tint 배경.
 */
@Composable
fun TodayStatsGrid(
    feedingCount: Int,
    diaperCount: Int,
    sleepMinutes: Long,
    cleaningCount: Int,
    onShareClick: () -> Unit,
) {
    val ext = LocalExtendedColors.current

    val sleepHours = sleepMinutes / 60
    val sleepMins = sleepMinutes % 60
    val sleepValue = if (sleepHours > 0) "${sleepHours}시간 ${sleepMins}분" else "${sleepMins}분"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "공유",
                        tint = ext.coralAccent
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "🍼",
                    label = "수유",
                    value = "${feedingCount}회",
                    tint = ext.categoryFeeding
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "🧷",
                    label = "기저귀",
                    value = "${diaperCount}회",
                    tint = ext.categoryDiaper
                )
            }
            Spacer(Modifier.height(10.dp))
            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "😴",
                    label = "수면",
                    value = sleepValue,
                    tint = ext.categorySleep
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "🧹",
                    label = "세척",
                    value = "${cleaningCount}회",
                    tint = ext.categoryBath
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    tint: Color,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = emoji, fontSize = 26.sp)
        Column {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = LocalExtendedColors.current.subtleText
            )
        }
    }
}

/**
 * 오늘 기록 없을 때 표시되는 빈 카드.
 */
@Composable
fun EmptyTodayCard(message: String = "아직 기록이 없어요") {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "📝", fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
