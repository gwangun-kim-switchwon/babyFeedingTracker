package com.baby.feedingtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────
// Soft Minimal Color Palette
// ──────────────────────────────────────────────

// Background & Surface
val CreamWhite = Color(0xFFFFF8F5)          // 메인 배경
val WarmWhite = Color(0xFFFFFBF8)           // 카드/섹션 배경
val SoftBeige = Color(0xFFFFF1EB)           // 미세한 구분 배경

// Accent / Primary
val SoftCoral = Color(0xFFFF8A76)           // 메인 액센트
val SoftCoralLight = Color(0xFFFFB4A2)      // 연한 액센트
val SoftCoralDark = Color(0xFFE8735F)       // 눌림 상태

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
)

private val DefaultExtendedColors = ExtendedColors()

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

// ──────────────────────────────────────────────
// Material3 Color Scheme
// ──────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = SoftCoral,
    onPrimary = Color.White,
    primaryContainer = SoftCoralLight,
    onPrimaryContainer = DarkCharcoal,
    secondary = SoftCoralLight,
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
    darkTheme: Boolean = false, // 라이트 모드만 우선 지원
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalExtendedColors provides DefaultExtendedColors) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
