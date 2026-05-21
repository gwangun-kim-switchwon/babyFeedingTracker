package com.baby.feedingtracker.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.SleepRecord
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
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

    var selectedRecord by remember { mutableStateOf<SleepRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<SleepRecord?>(null) }
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
        SleepDeleteConfirmDialog(
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
        SleepEditBottomSheet(
            record = record,
            isNewRecord = isNewRecord,
            onUpdateType = { type ->
                viewModel.updateType(record.id, type)
            },
            onUpdateTimestamp = { timestamp ->
                viewModel.updateTimestamp(record.id, timestamp)
            },
            onUpdateEndTimestamp = { endTimestamp ->
                viewModel.updateEndTimestamp(record.id, endTimestamp)
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
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // -- Hero Card --
            item {
                val todayMillis = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val todayRecs = uiState.records.filter { it.timestamp >= todayMillis }
                val now = System.currentTimeMillis()
                val todayTotalMinutes = todayRecs.sumOf { r ->
                    val end = r.endTimestamp ?: now
                    ((end - r.timestamp) / 60_000L).coerceAtLeast(0)
                }
                SleepHeroCard(
                    profile = babyProfile,
                    daysOld = daysOld,
                    elapsedMinutes = uiState.elapsedMinutes,
                    isCurrentlySleeping = uiState.isCurrentlySleeping,
                    todayTotalSleepMinutes = todayTotalMinutes,
                    todayNapCount = todayRecs.count { it.type == "nap" },
                    todayNightCount = todayRecs.count { it.type == "night" },
                    onNavigateToProfile = onNavigateToProfile
                )
            }

            // -- 중단: 기록 목록 --
            if (uiState.records.isEmpty()) {
                item {
                    SleepEmptyState(
                        modifier = Modifier
                            .fillParentMaxHeight(0.5f)
                            .fillMaxWidth()
                    )
                }
            } else {
                val groupedRecords = groupSleepRecordsByDate(uiState.records)

                item { Spacer(modifier = Modifier.height(8.dp)) }

                groupedRecords.forEach { (dateLabel, dayRecords) ->
                    item(key = "header_$dateLabel") {
                        SleepDateSectionHeader(dateLabel, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    itemsIndexed(
                        items = dayRecords,
                        key = { _, record -> record.id }
                    ) { _, record ->
                        SleepRecordCard(record = record, onClick = { selectedRecord = record })
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
                    if (uiState.isCurrentlySleeping) {
                        viewModel.endSleep()
                    } else {
                        viewModel.addRecord()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp),
            containerColor = if (uiState.isCurrentlySleeping) extendedColors.deleteColor else extendedColors.categorySleep,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (uiState.isCurrentlySleeping) Icons.Rounded.Stop else Icons.Rounded.Add,
                contentDescription = if (uiState.isCurrentlySleeping) "수면 종료" else "수면 기록 추가"
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
// 빈 상태
// ──────────────────────────────────────────────

@Composable
private fun SleepEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83D\uDE34",
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
                text = "+ 버튼을 눌러\n첫 수면을 기록해보세요",
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
private fun SleepDateSectionHeader(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
// 바텀시트: 수면 편집
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepEditBottomSheet(
    record: SleepRecord,
    isNewRecord: Boolean,
    onUpdateType: (type: String?) -> Unit,
    onUpdateTimestamp: (timestamp: Long) -> Unit,
    onUpdateEndTimestamp: (endTimestamp: Long) -> Unit,
    onUpdateNote: (String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf(record.type) }
    var currentTimestamp by remember { mutableStateOf(record.timestamp) }
    var endTimestampMs by remember { mutableStateOf(record.endTimestamp) }
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
            // 날짜 + 시간 편집 헤더 (시작 시간)
            com.baby.feedingtracker.ui.components.RecordDateTimeEditor(
                timestamp = currentTimestamp,
                titleSuffix = "수면 시작",
                onTimestampChange = {
                    currentTimestamp = it
                    onUpdateTimestamp(it)
                }
            )

            // 종료 시간 — endTimestamp가 있을 때만 표시
            if (endTimestampMs != null) {
                Spacer(modifier = Modifier.height(16.dp))
                com.baby.feedingtracker.ui.components.RecordDateTimeEditor(
                    timestamp = endTimestampMs!!,
                    titleSuffix = "종료",
                    onTimestampChange = {
                        endTimestampMs = it
                        onUpdateEndTimestamp(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 유형 선택 (2개 버튼 한 행)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SleepToggleButton(
                    text = "낮잠",
                    selected = selectedType == "nap",
                    onClick = {
                        val newType = if (selectedType == "nap") null else "nap"
                        selectedType = newType
                        onUpdateType(newType)
                        if (isNewRecord && newType != null) onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                SleepToggleButton(
                    text = "밤잠",
                    selected = selectedType == "night",
                    onClick = {
                        val newType = if (selectedType == "night") null else "night"
                        selectedType = newType
                        onUpdateType(newType)
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
private fun SleepToggleButton(
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
private fun SleepDeleteConfirmDialog(
    record: SleepRecord,
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
                text = "${timeStr} 수면 기록을 삭제할까요?",
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
// Sleep Hero Card (퍼플 그라디언트)
// ──────────────────────────────────────────────

@Composable
private fun SleepHeroCard(
    profile: com.baby.feedingtracker.data.BabyProfile?,
    daysOld: Int?,
    elapsedMinutes: Long?,
    isCurrentlySleeping: Boolean,
    todayTotalSleepMinutes: Long,
    todayNapCount: Int,
    todayNightCount: Int,
    onNavigateToProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFB3A3E8), Color(0xFF7B6CB5))))
            .clickable(onClick = onNavigateToProfile)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val babyName = if (profile != null && profile.name.isNotBlank())
                        "${profile.name} 🌙" else "아기 🌙"
                    Text(text = babyName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (daysOld != null) {
                        Text(text = "생후 ${daysOld}일", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f))
                    }
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text(text = "😴", fontSize = 20.sp) }
            }
            Spacer(Modifier.height(14.dp))
            val label = if (isCurrentlySleeping) "수면 중" else "마지막 수면"
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.80f), letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = formatSleepElapsedTime(elapsedMinutes, isCurrentlySleeping),
                fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White,
                letterSpacing = (-1).sp, lineHeight = 40.sp)
            if (isCurrentlySleeping) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.62f).fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
            if (todayTotalSleepMinutes > 0 || todayNapCount > 0 || todayNightCount > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SleepHeroStatItem(formatSleepDuration(todayTotalSleepMinutes), "오늘 총")
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.30f)))
                    SleepHeroStatItem("${todayNapCount}회", "낮잠")
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.30f)))
                    SleepHeroStatItem("${todayNightCount}회", "밤잠")
                }
            }
        }
    }
}

@Composable
private fun SleepHeroStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.80f))
    }
}

