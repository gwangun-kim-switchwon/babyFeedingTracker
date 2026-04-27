package com.baby.feedingtracker.ui.feeding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.SharingState
import com.baby.feedingtracker.ui.ShareBottomSheet
import com.baby.feedingtracker.ui.profile.BabyProfileBanner
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.style.TextDecoration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingScreen(
    viewModel: FeedingViewModel,
    babyProfileViewModel: BabyProfileViewModel,
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastAddedRecord by viewModel.lastAddedRecord.collectAsStateWithLifecycle()
    val sharingState by viewModel.sharingState.collectAsStateWithLifecycle()
    val isGoogleLoggedIn by viewModel.isGoogleLoggedIn.collectAsStateWithLifecycle()
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val sharingError by viewModel.sharingError.collectAsStateWithLifecycle()
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

    var selectedRecord by remember { mutableStateOf<FeedingRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<FeedingRecord?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }
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
        DeleteConfirmDialog(
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
        RecordEditBottomSheet(
            record = record,
            isNewRecord = isNewRecord,
            onUpdateType = { type, amountMl, leftMin, rightMin ->
                viewModel.updateRecordType(record.id, type, amountMl, leftMin, rightMin)
            },
            onUpdateTimestamp = { newTimestamp ->
                viewModel.updateRecordTimestamp(record.id, newTimestamp)
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

    // Share bottom sheet
    if (showShareSheet) {
        ShareBottomSheet(
            sharingState = sharingState,
            isGoogleLoggedIn = isGoogleLoggedIn,
            inviteCode = inviteCode,
            sharingError = sharingError,
            googleAuthHelper = googleAuthHelper,
            googleSignInLauncher = googleSignInLauncher,
            onGenerateCode = { viewModel.generateInviteCode() },
            onRedeemCode = { code ->
                viewModel.redeemInviteCode(code)
            },
            onClearError = { viewModel.clearSharingError() },
            onDismiss = {
                showShareSheet = false
                viewModel.clearInviteCode()
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

            // -- 상단: 경과 시간 영역 + 공유 아이콘 --
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    ElapsedTimeSection(
                        elapsedMinutes = uiState.elapsedMinutes,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Share icon with status dot
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        IconButton(
                            onClick = { showShareSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "공유",
                                tint = LocalExtendedColors.current.subtleText
                            )
                        }
                        // Status dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (sharingState is SharingState.Connected) {
                                        extendedColors.statusConnected
                                    } else {
                                        extendedColors.statusDisconnected
                                    }
                                )
                        )
                    }
                }
            }

            // -- 중단: 기록 목록 --
            if (uiState.records.isEmpty()) {
                item {
                    EmptyState(
                        modifier = Modifier
                            .fillParentMaxHeight(0.5f)
                            .fillMaxWidth()
                    )
                }
            } else {
                val groupedRecords = groupRecordsByDate(uiState.records)
                // 자정을 가로지르는 간격도 표시되도록 전체 기록(시간순 정렬)에서
                // 각 record.id → 직전(과거) 기록 매핑을 미리 구축한다.
                val previousByRecordId = buildMap<String, FeedingRecord> {
                    uiState.records.forEachIndexed { i, r ->
                        val older = uiState.records.getOrNull(i + 1)
                        if (older != null) put(r.id, older)
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp).padding(horizontal = 24.dp)) }

                groupedRecords.forEach { (dateLabel, dayRecords) ->
                    item(key = "header_$dateLabel") {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DateSectionHeader(dateLabel)
                        }
                    }
                    item(key = "stats_$dateLabel") {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DailyStats(dayRecords)
                        }
                    }
                    itemsIndexed(
                        items = dayRecords,
                        key = { _, record -> record.id }
                    ) { index, record ->
                        val isLast = index == dayRecords.lastIndex
                        val previousRecord = previousByRecordId[record.id]

                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            TimelineRecordRow(
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
                contentDescription = "수유 기록 추가"
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
private fun ElapsedTimeSection(
    elapsedMinutes: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        if (elapsedMinutes != null) {
            Text(
                text = "마지막 수유",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = formatElapsedTimeDisplay(elapsedMinutes),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

    }
}

// ──────────────────────────────────────────────
// 빈 상태
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
                text = "+ 버튼을 눌러\n첫 수유를 기록해보세요",
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
// 일일 통계
// ──────────────────────────────────────────────

@Composable
private fun DailyStats(records: List<FeedingRecord>) {
    val breastCount = records.count { it.type == "breast" }
    val totalBreastMin = records.filter { it.type == "breast" }.sumOf { (it.leftMin ?: 0) + (it.rightMin ?: 0) }
    val formulaCount = records.count { it.type == "formula" }
    val totalCount = records.size
    val totalFormulaMl = records.filter { it.type == "formula" }.mapNotNull { it.amountMl }.sum()
    val pumpedCount = records.count { it.type == "pumped" }
    val totalPumpedMl = records.filter { it.type == "pumped" }.mapNotNull { it.amountMl }.sum()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(label = "총", value = "${totalCount}회")
        if (breastCount > 0) {
            val minText = if (totalBreastMin > 0) " · ${totalBreastMin}분" else ""
            StatChip(label = "모유", value = "${breastCount}회$minText")
        }
        if (formulaCount > 0) {
            val mlText = if (totalFormulaMl > 0) " · ${totalFormulaMl}ml" else ""
            StatChip(label = "분유", value = "${formulaCount}회$mlText")
        }
        if (pumpedCount > 0) {
            val mlText = if (totalPumpedMl > 0) " · ${totalPumpedMl}ml" else ""
            StatChip(label = "유축", value = "${pumpedCount}회$mlText")
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
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
private fun TimelineRecordRow(
    record: FeedingRecord,
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
            // 동그라미
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            // 세로선
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
                // 시간
                Text(
                    text = timeFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // 수유 종류 표시
                val typeText = formatRecordType(record)
                if (typeText != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = typeText,
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

            // 간격 표시
            if (intervalMinutes != null && intervalMinutes > 0) {
                Text(
                    text = formatIntervalText(intervalMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.subtleText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 바텀시트: 수유 종류/용량 편집
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordEditBottomSheet(
    record: FeedingRecord,
    isNewRecord: Boolean,
    onUpdateType: (type: String?, amountMl: Int?, leftMin: Int?, rightMin: Int?) -> Unit,
    onUpdateTimestamp: (Long) -> Unit,
    onUpdateNote: (String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf(record.type) }
    var selectedAmount by remember { mutableStateOf(record.amountMl) }
    var selectedLeftMin by remember { mutableStateOf(record.leftMin) }
    var selectedRightMin by remember { mutableStateOf(record.rightMin) }
    val amounts = listOf(30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160)
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
            // 날짜 + 시간 편집 헤더
            com.baby.feedingtracker.ui.components.RecordDateTimeEditor(
                timestamp = currentTimestamp,
                titleSuffix = "수유 기록",
                onTimestampChange = {
                    currentTimestamp = it
                    onUpdateTimestamp(it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 모유 / 분유 / 유축 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleButton(
                    text = "모유",
                    selected = selectedType == "breast",
                    onClick = {
                        val newType = if (selectedType == "breast") null else "breast"
                        selectedType = newType
                        selectedAmount = null
                        onUpdateType(newType, null, selectedLeftMin, selectedRightMin)
                    },
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    text = "분유",
                    selected = selectedType == "formula",
                    onClick = {
                        val newType = if (selectedType == "formula") null else "formula"
                        selectedType = newType
                        if (newType != "formula") selectedAmount = null
                        onUpdateType(newType, if (newType == "formula") selectedAmount else null, null, null)
                    },
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    text = "유축",
                    selected = selectedType == "pumped",
                    onClick = {
                        val newType = if (selectedType == "pumped") null else "pumped"
                        selectedType = newType
                        if (newType != "pumped") selectedAmount = null
                        onUpdateType(newType, if (newType == "pumped") selectedAmount else null, null, null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 분유/유축 용량 선택 (분유 또는 유축 선택 시 표시)
            AnimatedVisibility(visible = selectedType == "formula" || selectedType == "pumped") {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "용량",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 첫 줄: 30~90
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        amounts.take(7).forEach { amount ->
                            AmountButton(
                                amount = amount,
                                selected = selectedAmount == amount,
                                onClick = {
                                    val newAmount = if (selectedAmount == amount) null else amount
                                    selectedAmount = newAmount
                                    onUpdateType(selectedType, newAmount, null, null)
                                    if (isNewRecord && newAmount != null) onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // 둘째 줄: 100~160
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        amounts.drop(7).forEach { amount ->
                            AmountButton(
                                amount = amount,
                                selected = selectedAmount == amount,
                                onClick = {
                                    val newAmount = if (selectedAmount == amount) null else amount
                                    selectedAmount = newAmount
                                    onUpdateType(selectedType, newAmount, null, null)
                                    if (isNewRecord && newAmount != null) onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 모유 상세 선택 (모유 선택 시만 표시)
            AnimatedVisibility(visible = selectedType == "breast") {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 왼쪽 수유
                    Text(
                        text = "왼쪽",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (5..15).forEach { min ->
                            AmountButton(
                                amount = min,
                                selected = selectedLeftMin == min,
                                onClick = {
                                    selectedLeftMin = if (selectedLeftMin == min) null else min
                                    onUpdateType(selectedType, null, selectedLeftMin, selectedRightMin)
                                    if (isNewRecord && selectedLeftMin != null && selectedRightMin != null) onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 오른쪽 수유
                    Text(
                        text = "오른쪽",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (5..15).forEach { min ->
                            AmountButton(
                                amount = min,
                                selected = selectedRightMin == min,
                                onClick = {
                                    selectedRightMin = if (selectedRightMin == min) null else min
                                    onUpdateType(selectedType, null, selectedLeftMin, selectedRightMin)
                                    if (isNewRecord && selectedLeftMin != null && selectedRightMin != null) onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 삭제 버튼
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
// 토글 버튼 (모유/분유)
// ──────────────────────────────────────────────

@Composable
private fun ToggleButton(
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
// 용량 버튼
// ──────────────────────────────────────────────

@Composable
private fun AmountButton(
    amount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)

    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            contentPadding = paddingValues,
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = "$amount",
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = paddingValues
        ) {
            Text(
                text = "$amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
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
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분 전"
        hours > 0 -> "${hours}시간 전"
        minutes > 0 -> "${minutes}분 전"
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

private fun formatRecordType(record: FeedingRecord): String? {
    return when (record.type) {
        "breast" -> {
            val parts = mutableListOf("모유")
            record.leftMin?.let { parts.add("왼 ${it}분") }
            record.rightMin?.let { parts.add("오 ${it}분") }
            parts.joinToString(" · ")
        }
        "formula" -> {
            if (record.amountMl != null) "분유 · ${record.amountMl}ml"
            else "분유"
        }
        "pumped" -> {
            if (record.amountMl != null) "유축 · ${record.amountMl}ml"
            else "유축"
        }
        else -> null
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
