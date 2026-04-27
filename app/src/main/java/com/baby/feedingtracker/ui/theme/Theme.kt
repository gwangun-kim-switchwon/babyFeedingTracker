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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.baby.feedingtracker.R
import java.util.Calendar

// ──────────────────────────────────────────────
// Theme Mode
// ──────────────────────────────────────────────

enum class ThemeMode { SYSTEM, AUTO, LIGHT, DARK }

// ──────────────────────────────────────────────
// Mammamia Brand Palette (CEO 확정 — Warm Peach 다홍 톤)
// Primary Warm Peach / Secondary 코럴 / Tertiary Terracotta
// ──────────────────────────────────────────────

// Primary — Warm Peach (런처 아이콘과 통일)
val PeachPrimary = Color(0xFFFFB59A)
val PeachPrimaryDark = Color(0xFFFFC9B0)      // 다크모드 디밍 피치
val PeachContainerLight = Color(0xFFFFD4C4)   // FAB/primaryContainer 배경 (Light Peach)
val PeachContainerDark = Color(0xFF5A3015)    // Deep Peach
val PeachOnContainerLight = Color(0xFF3D1F00)
val PeachOnContainerDark = Color(0xFFFFD4C4)

// 하위호환 alias — 기존 호출처(MintPrimary 등) 보존을 위해 새 색상으로 매핑
val MintPrimary = PeachPrimary
val MintPrimaryDark = PeachPrimaryDark
val MintContainerLight = PeachContainerLight
val MintContainerDark = PeachContainerDark
val MintOnContainerLight = PeachOnContainerLight
val MintOnContainerDark = PeachOnContainerDark

// Secondary — Coral (하트, 알림, 마일스톤 액센트, 통일감 유지)
val CoralSecondary = Color(0xFFFF8A7A)
val CoralSecondaryDark = Color(0xFFFFB3A5)
val CoralContainerLight = Color(0xFFFFDAD2)
val CoralContainerDark = Color(0xFF5C1D12)
val CoralOnContainerLight = Color(0xFF3B0A02)
val CoralOnContainerDark = Color(0xFFFFDAD2)

// Tertiary — Terracotta (성장/성취 뱃지, Peach와 톤 통일)
val TerracottaTertiary = Color(0xFFF4A88A)
val TerracottaTertiaryDark = Color(0xFFF4B59A)
val TerracottaContainerLight = Color(0xFFFAD4C0)
val TerracottaContainerDark = Color(0xFF4A3500)
val TerracottaOnContainerLight = Color(0xFF261A00)
val TerracottaOnContainerDark = Color(0xFFFAD4C0)

// 하위호환 alias — 기존 AmberTertiary 호출처 보존
val AmberTertiary = TerracottaTertiary
val AmberTertiaryDark = TerracottaTertiaryDark
val AmberContainerLight = TerracottaContainerLight
val AmberContainerDark = TerracottaContainerDark
val AmberOnContainerLight = TerracottaOnContainerLight
val AmberOnContainerDark = TerracottaOnContainerDark

// ──────────────────────────────────────────────
// Light Surfaces (Cream 계열)
// ──────────────────────────────────────────────

val CreamWhite = Color(0xFFFFF8F2)           // 메인 배경 (R9 §3.1)
val WarmWhite = Color(0xFFFFFFFF)            // 카드 surface
val SurfaceContainerLight = Color(0xFFF4F0EB) // 벤토 카드
val SurfaceContainerHighLight = Color(0xFFEBE6E0) // 바텀시트
val SoftBeige = Color(0xFFFFF1EB)            // 미세한 구분 배경 (gradient bottom)

// ──────────────────────────────────────────────
// Dark Surfaces (R9 §3.3 — 블루 섞인 딥 블랙)
// 순수 블랙 금지, 눈 피로 감소 우선
// ──────────────────────────────────────────────

val BackgroundDark = Color(0xFF0A0A0F)
val SurfaceDark = Color(0xFF14141A)
val SurfaceContainerDark = Color(0xFF1C1C24)
val SurfaceContainerHighDark = Color(0xFF24242E)

// ──────────────────────────────────────────────
// Text & Ink
// ──────────────────────────────────────────────

val InkLight = Color(0xFF1C1B1F)              // 본문 텍스트 (Light)
val InkDark = Color(0xFFE8E6F0)               // 웜 화이트 (Dark) — 순백 금지
val WarmGray = Color(0xFF6E6A73)              // 서브 텍스트 (Light)
val WarmGrayDark = Color(0xFF9E99A6)          // 서브 텍스트 (Dark)
val MutedGrayLight = Color(0xFFBEB8B6)        // 비활성

// 하위호환 용도 (기존 호출처 보존)
val DarkCharcoal = InkLight
val LightGray = MutedGrayLight

