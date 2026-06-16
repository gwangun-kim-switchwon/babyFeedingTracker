package com.baby.feedingtracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import java.util.Calendar

// ──────────────────────────────────────────────
// Night Mode — UI 크기/레이아웃 레이어 (ThemeMode와 별개)
// 22:00~06:00 자동 활성화
// ──────────────────────────────────────────────

data class NightModeState(val isActive: Boolean) {
    /** 야간 모드 시 버튼 높이 1.2배 */
    val buttonHeightMultiplier: Float get() = if (isActive) 1.2f else 1.0f
}

val LocalNightMode = compositionLocalOf { NightModeState(isActive = false) }

private fun isNightHour(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= 22 || hour < 6
}

@Composable
fun NightModeProvider(content: @Composable () -> Unit) {
    val state = remember { NightModeState(isActive = isNightHour()) }
    CompositionLocalProvider(LocalNightMode provides state) {
        content()
    }
}
