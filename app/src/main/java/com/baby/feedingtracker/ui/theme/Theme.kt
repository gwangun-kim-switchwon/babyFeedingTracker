package com.baby.feedingtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.Calendar

// ──────────────────────────────────────────────
// Theme Mode
// ──────────────────────────────────────────────

enum class ThemeMode { SYSTEM, AUTO, LIGHT, DARK }

// ──────────────────────────────────────────────
// Soft Minimal Color Palette (Light)
// ──────────────────────────────────────────────

// Background & Surface
val CreamWhite = Color(0xFFFFF8F5)          // 메인 배경
val WarmWhite = Color(0xFFFFFBF8)           // 카드/섹션 배경
val SoftBeige = Color(0xFFFFF1EB)           // 미세한 구분 배경

// Accent / Primary
val MintGreen = Color(0xFF8CC9B0)           // 메인 액센트
val MintGreenLight = Color(0xFFB5DFCC)      // 연한 액센트
val MintGreenDark = Color(0xFF6BAF96)       // 눌림 상태

// Text
val DarkCharcoal = Color(0xFF2D2626)        // 주요 텍스트
val WarmGray = Color(0xFF8E8685)            // 서브 텍스트
val LightGray = Color(0xFFBEB8B6)           // 비활성 텍스트

// Utility
val SoftRed = Color(0xFFFF6B6B)             // 삭제/경고
val SoftRedBg = Color(0xFFFFE8E8)           // 삭제 배경
val DividerColor = Color(0xFFF0E8E4)        // 구분선

// Gradient colors
val GradientTop = Color(0xFFFFF8F5)
val GradientBottom = Color(0xFFFFF1EB)

// ──────────────────────────────────────────────
// Dark Color Palette
// ──────────────────────────────────────────────

val DarkBackground = Color(0xFF1A1A1E)
val DarkSurface = Color(0xFF242428)
val DarkSoftBackground = Color(0xFF2A2A2E)
val DarkMintGreen = Color(0xFF9AD4BC)
val DarkMintGreenLight = Color(0xFF6BAF96)
val DarkMintGreenDark = Color(0xFF7CC4A8)
val DarkTextPrimary = Color(0xFFE8E2E0)
val DarkTextSecondary = Color(0xFF9E9896)
val DarkDivider = Color(0xFF3A3A3E)
val DarkDeleteBg = Color(0xFF3D2020)
val DarkSoftRed = Color(0xFFFF8A8A)
val DarkGradientTop = Color(0xFF1A1A1E)
val DarkGradientBottom = Color(0xFF222226)

// ──────────────────────────────────────────────
// Extended Color Scheme (beyond Material3)
// ──────────────────────────────────────────────

@Immutable
data class ExtendedColors(
    val gradientTop: Color = GradientTop,
    val gradientBottom: Color = GradientBottom,
    val softBackground: Color = SoftBeige,
    val subtleText: Color = WarmGray,
    val divider: Color = DividerColor,
    val deleteBackground: Color = SoftRedBg,
    val deleteColor: Color = SoftRed,
    val fabContainer: Color = MintGreen,
    val statusConnected: Color = Color(0xFF4CAF50),
    val statusDisconnected: Color = SoftRed,
)

private val DefaultExtendedColors = ExtendedColors()

private val DarkExtendedColors = ExtendedColors(
    gradientTop = DarkGradientTop,
    gradientBottom = DarkGradientBottom,
    softBackground = DarkSoftBackground,
    subtleText = DarkTextSecondary,
    divider = DarkDivider,
    deleteBackground = DarkDeleteBg,
    deleteColor = DarkSoftRed,
    fabContainer = DarkMintGreen,
    statusConnected = Color(0xFF66BB6A),
    statusDisconnected = DarkSoftRed,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

// ──────────────────────────────────────────────
// Material3 Color Schemes
// ──────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = MintGreen,
    onPrimary = Color.White,
    primaryContainer = MintGreenLight,
    onPrimaryContainer = DarkCharcoal,
    secondary = MintGreenLight,
    onSecondary = DarkCharcoal,
    secondaryContainer = SoftBeige,
    onSecondaryContainer = DarkCharcoal,
    background = CreamWhite,
    onBackground = DarkCharcoal,
    surface = CreamWhite,
    onSurface = DarkCharcoal,
    surfaceVariant = WarmWhite,
    onSurfaceVariant = WarmGray,
    error = SoftRed,
    onError = Color.White,
    outline = DividerColor,
    outlineVariant = DividerColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMintGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkMintGreenDark,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkBackground,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    error = DarkSoftRed,
    onError = DarkBackground,
    outline = DarkDivider,
)

// ──────────────────────────────────────────────
// Typography Scale
// ──────────────────────────────────────────────

private val AppTypography = Typography(
    // 경과 시간 (매우 큰 디스플레이)
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
    ),
    // 보조 큰 텍스트
    displayMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    // 섹션 큰 제목
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    // 중간 제목
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // 작은 제목
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    // 기록 시간 텍스트
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // 본문
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // 버튼 / 라벨
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

// ──────────────────────────────────────────────
// Theme Composable
// ──────────────────────────────────────────────

@Composable
fun BabyFeedingTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.AUTO -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour >= 22 || hour < 6
        }
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (isDark) DarkExtendedColors else DefaultExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
