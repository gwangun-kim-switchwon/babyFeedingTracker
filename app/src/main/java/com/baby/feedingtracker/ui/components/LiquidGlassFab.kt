package com.baby.feedingtracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 2026 트렌드 "Liquid Glass" 느낌의 FloatingActionButton.
 *
 * 3층 레이어 구조로 평면 FAB 대비 depth 감과 세련된 분위기를 연출한다.
 *  - Layer 1: primary 색상 85% alpha 배경 원형 (Liquid 필링)
 *  - Layer 2: primary → primaryContainer 1dp 그라데이션 보더 (유리 테두리)
 *  - Layer 3: 중앙 흰색 아이콘
 *
 * minSdk 26 호환. `graphicsLayer { renderEffect }`(API 31+) 및 Haze 라이브러리 미사용.
 * Alpha 블렌딩 + 그라데이션 + Box 스택으로 블러 없이 유리 질감 흉내.
 *
 * 사용 예시 (v1.3 이후 점진 교체):
 * ```
 * LiquidGlassFab(
 *     onClick = { viewModel.onAddClick() },
 *     icon = MammamiaIcons.Material.Add,
 *     contentDescription = "기록 추가",
 * )
 * ```
 */
@Composable
fun LiquidGlassFab(
    onClick: () -> Unit,
    icon: ImageVector = Icons.Rounded.Add,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    // Press 시 스케일 애니메이션 (0.95f) — 부드러운 spring
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "fab-scale",
    )

    // Elevation: default 6dp, pressed 2dp
    val elevation: Dp = if (isPressed) 2.dp else 6.dp

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            // elevation 그림자 (유리 느낌 강화)
            .shadow(
                elevation = elevation,
                shape = CircleShape,
                clip = false,
                ambientColor = primary,
                spotColor = primary,
            )
            .clip(CircleShape)
            // Layer 1: primary 85% alpha 배경
            .background(primary.copy(alpha = 0.85f), CircleShape)
            // Layer 2: 그라데이션 보더 (primary → primaryContainer)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.9f),
                        primaryContainer.copy(alpha = 0.6f),
                    ),
                ),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Layer 3: 중앙 흰색 아이콘
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}
