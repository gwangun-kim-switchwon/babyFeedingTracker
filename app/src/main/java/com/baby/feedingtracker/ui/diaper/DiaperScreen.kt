package com.baby.feedingtracker.ui.diaper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.DiaperRecord
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaperScreen(viewModel: DiaperViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastAddedRecord by viewModel.lastAddedRecord.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current

    var selectedRecord by remember { mutableStateOf<DiaperRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<DiaperRecord?>(null) }

    // 새 기록 추가 시 바텀시트 자동 오픈
    LaunchedEffect(lastAddedRecord) {
        lastAddedRecord?.let { record ->
            selectedRecord = record
            isNewRecord = true
            viewModel.clearLastAddedRecord()
        }
    }

    // 삭제 확인 다이얼로그
    recordToDelete?.let { record ->
        DiaperDeleteConfirmDialog(
            record = record,
            onConfirm = {
                viewModel.deleteRecord(record)
                recordToDelete = null
                selectedRecord = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    // 바텀시트
    selectedRecord?.let { record ->
        DiaperEditBottomSheet(
            record = record,
            isNewRecord = isNewRecord,
            onUpdateType = { type ->
                viewModel.updateType(record.id, type)
            },
            onUpdateTimestamp = { timestamp ->
                viewModel.updateTimestamp(record.id, timestamp)
            },
            onDelete = {
                recordToDelete = record
            },
            onDismiss = {
                selectedRecord = null
                isNewRecord = false
            }
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
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // -- 상단: 경과 시간 영역 --
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 48.dp, bottom = 16.dp)
                ) {
                    DiaperElapsedTimeSection(
                        elapsedMinutes = uiState.elapsedMinutes,
                        todayDiaperCount = uiState.todayDiaperCount,
                        todayUrineCount = uiState.todayUrineCount,
                        todayStoolCount = uiState.todayStoolCount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // -- 중단: 기록 목록 --
            if (uiState.records.isEmpty()) {
                item {
                    DiaperEmptyState(
                        modifier = Modifier
                            .fillParentMaxHeight(0.5f)
                            .fillMaxWidth()
                    )
                }
            } else {
                val groupedRecords = groupDiaperRecordsByDate(uiState.records)

                item { Spacer(modifier = Modifier.height(8.dp)) }

                groupedRecords.forEach { (dateLabel, dayRecords) ->
                    item(key = "header_$dateLabel") {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DiaperDateSectionHeader(dateLabel)
                        }
                    }
                    item(key = "stats_$dateLabel") {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DiaperDailyStats(dayRecords)
                        }
                    }
                    itemsIndexed(
                        items = dayRecords,
                        key = { _, record -> record.id }
                    ) { index, record ->
                        val isLast = index == dayRecords.lastIndex
                        val previousRecord = if (index + 1 < dayRecords.size) dayRecords[index + 1] else null

                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DiaperTimelineRecordRow(
                                record = record,
                                intervalMinutes = previousRecord?.let {
                                    ((record.timestamp - it.timestamp) / 60_000L)
                                },
                                showLine = !isLast,
                                onClick = { selectedRecord = record }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // -- 하단: 기저귀 기록 버튼 --
            item {
                DiaperBottomActionButton(
                    onClick = { viewModel.addRecord() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 경과 시간 섹션
// ──────────────────────────────────────────────

@Composable
private fun DiaperElapsedTimeSection(
    elapsedMinutes: Long?,
    todayDiaperCount: Int,
    todayUrineCount: Int,
    todayStoolCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        if (elapsedMinutes != null) {
            Text(
                text = "마지막 기저귀",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = formatDiaperElapsedTime(elapsedMinutes),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        // 오늘 통계
        val statParts = buildList {
            if (todayDiaperCount > 0) add("기저귀 ${todayDiaperCount}회")
            if (todayUrineCount > 0) add("소변 ${todayUrineCount}회")
            if (todayStoolCount > 0) add("대변 ${todayStoolCount}회")
        }
        if (statParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "오늘: ${statParts.joinToString(" · ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText
            )
        }
    }
}

// ──────────────────────────────────────────────
// 기록 목록 (타임라인)
// ──────────────────────────────────────────────

@Composable
private fun DiaperRecordList(
    records: List<DiaperRecord>,
    onRecordClick: (DiaperRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        DiaperEmptyState(modifier = modifier)
    } else {
        val groupedRecords = groupDiaperRecordsByDate(records)

        LazyColumn(
            modifier = modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            groupedRecords.forEach { (dateLabel, dayRecords) ->
                item(key = "header_$dateLabel") {
                    DiaperDateSectionHeader(dateLabel)
                }
                item(key = "stats_$dateLabel") {
                    DiaperDailyStats(dayRecords)
                }
                itemsIndexed(
                    items = dayRecords,
                    key = { _, record -> record.id }
                ) { index, record ->
                    val isLast = index == dayRecords.lastIndex
                    val previousRecord = if (index + 1 < dayRecords.size) dayRecords[index + 1] else null

                    DiaperTimelineRecordRow(
                        record = record,
                        intervalMinutes = previousRecord?.let {
                            ((record.timestamp - it.timestamp) / 60_000L)
                        },
                        showLine = !isLast,
                        onClick = { onRecordClick(record) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ──────────────────────────────────────────────
// 빈 상태
// ──────────────────────────────────────────────

@Composable
private fun DiaperEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83D\uDC76",
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
                text = "아래 버튼을 눌러\n첫 기저귀를 기록해보세요",
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
private fun DiaperDateSectionHeader(label: String) {
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
// 일일 통계
// ──────────────────────────────────────────────

@Composable
private fun DiaperDailyStats(records: List<DiaperRecord>) {
    val totalCount = records.size
    val diaperCount = records.count { it.type == "diaper" }
    val urineCount = records.count { it.type == "urine" }
    val stoolCount = records.count { it.type == "stool" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiaperStatChip(label = "총", value = "${totalCount}회")
        if (diaperCount > 0) DiaperStatChip(label = "기저귀", value = "${diaperCount}회")
        if (urineCount > 0) DiaperStatChip(label = "소변", value = "${urineCount}회")
        if (stoolCount > 0) DiaperStatChip(label = "대변", value = "${stoolCount}회")
    }
}

@Composable
private fun DiaperStatChip(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalExtendedColors.current.subtleText
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ──────────────────────────────────────────────
// 타임라인 기록 행
// ──────────────────────────────────────────────

@Composable
private fun DiaperTimelineRecordRow(
    record: DiaperRecord,
    intervalMinutes: Long?,
    showLine: Boolean,
    onClick: () -> Unit
) {
    val dotSize = 10.dp
    val lineWidth = 1.5.dp
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 타임라인 (동그라미 + 세로선)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(lineWidth)
                        .height(36.dp)
                        .background(accentColor.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 컨텐츠
        Column(
            modifier = Modifier.padding(bottom = if (showLine) 0.dp else 4.dp)
        ) {
            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                val typeText = formatDiaperType(record.type)
                if (typeText != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                }
            }

            if (intervalMinutes != null && intervalMinutes > 0) {
                Text(
                    text = formatDiaperIntervalText(intervalMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.subtleText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 바텀시트: 기저귀 유형 선택
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaperEditBottomSheet(
    record: DiaperRecord,
    isNewRecord: Boolean,
    onUpdateType: (type: String?) -> Unit,
    onUpdateTimestamp: (timestamp: Long) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedType by remember { mutableStateOf(record.type) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    var showTimePicker by remember { mutableStateOf(false) }
    var currentTimestamp by remember { mutableStateOf(record.timestamp) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // 시간 표시 (탭하면 TimePicker 오픈)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showTimePicker = true }
            ) {
                Text(
                    text = timeFormat.format(Date(currentTimestamp)),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "기저귀 기록",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (showTimePicker) {
                val calendar = remember {
                    Calendar.getInstance().apply { timeInMillis = currentTimestamp }
                }
                val timePickerState = rememberTimePickerState(
                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                    initialMinute = calendar.get(Calendar.MINUTE),
                    is24Hour = true
                )

                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = currentTimestamp
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                            }
                            currentTimestamp = newCal.timeInMillis
                            onUpdateTimestamp(currentTimestamp)
                            showTimePicker = false
                        }) {
                            Text("확인")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("취소", color = LocalExtendedColors.current.subtleText)
                        }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 유형 선택 (3개 버튼 한 행)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DiaperToggleButton(
                    text = "기저귀",
                    selected = selectedType == "diaper",
                    onClick = {
                        val newType = if (selectedType == "diaper") null else "diaper"
                        selectedType = newType
                        onUpdateType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                DiaperToggleButton(
                    text = "소변",
                    selected = selectedType == "urine",
                    onClick = {
                        val newType = if (selectedType == "urine") null else "urine"
                        selectedType = newType
                        onUpdateType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                DiaperToggleButton(
                    text = "대변",
                    selected = selectedType == "stool",
                    onClick = {
                        val newType = if (selectedType == "stool") null else "stool"
                        selectedType = newType
                        onUpdateType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "삭제",
                    color = LocalExtendedColors.current.deleteColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 토글 버튼
// ──────────────────────────────────────────────

@Composable
private fun DiaperToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ──────────────────────────────────────────────
// 하단 액션 버튼
// ──────────────────────────────────────────────

@Composable
private fun DiaperBottomActionButton(
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
            text = "+ 기저귀 기록",
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
private fun DiaperDeleteConfirmDialog(
    record: DiaperRecord,
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
                text = "${timeStr} 기저귀 기록을 삭제할까요?",
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

private fun formatDiaperElapsedTime(elapsedMinutes: Long?): String {
    if (elapsedMinutes == null) return "첫 기저귀를\n기록해보세요"
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분 전"
        hours > 0 -> "${hours}시간 전"
        minutes > 0 -> "${minutes}분 전"
        else -> "방금"
    }
}

private fun formatDiaperIntervalText(intervalMinutes: Long): String {
    val hours = intervalMinutes / 60
    val minutes = intervalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m 간격"
        hours > 0 -> "${hours}h 간격"
        else -> "${minutes}m 간격"
    }
}

private fun formatDiaperType(type: String?): String? {
    return when (type) {
        "diaper" -> "기저귀"
        "urine" -> "소변"
        "stool" -> "대변"
        else -> null
    }
}

private fun groupDiaperRecordsByDate(records: List<DiaperRecord>): List<Pair<String, List<DiaperRecord>>> {
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
