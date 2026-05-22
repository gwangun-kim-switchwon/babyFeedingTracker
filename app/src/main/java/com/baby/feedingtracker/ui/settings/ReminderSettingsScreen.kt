package com.baby.feedingtracker.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

/**
 * 수유 리마인더 설정 화면.
 * - 상단: 백 버튼 + "수유 알림 설정" 타이틀
 * - 카드 1: 알림 ON/OFF Switch
 * - 카드 2: 간격 슬라이더 (1~6h, step 1)
 * - 카드 3: 야간 방해금지 (Switch + 시작/종료 TimePicker)
 *
 * 모든 색상은 MaterialTheme.colorScheme / LocalExtendedColors 토큰 사용 (다크모드 자동 대응).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    viewModel: ReminderSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "수유 알림 설정",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 카드 1: 알림 ON/OFF ──
            SettingCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "수유 리마인더",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "마지막 수유 후 일정 시간 경과 시 알려드려요",
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors.subtleText,
                        )
                    }
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = { viewModel.toggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = extendedColors.categoryFeeding,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }

            // ── 카드 2: 간격 슬라이더 ──
            SettingCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "알림 간격",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${state.intervalHours}시간마다",
                            style = MaterialTheme.typography.titleMedium,
                            color = extendedColors.categoryFeeding,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = state.intervalHours.toFloat(),
                        onValueChange = { v -> viewModel.setInterval(v.toInt().coerceIn(1, 6)) },
                        valueRange = 1f..6f,
                        steps = 4, // 1,2,3,4,5,6 → 사이 step 4개
                        enabled = state.reminderEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = extendedColors.categoryFeeding,
                            activeTrackColor = extendedColors.categoryFeeding,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "1시간",
                            style = MaterialTheme.typography.labelSmall,
                            color = extendedColors.subtleText,
                        )
                        Text(
                            text = "6시간",
                            style = MaterialTheme.typography.labelSmall,
                            color = extendedColors.subtleText,
                        )
                    }
                }
            }

            // ── 카드 3: 야간 방해금지 ──
            SettingCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "야간 방해금지",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "설정한 시간대에는 알림을 보내지 않아요",
                                style = MaterialTheme.typography.bodySmall,
                                color = extendedColors.subtleText,
                            )
                        }
                        Switch(
                            checked = state.dndEnabled,
                            onCheckedChange = { viewModel.toggleDnd(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = extendedColors.categoryFeeding,
                            ),
                        )
                    }
                    if (state.dndEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TimeChip(
                                label = "시작",
                                hour = state.dndStartHour,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, _ -> viewModel.setDndRange(h, state.dndEndHour) },
                                        state.dndStartHour,
                                        0,
                                        true,
                                    ).show()
                                },
                            )
                            TimeChip(
                                label = "종료",
                                hour = state.dndEndHour,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, _ -> viewModel.setDndRange(state.dndStartHour, h) },
                                        state.dndEndHour,
                                        0,
                                        true,
                                    ).show()
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeChip(
    label: String,
    hour: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val extendedColors = LocalExtendedColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = extendedColors.subtleText,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