// ──────────────────────────────────────────────
// Utility Colors
// ──────────────────────────────────────────────

val ErrorLight = Color(0xFFD84545)            // R9 §3.1 error
val ErrorDark = Color(0xFFFF9B8A)             // R9 §3.3 dark error
val ErrorBgLight = Color(0xFFFFE8E8)
val ErrorBgDark = Color(0xFF3D2020)
val OutlineLight = Color(0xFFCAC4CF)
val OutlineDark = Color(0xFF3F3F47)
val DividerLight = Color(0xFFF0E8E4)          // subtle 구분선
val DividerDark = Color(0xFF2A2A33)

// 하위호환 alias
val SoftRed = ErrorLight
val SoftRedBg = ErrorBgLight
val DividerColor = DividerLight
val DarkBackground = BackgroundDark
val DarkSurface = SurfaceDark
val DarkTextPrimary = InkDark
val DarkTextSecondary = WarmGrayDark
val DarkDivider = DividerDark
val DarkDeleteBg = ErrorBgDark
val DarkSoftRed = ErrorDark

// Gradient (홈 히어로 배경)
val GradientTop = CreamWhite
val GradientBottom = SoftBeige
val DarkGradientTop = BackgroundDark
val DarkGradientBottom = Color(0xFF16161E)

// ──────────────────────────────────────────────
// Category Colors (R9 §3.2)
// 카테고리별 액센트 바 & 카드 탭 컬러
// ──────────────────────────────────────────────

// Light mode
val CategoryFeedingLight = Color(0xFFFFB59A)      // Primary Warm Peach (가장 빈도 높음)
val CategoryDiaperLight = Color(0xFFD4B59A)       // Sand (Feeding과 분리)
val CategorySleepLight = Color(0xFFB3A3E8)        // 라벤더 (국내 사용자 인식)
val CategoryBathLight = Color(0xFF74C0FC)         // 스카이블루 (물 메타포)
val CategoryGrowthLight = Color(0xFFA8D4A0)       // Sage Green (Peach 보색)
val CategoryMedicalLight = Color(0xFFD84545)      // 소프트 레드 (주의)
val CategoryMilestoneLight = Color(0xFFFF8A7A)    // 코럴

// Dark mode — 채도/밝기 조정으로 눈부심 감소
val CategoryFeedingDark = Color(0xFFFFC9B0)
val CategoryDiaperDark = Color(0xFFE0CCB5)
val CategorySleepDark = Color(0xFFC7BBEF)
val CategoryBathDark = Color(0xFF9DD3FF)
val CategoryGrowthDark = Color(0xFFC5DCBA)
val CategoryMedicalDark = Color(0xFFFF8A8A)
val CategoryMilestoneDark = Color(0xFFFFB3A5)

/** 카테고리별 색상 토큰 — 스크린에서 참조 가능 */
object CategoryColors {
    val Feeding = CategoryFeedingLight
    val Diaper = CategoryDiaperLight
    val Sleep = CategorySleepLight
    val Bath = CategoryBathLight
    val Growth = CategoryGrowthLight
    val Medical = CategoryMedicalLight
    val Milestone = CategoryMilestoneLight
}

// ──────────────────────────────────────────────
// Extended Color Scheme (beyond Material3)
// 기존 호출처 호환: fabContainer, gradientTop/Bottom, softBackground,
// subtleText, divider, deleteBackground, deleteColor, statusConnected/Disconnected
// 신규 토큰: coralAccent, amberTertiary, category* (7개)
// ──────────────────────────────────────────────

@Immutable
data class ExtendedColors(
    val gradientTop: Color = GradientTop,
    val gradientBottom: Color = GradientBottom,
    val softBackground: Color = SoftBeige,
    val subtleText: Color = WarmGray,
    val divider: Color = DividerLight,
    val deleteBackground: Color = ErrorBgLight,
    val deleteColor: Color = ErrorLight,
    val fabContainer: Color = MintPrimary,
    val statusConnected: Color = Color(0xFF4CAF50),
    val statusDisconnected: Color = ErrorLight,
    // ── 신규: 브랜드 액센트 ──
    val coralAccent: Color = CoralSecondary,
    val amberTertiary: Color = AmberTertiary,
    // ── 신규: 카테고리 색상 ──
    val categoryFeeding: Color = CategoryFeedingLight,
    val categoryDiaper: Color = CategoryDiaperLight,
    val categorySleep: Color = CategorySleepLight,
    val categoryBath: Color = CategoryBathLight,
    val categoryGrowth: Color = CategoryGrowthLight,
    val categoryMedical: Color = CategoryMedicalLight,
    val categoryMilestone: Color = CategoryMilestoneLight,
)

private val LightExtendedColors = ExtendedColors()

