package com.baby.feedingtracker.ui.cleaning

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.baby.feedingtracker.data.CleaningRecord
import com.baby.feedingtracker.ui.profile.BabyProfileBanner
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningScreen(
    viewModel: CleaningViewModel,
    babyProfileViewModel: BabyProfileViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastAddedRecord by viewModel.lastAddedRecord.collectAsStateWithLifecycle()
    val babyProfile by babyProfileViewModel.profile.collectAsStateWithLifecycle()
    val daysOld by babyProfileViewModel.daysOld.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var selectedRecord by remember { mutableStateOf<CleaningRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<CleaningRecord?>(null) }
    val listState = rememberLazyListState()

    // 마지막 3개 아이템 근처에서 자동 loadMore 트리거
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }.collect { should ->
            if (should && uiState.hasMoreData && !uiState.isLoadingMore) {
                viewModel.loadMore()
            }
        }
    }

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
        CleaningDeleteConfirmDialog(
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
        CleaningEditBottomSheet(
            record = record,
            isNewRecord = isNewRecord,
            onUpdateItemType = { itemType ->
                viewModel.updateItemType(record.id, itemType)
            },
            onUpdateTimestamp = { timestamp ->
                viewModel.updateTimestamp(record.id, timestamp)
            },
            onUpdateNote = { note ->
                viewModel.updateNote(record.id, note)
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
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // -- 아기 프로필 배너 --
            item {
                BabyProfileBanner(
                    profile = babyProfile,
                    daysOld = daysOld,
                    onNavigateToProfile = onNavigateToProfile
                )
            }

            // -- 상단: 경과 시간 영역 --
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    CleaningElapsedTimeSection(
                        elapsedMinutes = uiState.elapsedMinutes,
                        perTypeElapsed = uiState.perTypeElapsed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // -- 중단: 기록 목록 --
            if (uiState.records.isEmpty()) {
                item {
                    CleaningEmptyState(
                        modifier = Modifier
                            .fillParentMaxHeight(0.5f)
                            .fillMaxWidth()
                    )
                }
            } else {
                val groupedRecords = groupCleaningRecordsByDate(uiState.records)

                item { Spacer(modifier = Modifier.height(8.dp)) }

                groupedRecords.forEach { (dateLabel, dayRecords) ->
                    item(key = "header_$dateLabel") {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            CleaningDateSectionHeader(dateLabel)
                        }
                    }
                    item(key = "stats_$dateLabel") {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            CleaningDailyStats(dayRecords)
                        }
                    }
                    itemsIndexed(
                        items = dayRecords,
                        key = { _, record -> record.id }
                    ) { index, record ->
                        val isLast = index == dayRecords.lastIndex
                        val previousRecord = if (index + 1 < dayRecords.size) dayRecords[index + 1] else null

                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            CleaningTimelineRecordRow(
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

            // -- 하단: FAB가 가리지 않도록 Spacer --
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (selectedRecord == null) {
                    viewModel.addRecord()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp),
            containerColor = extendedColors.fabContainer,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "세척 기록 추가"
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

// ──────────────────────────────────────────────
// 경과 시간 섹션
// ──────────────────────────────────────────────

@Composable
private fun CleaningElapsedTimeSection(
    elapsedMinutes: Long?,
    perTypeElapsed: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    val itemTypeLabels = mapOf(
        "bottle" to "젖병",
        "pot" to "분유포트",
        "pump" to "유축기",
        "other" to "기타"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        if (elapsedMinutes != null) {
            Text(
                text = "마지막 세척",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = formatCleaningElapsedTime(elapsedMinutes),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        // 종류별 마지막 세척 경과 시간
        if (perTypeElapsed.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                perTypeElapsed.forEach { (type, minutes) ->
                    val label = itemTypeLabels[type] ?: type
                    Text(
                        text = "$label ${formatShortElapsed(minutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 빈 상태
// ──────────────────────────────────────────────

@Composable
private fun CleaningEmptyState(modifier: Modifier = Modifier) {
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
                text = "+ 버튼을 눌러\n첫 세척을 기록해보세요",
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
private fun CleaningDateSectionHeader(label: String) {
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
private fun CleaningDailyStats(records: List<CleaningRecord>) {
    val totalCount = records.size
    val bottleCount = records.count { it.itemType == "bottle" }
    val potCount = records.count { it.itemType == "pot" }
    val pumpCount = records.count { it.itemType == "pump" }
    val otherCount = records.count { it.itemType == "other" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CleaningStatChip(label = "총", value = "${totalCount}회")
        if (bottleCount > 0) CleaningStatChip(label = "젖병", value = "${bottleCount}회")
        if (potCount > 0) CleaningStatChip(label = "분유포트", value = "${potCount}회")
        if (pumpCount > 0) CleaningStatChip(label = "유축기", value = "${pumpCount}회")
        if (otherCount > 0) CleaningStatChip(label = "기타", value = "${otherCount}회")
    }
}

@Composable
private fun CleaningStatChip(label: String, value: String) {
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
private fun CleaningTimelineRecordRow(
    record: CleaningRecord,
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

                val itemTypeText = formatCleaningItemType(record.itemType)
                if (itemTypeText != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = itemTypeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                }

                // 메모 아이콘
                if (!record.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = "메모",
                        modifier = Modifier.size(16.dp),
                        tint = LocalExtendedColors.current.subtleText
                    )
                }
            }

            if (intervalMinutes != null && intervalMinutes > 0) {
                Text(
                    text = formatCleaningIntervalText(intervalMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.subtleText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 바텀시트: 세척 품목 선택
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleaningEditBottomSheet(
    record: CleaningRecord,
    isNewRecord: Boolean,
    onUpdateItemType: (itemType: String?) -> Unit,
    onUpdateTimestamp: (timestamp: Long) -> Unit,
    onUpdateNote: (String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedItemType by remember { mutableStateOf(record.itemType) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    var showTimePicker by remember { mutableStateOf(false) }
    var currentTimestamp by remember { mutableStateOf(record.timestamp) }
    var noteText by remember(record) { mutableStateOf(record.note ?: "") }
    val extendedColors = LocalExtendedColors.current

    ModalBottomSheet(
        onDismissRequest = {
            if (noteText.ifBlank { null } != record.note) {
                onUpdateNote(noteText.ifBlank { null })
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
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
                    text = "세척 기록",
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

            // 아이템 종류 선택 (2x2 그리드)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CleaningToggleButton(
                    text = "젖병",
                    selected = selectedItemType == "bottle",
                    onClick = {
                        val newType = if (selectedItemType == "bottle") null else "bottle"
                        selectedItemType = newType
                        onUpdateItemType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                CleaningToggleButton(
                    text = "분유포트",
                    selected = selectedItemType == "pot",
                    onClick = {
                        val newType = if (selectedItemType == "pot") null else "pot"
                        selectedItemType = newType
                        onUpdateItemType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CleaningToggleButton(
                    text = "유축기",
                    selected = selectedItemType == "pump",
                    onClick = {
                        val newType = if (selectedItemType == "pump") null else "pump"
                        selectedItemType = newType
                        onUpdateItemType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                CleaningToggleButton(
                    text = "기타",
                    selected = selectedItemType == "other",
                    onClick = {
                        val newType = if (selectedItemType == "other") null else "other"
                        selectedItemType = newType
                        onUpdateItemType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 메모 입력 UI
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "메모",
                style = MaterialTheme.typography.labelMedium,
                color = extendedColors.subtleText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("메모를 입력하세요") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = extendedColors.divider,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "삭제",
                    color = extendedColors.deleteColor,
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
private fun CleaningToggleButton(
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
// 삭제 확인 다이얼로그
// ──────────────────────────────────────────────

@Composable
private fun CleaningDeleteConfirmDialog(
    record: CleaningRecord,
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
                text = "${timeStr} 세척 기록을 삭제할까요?",
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

private fun formatCleaningElapsedTime(elapsedMinutes: Long?): String {
    if (elapsedMinutes == null) return "첫 세척을\n기록해보세요"
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분 전"
        hours > 0 -> "${hours}시간 전"
        minutes > 0 -> "${minutes}분 전"
        else -> "방금"
    }
}

private fun formatShortElapsed(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m 전"
        hours > 0 -> "${hours}h 전"
        mins > 0 -> "${mins}m 전"
        else -> "방금"
    }
}

private fun formatCleaningIntervalText(intervalMinutes: Long): String {
    val hours = intervalMinutes / 60
    val minutes = intervalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m 간격"
        hours > 0 -> "${hours}h 간격"
        else -> "${minutes}m 간격"
    }
}

private fun formatCleaningItemType(itemType: String?): String? {
    return when (itemType) {
        "bottle" -> "젖병"
        "pot" -> "분유포트"
        "pump" -> "유축기"
        "other" -> "기타"
        else -> null
    }
}

private fun groupCleaningRecordsByDate(records: List<CleaningRecord>): List<Pair<String, List<CleaningRecord>>> {
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
