package com.baby.feedingtracker.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    var recordToDelete by remember { mutableStateOf<FeedingRecord?>(null) }

    // 삭제 확인 다이얼로그
    recordToDelete?.let { record ->
        DeleteConfirmDialog(
            record = record,
            onConfirm = {
                viewModel.deleteRecord(record)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientTop,
                        extendedColors.gradientBottom
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // -- 상단: 경과 시간 영역 --
            ElapsedTimeSection(
                elapsedMinutes = uiState.elapsedMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
            )

            // -- 중단: 기록 목록 --
            FeedingRecordList(
                records = uiState.records,
                onSwipeToDelete = { record -> recordToDelete = record },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // -- 하단: 수유 기록 버튼 (thumb zone) --
            BottomActionButton(
                onClick = { viewModel.addRecord() },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────
// 경과 시간 섹션
// ──────────────────────────────────────────────

@Composable
private fun ElapsedTimeSection(
    elapsedMinutes: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        // 서브 라벨
        if (elapsedMinutes != null) {
            Text(
                text = "마지막 수유",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 메인 경과 시간 (매우 크게)
        Text(
            text = formatElapsedTimeDisplay(elapsedMinutes),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        // 보조 텍스트
        if (elapsedMinutes != null) {
            Text(
                text = "전",
                style = MaterialTheme.typography.headlineSmall,
                color = LocalExtendedColors.current.subtleText
            )
        }
    }
}

// ──────────────────────────────────────────────
// 기록 목록
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedingRecordList(
    records: List<FeedingRecord>,
    onSwipeToDelete: (FeedingRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        EmptyState(modifier = modifier)
    } else {
        val groupedRecords = groupRecordsByDate(records)

        LazyColumn(
            modifier = modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            groupedRecords.forEach { (dateLabel, dayRecords) ->
                item(key = "header_$dateLabel") {
                    DateSectionHeader(dateLabel)
                }
                itemsIndexed(
                    items = dayRecords,
                    key = { _, record -> record.id }
                ) { index, record ->
                    val previousRecord = if (index + 1 < dayRecords.size) dayRecords[index + 1] else null
                    SwipeableRecordRow(
                        record = record,
                        intervalMinutes = previousRecord?.let {
                            ((record.timestamp - it.timestamp) / 60_000L)
                        },
                        onSwipeToDelete = { onSwipeToDelete(record) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ──────────────────────────────────────────────
// 빈 상태 (Empty State)
// ──────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83C\uDF7C",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "아직 기록이 없어요",
                style = MaterialTheme.typography.headlineSmall,
                color = LocalExtendedColors.current.subtleText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "아래 버튼을 눌러\n첫 수유를 기록해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ──────────────────────────────────────────────
// 날짜 섹션 헤더
// ──────────────────────────────────────────────

@Composable
private fun DateSectionHeader(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = LocalExtendedColors.current.subtleText
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LocalExtendedColors.current.divider)
        )
    }
}

// ──────────────────────────────────────────────
// 스와이프 가능한 기록 행
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRecordRow(
    record: FeedingRecord,
    intervalMinutes: Long?,
    onSwipeToDelete: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == DismissValue.DismissedToStart) {
                onSwipeToDelete()
            }
            false // 항상 false를 반환하여 스와이프가 자동으로 원래 위치로 돌아가게 함
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            // 삭제 배경 (스와이프 시 노출)
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == DismissValue.DismissedToStart) {
                    LocalExtendedColors.current.deleteColor.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                label = "swipeBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "삭제",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalExtendedColors.current.deleteColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissContent = {
            // 기록 행
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }

                // 시간
                Text(
                    text = timeFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // 간격 표시
                if (intervalMinutes != null && intervalMinutes > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatIntervalText(intervalMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalExtendedColors.current.subtleText.copy(alpha = 0.7f)
                    )
                }
            }
        }
    )
}

// ──────────────────────────────────────────────
// 하단 액션 버튼 (pill shape, thumb zone)
// ──────────────────────────────────────────────

@Composable
private fun BottomActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = "+ 수유 기록",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

// ──────────────────────────────────────────────
// 삭제 확인 다이얼로그
// ──────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    record: FeedingRecord,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(record.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = "기록 삭제",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "${timeStr} 수유 기록을 삭제할까요?",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalExtendedColors.current.subtleText
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "삭제",
                    color = LocalExtendedColors.current.deleteColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "취소",
                    color = LocalExtendedColors.current.subtleText
                )
            }
        }
    )
}

// ──────────────────────────────────────────────
// 유틸리티 함수
// ──────────────────────────────────────────────

private fun formatElapsedTimeDisplay(elapsedMinutes: Long?): String {
    if (elapsedMinutes == null) return "첫 수유를\n기록해보세요"
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        minutes > 0 -> "${minutes}분"
        else -> "방금"
    }
}

private fun formatIntervalText(intervalMinutes: Long): String {
    val hours = intervalMinutes / 60
    val minutes = intervalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m 간격"
        hours > 0 -> "${hours}h 간격"
        else -> "${minutes}m 간격"
    }
}

private fun groupRecordsByDate(records: List<FeedingRecord>): List<Pair<String, List<FeedingRecord>>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayMillis = today.timeInMillis

    val yesterday = (today.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val yesterdayMillis = yesterday.timeInMillis

    val dateFormat = SimpleDateFormat("M월 d일", Locale.KOREA)

    return records
        .groupBy { record ->
            when {
                record.timestamp >= todayMillis -> "오늘"
                record.timestamp >= yesterdayMillis -> "어제"
                else -> dateFormat.format(Date(record.timestamp))
            }
        }
        .toList()
}
