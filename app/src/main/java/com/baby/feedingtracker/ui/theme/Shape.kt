package com.baby.feedingtracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ──────────────────────────────────────────────
// Mammamia Shape System
// R9 §4 — 세련된 둥근 모서리 스케일 (8 / 12 / 16 / 24 / 32 dp)
// ──────────────────────────────────────────────

val MammamiaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
