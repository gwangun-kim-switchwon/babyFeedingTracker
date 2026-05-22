package com.baby.feedingtracker.ui.diaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.baby.feedingtracker.data.DiaperRecord
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaperScreen(
    viewModel: DiaperViewModel,
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

    var selectedRecord by remember { mutableStateOf<DiaperRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<DiaperRecord?>(null) }
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
            onUpdateDetail = { detail ->
                viewModel.updateDetail(record.id, detail)
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
            .background(MaterialTheme.colorScheme.background)
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
                DiaperHeroCard(
                    profile = babyProfile,
                    daysOld = daysOld,
                    elapsedMinutes = uiState.elapsedMinutes,
                    lastRecord = uiState.records.firstOrNull(),
                    todayTotalCount = todayRecs.size,
                    todayUrineCount = todayRecs.count { it.type == "urine" },
                    todayStoolCount = todayRecs.count { it.type == "stool" },
                    onNavigateToProfile = onNavigateToProfile
                )
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
                // 자정을 가로지르는 간격도 표시되도록 전체 기록(시간순 정렬)에서
                // 각 record.id → 직전(과거) 기록 매핑을 미리 구축한다.
                val previousByRecordId = buildMap<String, DiaperRecord> {
                    uiState.records.forEachIndexed { i, r ->
                        val older = uiState.records.getOrNull(i + 1)
                        if (older != null) put(r.id, older)
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                groupedRecords.forEach { (dateLabel, dayRecords) ->
                    item(key = "header_$dateLabel") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            DiaperDateSectionHeader(dateLabel)
                        }
                    }
                    itemsIndexed(
                        items = dayRecords,
                        key = { _, record -> record.id }
                    ) { _, record ->
                        val previousRecord = previousByRecordId[record.id]
                        DiaperRecordCard(
                            record = record,
                            intervalMinutes = previousRecord?.let { (record.timestamp - it.timestamp) / 60_000L },
                            onClick = { selectedRecord = record }
                        )
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
            containerColor = extendedColors.categoryDiaper,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "기저귀 기록 추가"
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
                text = "+ 버튼을 눌러\n첫 기저귀를 기록해보세요",
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
// 바텀시트: 기저귀 유형 선택
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DiaperEditBottomSheet(
    record: DiaperRecord,
    isNewRecord: Boolean,
    onUpdateType: (type: String?) -> Unit,
    onUpdateDetail: (detail: String?) -> Unit,
    onUpdateTimestamp: (timestamp: Long) -> Unit,
    onUpdateNote: (String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf(record.type) }
    var selectedDetail by remember { mutableStateOf(record.detail) }
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
            com.baby.feedingtracker.ui.components.CategorySheetHeader(
                emoji = "💧",
                title = "기저귀 기록",
                categoryColor = extendedColors.categoryDiaper,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 날짜 + 시간 편집 헤더
            com.baby.feedingtracker.ui.components.RecordDateTimeEditor(
                timestamp = currentTimestamp,
                titleSuffix = "기저귀 기록",
                onTimestampChange = {
                    currentTimestamp = it
                    onUpdateTimestamp(it)
                }
            )

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
                        // type 변경 시 기존 detail 의미 없음 → 비우기
                        if (selectedDetail != null) {
                            selectedDetail = null
                            onUpdateDetail(null)
                        }
                        onUpdateType(newType)
                    },
                    modifier = Modifier.weight(1f)
                )
                DiaperToggleButton(
                    text = "소변",
                    selected = selectedType == "urine",
                    onClick = {
                        val newType = if (selectedType == "urine") null else "urine"
                        selectedType = newType
                        if (selectedDetail != null) {
                            selectedDetail = null
                            onUpdateDetail(null)
                        }
                        onUpdateType(newType)
                    },
                    modifier = Modifier.weight(1f)
                )
                DiaperToggleButton(
                    text = "대변",
                    selected = selectedType == "stool",
                    onClick = {
                        val newType = if (selectedType == "stool") null else "stool"
                        selectedType = newType
                        if (selectedDetail != null) {
                            selectedDetail = null
                            onUpdateDetail(null)
                        }
                        onUpdateType(newType)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 상세(detail) chip 영역 — urine / stool 일 때만 표시
            val detailOptions: List<String>? = when (selectedType) {
                "urine" -> listOf("보통", "많음", "적음")
                "stool" -> listOf("노란색·묽음", "노란색·보통", "녹색", "갈색")
                else -> null
            }
            if (detailOptions != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "상세",
                    style = MaterialTheme.typography.labelMedium,
                    color = extendedColors.subtleText
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detailOptions.forEach { opt ->
                        DiaperDetailChip(
                            text = opt,
                            selected = selectedDetail == opt,
                            categoryColor = extendedColors.categoryDiaper,
                            onClick = {
                                val newDetail = if (selectedDetail == opt) null else opt
                                selectedDetail = newDetail
                                onUpdateDetail(newDetail)
                            }
                        )
                    }
                }
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
                    focusedBorderColor = extendedColors.categoryDiaper
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
private fun DiaperToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extendedColors.categoryDiaper,
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
// Detail Chip (소변/대변 상세 선택)
// ──────────────────────────────────────────────

@Composable
private fun DiaperDetailChip(
    text: String,
    selected: Boolean,
    categoryColor: Color,
    onClick: () -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bg = if (selected) {
        categoryColor.copy(alpha = if (isDark) 0.32f else 0.18f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
    }
    val fg = if (selected) categoryColor else LocalExtendedColors.current.subtleText
    val borderColor = if (selected) categoryColor.copy(alpha = 0.6f) else LocalExtendedColors.current.divider
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg
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

// ──────────────────────────────────────────────
// Diaper Hero Card (샌드 그라디언트)
// ──────────────────────────────────────────────

@Composable
private fun DiaperHeroCard(
    profile: com.baby.feedingtracker.data.BabyProfile?,
    daysOld: Int?,
    elapsedMinutes: Long?,
    lastRecord: com.baby.feedingtracker.data.DiaperRecord?,
    todayTotalCount: Int,
    todayUrineCount: Int,
    todayStoolCount: Int,
    onNavigateToProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFD4B59A), Color(0xFFC4956A))))
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
                        "${profile.name} 💧" else "아기 💧"
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
                ) { Text(text = "🧷", fontSize = 20.sp) }
            }
            Spacer(Modifier.height(14.dp))
            Text(text = "마지막 기저귀", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.80f), letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = formatDiaperElapsedTime(elapsedMinutes), fontSize = 36.sp,
                fontWeight = FontWeight.Black, color = Color.White,
                letterSpacing = (-1).sp, lineHeight = 40.sp)
            if (lastRecord != null) {
                val sub = formatDiaperType(lastRecord.type)
                if (sub != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = "💧 $sub", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.80f))
                }
            }
            if (todayTotalCount > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiaperHeroStatItem("${todayTotalCount}회", "오늘 총")
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.30f)))
                    DiaperHeroStatItem("${todayUrineCount}회", "소변")
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.30f)))
                    DiaperHeroStatItem("${todayStoolCount}회", "대변")
                }
            }
        }
    }
}

