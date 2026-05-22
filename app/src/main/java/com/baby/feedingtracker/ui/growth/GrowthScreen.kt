package com.baby.feedingtracker.ui.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.BabyProfile
import com.baby.feedingtracker.data.GrowthRecord
import com.baby.feedingtracker.ui.components.RecordDateTimeEditor
import com.baby.feedingtracker.ui.profile.BabyProfileViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

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

    val extendedColors = LocalExtendedColors.current

    // 직전 측정 대비 변화량 (records[0] vs records[1])
    val records = uiState.records
    val latestRecord = records.firstOrNull()
    val previousRecord = records.getOrNull(1)
    val heightDelta = computeDelta(latestRecord?.heightCm, previousRecord?.heightCm)
    val weightDelta = computeDelta(latestRecord?.weightKg, previousRecord?.weightKg)
    val headDelta = computeDelta(latestRecord?.headCm, previousRecord?.headCm)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // 1. Hero 카드
            item {
                GrowthHeroCard(
                    profile = profile,
                    daysOld = daysOld,
                    latestRecord = latestRecord,
                    onNavigateToProfile = onNavigateToProfile,
                )
            }

            // 2. 요약 카드 Row
            item {
                GrowthSummaryRow(
                    latestHeight = uiState.latestHeight,
                    latestWeight = uiState.latestWeight,
                    latestHead   = uiState.latestHead,
                    heightDelta  = heightDelta,
                    weightDelta  = weightDelta,
                    headDelta    = headDelta,
                )
            }

            // 3. 섹션 헤더
            item {
                Text(
                    text = "성장 기록",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    color = extendedColors.subtleText,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }

            // 4. 기록 타임라인
            if (uiState.records.isEmpty()) {
                item {
                    Text(
                        text = "아직 성장 기록이 없어요\n+ 버튼을 눌러 첫 기록을 추가하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extendedColors.subtleText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(uiState.records, key = { it.id }) { record ->
                    GrowthRecordCard(
                        record   = record,
                        profile  = profile,
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

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp),
            containerColor = extendedColors.categoryGrowth,
            contentColor = Color.White,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "성장 기록 추가",
            )
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }

    if (showSheet) {
        GrowthBottomSheet(
            onDismiss = { showSheet = false },
            onSave    = { record -> viewModel.addRecord(record); showSheet = false },
        )
    }
}

private fun computeDelta(latest: Float?, previous: Float?): Float? {
    if (latest == null || previous == null) return null
    val diff = latest - previous
    if (abs(diff) < 0.05f) return null
    return diff
}

// ── 생후 히어로 카드 ───────────────────────────────────────
@Composable
private fun GrowthHeroCard(
    profile: BabyProfile?,
    daysOld: Int?,
    latestRecord: GrowthRecord?,
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

    val dateFmt = remember { SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN) }
    val now = remember { System.currentTimeMillis() }
    val recordDateText = latestRecord?.let { dateFmt.format(Date(it.timestamp)) } ?: "측정 기록 없음"
    val recordSubText = latestRecord?.let {
        val days = TimeUnit.MILLISECONDS.toDays(now - it.timestamp).toInt()
        when {
            days <= 0 -> "오늘 측정"
            days == 1 -> "어제 측정"
            else -> "${days}일 전 측정"
        }
    } ?: "기록을 추가해주세요"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFA8D4A0), Color(0xFF7BB87A))
                )
            )
            .clickable(onClick = onNavigateToProfile)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // hero-top-row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${profile.name} 📏",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "생후 ${daysOld}일",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🌱", fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // hero-label
            Text(
                text = "최근 측정",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = Color.White.copy(alpha = 0.80f),
            )
            Spacer(Modifier.height(4.dp))

            // hero-time
            Text(
                text = recordDateText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 30.8.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))

            // hero-sub
            Text(
                text = recordSubText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.80f),
            )

            // hero-sub-row
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroSubItem(
                    value = latestRecord?.heightCm?.let { "%.1fcm".format(it) } ?: "--",
                    label = "키",
                    modifier = Modifier.weight(1f),
                )
                HeroDivider()
                HeroSubItem(
                    value = latestRecord?.weightKg?.let { "%.1fkg".format(it) } ?: "--",
                    label = "몸무게",
                    modifier = Modifier.weight(1f),
                )
                HeroDivider()
                HeroSubItem(
                    value = latestRecord?.headCm?.let { "%.1fcm".format(it) } ?: "--",
                    label = "머리둘레",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroSubItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.80f),
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color.White.copy(alpha = 0.30f))
    )
}

