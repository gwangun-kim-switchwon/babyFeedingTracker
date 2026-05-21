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

    val extendedColors = LocalExtendedColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // 1. 생후 배너
            item {
                GrowthHeroCard(
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

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp),
            containerColor = extendedColors.categoryGrowth,
            contentColor = Color.White,
            shape = androidx.compose.foundation.shape.CircleShape,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Add,
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

// ── 생후 히어로 카드 ───────────────────────────────────────
@Composable
private fun GrowthHeroCard(
    profile: BabyProfile?,
    daysOld: Int?,
    onNavigateToProfile: () -> Unit,
) {
    if (profile == null || profile.name.isBlank() || daysOld == null) {
        TextButton(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("프로필을 설정하세요", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        }
        return
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFFA8D4A0), Color(0xFF7BB87A))
                )
            )
            .clickable(onClick = onNavigateToProfile)
            .padding(20.dp),
    ) {
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
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "👶", fontSize = 20.sp)
            }
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
        GrowthSummaryCard("키", latestHeight?.let { "%.1f".format(it) }, "cm", "📏", Modifier.weight(1f))
        GrowthSummaryCard("몸무게", latestWeight?.let { "%.1f".format(it) }, "kg", "⚖️", Modifier.weight(1f))
        GrowthSummaryCard("머리둘레", latestHead?.let { "%.1f".format(it) }, "cm", "📐", Modifier.weight(1f))
    }
}

@Composable
private fun GrowthSummaryCard(label: String, value: String?, unit: String, icon: String, modifier: Modifier) {
    androidx.compose.material3.Card(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value ?: "--",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (value != null) Color(0xFF7BB87A) else Color(0xFF9E9E9E),
            )
            Text(
                text = "$label ($unit)",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ── 기록 카드 ──────────────────────────────────────────────
@Composable
private fun GrowthRecordCard(record: GrowthRecord, onDelete: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN) }
    val parts = buildList {
        record.weightKg?.let { add("몸무게 ${"%.1f".format(it)} kg") }
        record.heightCm?.let { add("키 ${"%.1f".format(it)} cm") }
        record.headCm?.let   { add("머리둘레 ${"%.1f".format(it)} cm") }
    }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                            .background(Color(0xFFD4EED0))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(text = "성장", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A7C35))
                    }
                    Text(
                        text = parts.joinToString(" · "),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1B1F),
                    )
                }
                val dateStr = dateFmt.format(Date(record.timestamp))
                val sub = if (record.note != null) "$dateStr · ${record.note}" else dateStr
                Text(
                    text = sub,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "삭제",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(18.dp)
                )
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