@Composable
private fun DiaperHeroStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.80f))
    }
}

// ──────────────────────────────────────────────
// Diaper Record Card
// ──────────────────────────────────────────────

@Composable
private fun DiaperRecordCard(
    record: com.baby.feedingtracker.data.DiaperRecord,
    intervalMinutes: Long?,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val extendedColors = LocalExtendedColors.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = timeFormat.format(Date(record.timestamp)), fontSize = 13.sp,
                fontWeight = FontWeight.Bold, color = extendedColors.subtleText, modifier = Modifier.width(38.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiaperTypePill(type = record.type)
                    if (!record.detail.isNullOrBlank()) {
                        Text(
                            text = record.detail,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!record.note.isNullOrBlank()) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Notes, contentDescription = null,
                            modifier = Modifier.size(14.dp), tint = extendedColors.subtleText)
                    }
                }
                if (!record.note.isNullOrBlank()) {
                    Text(text = record.note!!, fontSize = 12.sp, color = extendedColors.subtleText,
                        maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (intervalMinutes != null && intervalMinutes > 0) {
                Text(text = formatDiaperIntervalText(intervalMinutes), fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, color = extendedColors.subtleText, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun DiaperTypePill(type: String?) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val (bgColor, textColor, label) = when (type) {
        "urine" -> if (isDark) Triple(Color(0xFF2066B0).copy(alpha = 0.30f), Color(0xFF9DD3FF), "소변")
            else Triple(Color(0xFFD6ECFF), Color(0xFF2066B0), "소변")
        "stool" -> if (isDark) Triple(Color(0xFF8B5E3C).copy(alpha = 0.30f), Color(0xFFE0CCB5), "대변")
            else Triple(Color(0xFFF0E4D4), Color(0xFF8B5E3C), "대변")
        "diaper" -> if (isDark) Triple(Color(0xFF555555).copy(alpha = 0.30f), Color(0xFFCCCCCC), "기저귀")
            else Triple(Color(0xFFE8E8E8), Color(0xFF555555), "기저귀")
        else -> return
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bgColor)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}