// ── 요약 카드 3개 ──────────────────────────────────────────
@Composable
private fun GrowthSummaryRow(
    latestHeight: Float?,
    latestWeight: Float?,
    latestHead: Float?,
    heightDelta: Float?,
    weightDelta: Float?,
    headDelta: Float?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GrowthSummaryCard(
            icon = "📏",
            value = latestHeight?.let { "%.1f".format(it) },
            unit = "cm",
            label = "키",
            delta = heightDelta,
            deltaUnit = "cm",
            modifier = Modifier.weight(1f),
        )
        GrowthSummaryCard(
            icon = "⚖️",
            value = latestWeight?.let { "%.1f".format(it) },
            unit = "kg",
            label = "몸무게",
            delta = weightDelta,
            deltaUnit = "kg",
            modifier = Modifier.weight(1f),
        )
        GrowthSummaryCard(
            icon = "🎯",
            value = latestHead?.let { "%.1f".format(it) },
            unit = "cm",
            label = "머리둘레",
            delta = headDelta,
            deltaUnit = "cm",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GrowthSummaryCard(
    icon: String,
    value: String?,
    unit: String,
    label: String,
    delta: Float?,
    deltaUnit: String,
    modifier: Modifier = Modifier,
) {
    val extendedColors = LocalExtendedColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value ?: "--",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (value != null) {
                    Text(
                        text = unit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.subtleText,
                        modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
                    )
                }
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = extendedColors.subtleText,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (delta != null) {
                val sign = if (delta >= 0) "+" else ""
                val arrow = if (delta >= 0) " ↑" else " ↓"
                Text(
                    text = "$sign${"%.1f".format(delta)}$deltaUnit$arrow",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.categoryGrowth,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ── 기록 카드 ──────────────────────────────────────────────
@Composable
private fun GrowthRecordCard(
    record: GrowthRecord,
    profile: BabyProfile?,
    onDelete: () -> Unit,
) {
    val extendedColors = LocalExtendedColors.current
    val recordDateFmt = remember { SimpleDateFormat("M월 d일", Locale.KOREAN) }
    val recordDateText = recordDateFmt.format(Date(record.timestamp))

    val daysOldAtRecord = profile?.takeIf { it.birthDate > 0L }?.let {
        val diffMs = record.timestamp - it.birthDate
        TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
    }

    val parts = buildList {
        record.heightCm?.let { add("%.1fcm".format(it)) }
        record.weightKg?.let { add("%.1fkg".format(it)) }
        record.headCm?.let   { add("%.1fcm".format(it)) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 좌측: record-time
            Text(
                text = recordDateText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = extendedColors.subtleText,
                modifier = Modifier.widthIn(min = 50.dp),
            )

            // 가운데: record-body
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFD4EED0))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "측정",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A7C35),
                    )
                }
                if (parts.isNotEmpty()) {
                    Text(
                        text = parts.joinToString(" · "),
                        fontSize = 12.sp,
                        color = extendedColors.subtleText,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                if (!record.note.isNullOrBlank()) {
                    Text(
                        text = record.note,
                        fontSize = 11.sp,
                        color = extendedColors.subtleText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // 우측: 생후 N일 + 삭제 버튼
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (daysOldAtRecord != null && daysOldAtRecord >= 0) {
                    Text(
                        text = "생후 ${daysOldAtRecord}일",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = extendedColors.subtleText,
                        textAlign = TextAlign.End,
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "삭제",
                        tint = extendedColors.subtleText,
                        modifier = Modifier.size(16.dp)
                    )
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

    val extendedColors = LocalExtendedColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.baby.feedingtracker.ui.components.CategorySheetHeader(
                emoji = "📏",
                title = "성장 기록",
                categoryColor = extendedColors.categoryGrowth,
            )

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
                colors = ButtonDefaults.buttonColors(
                    containerColor = extendedColors.categoryGrowth,
                    contentColor = Color.White,
                ),
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