private val DarkExtendedColors = ExtendedColors(
    gradientTop = DarkGradientTop,
    gradientBottom = DarkGradientBottom,
    softBackground = SurfaceContainerDark,
    subtleText = WarmGrayDark,
    divider = DividerDark,
    deleteBackground = ErrorBgDark,
    deleteColor = ErrorDark,
    fabContainer = MintPrimaryDark,
    statusConnected = Color(0xFF66BB6A),
    statusDisconnected = ErrorDark,
    coralAccent = CoralSecondaryDark,
    amberTertiary = AmberTertiaryDark,
    categoryFeeding = CategoryFeedingDark,
    categoryDiaper = CategoryDiaperDark,
    categorySleep = CategorySleepDark,
    categoryBath = CategoryBathDark,
    categoryGrowth = CategoryGrowthDark,
    categoryMedical = CategoryMedicalDark,
    categoryMilestone = CategoryMilestoneDark,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

// ──────────────────────────────────────────────
// Material3 Color Schemes (R9 §3.1, §3.3)
// ──────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = PeachPrimary,
    onPrimary = Color(0xFF3D1F00),
    primaryContainer = PeachContainerLight,
    onPrimaryContainer = PeachOnContainerLight,
    secondary = CoralSecondary,
    onSecondary = Color.White,
    secondaryContainer = CoralContainerLight,
    onSecondaryContainer = CoralOnContainerLight,
    tertiary = TerracottaTertiary,
    onTertiary = Color(0xFF3D1F00),
    tertiaryContainer = TerracottaContainerLight,
    onTertiaryContainer = TerracottaOnContainerLight,
    background = CreamWhite,
    onBackground = InkLight,
    surface = WarmWhite,
    onSurface = InkLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = WarmGray,
    surfaceTint = PeachPrimary,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorBgLight,
    onErrorContainer = Color(0xFF410002),
    outline = OutlineLight,
    outlineVariant = DividerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PeachPrimaryDark,
    onPrimary = Color(0xFF4A2200),
    primaryContainer = PeachContainerDark,
    onPrimaryContainer = PeachOnContainerDark,
    secondary = CoralSecondaryDark,
    onSecondary = Color(0xFF5C1D12),
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = CoralOnContainerDark,
    tertiary = TerracottaTertiaryDark,
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = TerracottaContainerDark,
    onTertiaryContainer = TerracottaOnContainerDark,
    background = BackgroundDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = WarmGrayDark,
    surfaceTint = PeachPrimaryDark,
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorBgDark,
    onErrorContainer = Color(0xFFFFDAD6),
    outline = OutlineDark,
    outlineVariant = DividerDark,
)

// ──────────────────────────────────────────────
// Pretendard Font Family (R9 §5 — "세련된 한국 디자인")
// androidx.compose.ui:ui-text-google-fonts 방식.
// Async 로딩 + 시스템 기본 폰트 자동 fallback → 크래시 없음.
// ──────────────────────────────────────────────
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val PretendardGoogleFont = GoogleFont("Pretendard")

val PretendardFontFamily = FontFamily(
    Font(googleFont = PretendardGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(googleFont = PretendardGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(googleFont = PretendardGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(googleFont = PretendardGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold, style = FontStyle.Normal),
)

// 숫자 가독성 향상을 위한 OpenType feature: tabular numbers.
// 수유량/경과시간 등 숫자 정렬 시 자리폭 고정 → 깜빡임 없음.
private const val TNUM = "tnum"

// ──────────────────────────────────────────────
// Typography Scale — R9 §5 기반
// headlineLarge/Medium 가독성 향상: tight letter-spacing 적용
// Pretendard fontFamily 전체 적용 + 숫자 표시용 TextStyle에 tnum 활성화
// ──────────────────────────────────────────────

private val AppTypography = Typography(
    // 경과 시간 (매우 큰 디스플레이) — 숫자 tnum 필수
    displayLarge = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = TNUM,
    ),
    // 보조 큰 텍스트
    displayMedium = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TNUM,
    ),
    displaySmall = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = TNUM,
    ),
    // 섹션 큰 제목 — tight letter-spacing으로 세련된 느낌
    headlineLarge = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = TNUM,
    ),
    // 중간 제목
    headlineMedium = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp,
    ),
    // 작은 제목
    headlineSmall = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    // 기록 시간 텍스트 — 시/분 정렬 위해 tnum
    titleLarge = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.15).sp,
        fontFeatureSettings = TNUM,
    ),
    titleMedium = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    // 본문
    bodyLarge = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    // 버튼 / 라벨 — 버튼 내 숫자 표기 시 tnum 이점
    labelLarge = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = TNUM,
    ),
    labelMedium = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ──────────────────────────────────────────────
// Theme Composable
// 이름 BabyFeedingTrackerTheme 유지 — 기존 호출처 보존
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
    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = MammamiaShapes,
            content = content
        )
    }
}