// ──────────────────────────────────────────────
// Sleep Record Card (기존 SleepTimelineRecordRow 대체)
// ──────────────────────────────────────────────

@Composable
private fun SleepRecordCard(record: SleepRecord, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val now = System.currentTimeMillis()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = timeFormat.format(Date(record.timestamp)), fontSize = 13.sp,
                fontWeight = FontWeight.Bold, color = Color(0xFF6E6A73), modifier = Modifier.width(38.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepTypePill(type = record.type)
                    val mainText = if (record.endTimestamp != null) {
                        formatSleepDuration(((record.endTimestamp - record.timestamp) / 60_000L).coerceAtLeast(0))
                    } else {
                        "수면 중"
                    }
                    Text(text = mainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = if (record.endTimestamp == null) Color(0xFF7B6CB5) else Color(0xFF1C1B1F))
                    if (!record.note.isNullOrBlank()) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Notes, contentDescription = null,
                            modifier = Modifier.size(14.dp), tint = Color(0xFF9E9E9E))
                    }
                }
                val timeRangeText = if (record.endTimestamp != null) {
                    "${timeFormat.format(Date(record.timestamp))} ~ ${timeFormat.format(Date(record.endTimestamp))}"
                } else {
                    "${timeFormat.format(Date(record.timestamp))} ~ 진행중"
                }
                Text(text = timeRangeText, fontSize = 12.sp, color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 2.dp))
            }
            if (record.endTimestamp == null) {
                val elapsedMin = ((now - record.timestamp) / 60_000L).coerceAtLeast(0)
                Text(text = formatSleepDuration(elapsedMin), fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, color = Color(0xFF7B6CB5), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun SleepTypePill(type: String?) {
    val (bgColor, textColor, label) = when (type) {
        "nap" -> Triple(Color(0xFFEAE6FA), Color(0xFF5C4E9E), "낮잠")
        "night" -> Triple(Color(0xFFD4CFF5), Color(0xFF3D3080), "밤잠")
        else -> return
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bgColor)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// ──────────────────────────────────────────────
// 유틸리티 함수
// ──────────────────────────────────────────────

private fun formatSleepElapsedTime(elapsedMinutes: Long?, isCurrentlySleeping: Boolean): String {
    if (elapsedMinutes == null) return "첫 수면을\n기록해보세요"
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return if (isCurrentlySleeping) {
        // 수면 중: 진행 시간 표시
        when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분째"
            hours > 0 -> "${hours}시간째"
            minutes > 0 -> "${minutes}분째"
            else -> "방금 시작"
        }
    } else {
        when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분 전"
            hours > 0 -> "${hours}시간 전"
            minutes > 0 -> "${minutes}분 전"
            else -> "방금"
        }
    }
}

private fun formatSleepDuration(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun formatSleepType(type: String?): String? {
    return when (type) {
        "nap" -> "낮잠"
        "night" -> "밤잠"
        else -> null
    }
}

private fun groupSleepRecordsByDate(records: List<SleepRecord>): List<Pair<String, List<SleepRecord>>> {
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
