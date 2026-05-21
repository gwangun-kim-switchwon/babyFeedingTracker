package com.baby.feedingtracker.ui.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.BabyProfile
import com.baby.feedingtracker.data.GrowthRecord
import com.baby.feedingtracker.ui.components.LiquidGlassFab
import com.baby.feedingtracker.ui.components.RecordDateTimeEditor
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GrowthScreen(
    viewModel: GrowthViewModel,
    babyProfileViewModel: BabyProfileViewModel,
    onNavigateToProfile: () -> Unit,
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val profile      by babyProfileViewModel.profile.collectAsStateWithLifecycle()
    val daysOld      by babyProfileViewModel.daysOld.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showSheet    by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHost.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = { LiquidGlassFab(onClick = { showSheet = true }) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            LocalExtendedColors.current.gradientTop,
                            LocalExtendedColors.current.gradientBottom,
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(padding)
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                // 1. 생후 배너
                item {
                    GrowthDaysBanner(
                        profile = profile,
                        daysOld = daysOld,
                        onNavigateToProfile = onNavigateToProfile,
                    )
                }

                // 2. 최신값 요약 카드
                item {
                    GrowthSummaryRow(
                        latestHeight = uiState.latestHeight,
                        latestWeight = uiState.latestWeight,
                        latestHead   = uiState.latestHead,
                    )
                }

                // 3. 기록 타임라인
                if (uiState.records.isEmpty()) {
                    item {
                        Text(
                            text = "아직 성장 기록이 없어요\n+ 버튼을 눌러 첫 기록을 추가하세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalExtendedColors.current.subtleText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                } else {
                    items(uiState.records, key = { it.id }) { record ->
                        GrowthRecordCard(
                            record   = record,
                            onDelete = { viewModel.deleteRecord(record) },
                        )
                    }
                    if (uiState.hasMoreData) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMore() }
                            if (uiState.isLoadingMore) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        GrowthBottomSheet(
            onDismiss = { showSheet = false },
            onSave    = { record -> viewModel.addRecord(record); showSheet = false },
        )
    }
}

// ── 생후 배너 ─────────────────────────────────────────────
@Composable
private fun GrowthDaysBanner(
    profile: BabyProfile?,
    daysOld: Int?,
    onNavigateToProfile: () -> Unit,
) {
    if (profile == null || profile.name.isBlank() || daysOld == null) {
        TextButton(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("프로필을 설정하세요", color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                    )
                )
            )
            .clickable(onClick = onNavigateToProfile)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text  = profile.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text       = "생후 ${daysOld}일",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
            Text(text = "👶", fontSize = 34.sp)
        }
    }
}

// ── 요약 카드 3개 ──────────────────────────────────────────
@Composable
private fun GrowthSummaryRow(
    latestHeight: Float?,
    latestWeight: Float?,
    latestHead: Float?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GrowthSummaryCard("키",      latestHeight?.let { "%.1f".format(it) }, "cm", Modifier.weight(1f))
        GrowthSummaryCard("몸무게",  latestWeight?.let { "%.1f".format(it) }, "kg", Modifier.weight(1f))
        GrowthSummaryCard("머리둘레",latestHead?.let   { "%.1f".format(it) }, "cm", Modifier.weight(1f))
    }
}

@Composable
private fun GrowthSummaryCard(label: String, value: String?, unit: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier                = Modifier.padding(10.dp),
            horizontalAlignment     = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = LocalExtendedColors.current.subtleText)
            Spacer(Modifier.height(4.dp))
            Text(
                text  = value ?: "--",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (value != null) MaterialTheme.colorScheme.primary
                        else LocalExtendedColors.current.subtleText,
            )
            Text(unit, style = MaterialTheme.typography.labelSmall,
                color = LocalExtendedColors.current.subtleText)
        }
    }
}

// ── 타임라인 카드 ──────────────────────────────────────────
@Composable
private fun GrowthRecordCard(record: GrowthRecord, onDelete: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN) }
    val parts = buildList {
        record.weightKg?.let { add("몸무게 ${"%.1f".format(it)} kg") }
        record.heightCm?.let { add("키 ${"%.1f".format(it)} cm")    }
        record.headCm?.let   { add("머리둘레 ${"%.1f".format(it)} cm") }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 타임라인 도트
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(Modifier.width(2.dp).height(40.dp).background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ))
        }
        Spacer(Modifier.width(12.dp))
        Card(
            modifier  = Modifier.weight(1f),
            shape     = RoundedCornerShape(10.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = parts.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    val dateStr = dateFmt.format(Date(record.timestamp))
                    val sub = if (record.note != null) "$dateStr · ${record.note}" else dateStr
                    Text(sub, style = MaterialTheme.typography.bodySmall,
                        color = LocalExtendedColors.current.subtleText)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "삭제",
                        tint = LocalExtendedColors.current.subtleText, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── 기록 추가 BottomSheet ──────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrowthBottomSheet(onDismiss: () -> Unit, onSave: (GrowthRecord) -> Unit) {
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var headInput   by remember { mutableStateOf("") }
    var noteInput   by remember { mutableStateOf("") }
    var timestampMs by remember { mutableStateOf(System.currentTimeMillis()) }

    val hasAny = heightInput.isNotBlank() || weightInput.isNotBlank() || headInput.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("성장 기록 추가", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            MeasurementField("키 (cm)",       heightInput, "예) 56.2") { heightInput = it }
            MeasurementField("몸무게 (kg)",   weightInput, "예) 4.8")  { weightInput = it }
            MeasurementField("머리둘레 (cm)", headInput,   "예) 37.5") { headInput = it }

            RecordDateTimeEditor(
                timestamp         = timestampMs,
                titleSuffix       = "성장 기록",
                onTimestampChange = { timestampMs = it },
            )

            OutlinedTextField(
                value         = noteInput,
                onValueChange = { noteInput = it },
                label         = { Text("메모 (선택)") },
                placeholder   = { Text("예) 소아과 방문") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            Button(
                onClick = {
                    onSave(GrowthRecord(
                        timestamp = timestampMs,
                        heightCm  = heightInput.toFloatOrNull(),
                        weightKg  = weightInput.toFloatOrNull(),
                        headCm    = headInput.toFloatOrNull(),
                        note      = noteInput.takeIf { it.isNotBlank() },
                    ))
                },
                enabled  = hasAny,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("저장")
            }
        }
    }
}

@Composable
private fun MeasurementField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label) },
        placeholder    = { Text(placeholder) },
        modifier       = Modifier.fillMaxWidth(),
        singleLine     = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
