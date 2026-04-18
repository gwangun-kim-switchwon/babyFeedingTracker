package com.baby.feedingtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon

/**
 * Flighty식 고밀도 타임라인 카드 (R6 §7-4, R9 §5.2).
 *
 * 구조:
 *  - 좌측 4dp 세로 액센트 바 (카테고리 색상)
 *  - 40dp 카테고리 아이콘 (컨테이너 배경)
 *  - 중앙: 제목(titleMedium) + 시간(bodySmall, 회색)
 *  - 우측: 메트릭 2-3개 (예: "60ml · 15분")
 *  - 고정 높이 72dp
 *
 * 카드 배경: surfaceContainer (surfaceVariant) + RoundedCornerShape(16.dp).
 *
 * 사용 예시 (v1.3 이후):
 * ```
 * RecordTimelineCard(
 *     title = "수유",
 *     time = "오후 2:30",
 *     accentColor = LocalExtendedColors.current.categoryFeeding,
 *     icon = painterResource(R.drawable.ic_feeding),
 *     metrics = listOf("60ml", "15분"),
 *     onClick = { /* 상세로 이동 */ },
 * )
 * ```
 */
@Composable
fun RecordTimelineCard(
    title: String,
    time: String,
    accentColor: Color,
    icon: Painter,
    metrics: List<String> = emptyList(),
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 좌측 4dp 세로 액센트 바
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 카테고리 아이콘 (40dp 원형 컨테이너)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 중앙: 제목 + 시간
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 우측: 메트릭 (최대 3개, " · "로 구분)
            if (metrics.isNotEmpty()) {
                Text(
                    text = metrics.take(3).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 16.dp),
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}
