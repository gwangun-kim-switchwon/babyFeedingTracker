package com.baby.feedingtracker.ui.statistics

enum class DailyEntryType { FEEDING, DIAPER, SLEEP }

data class DailyEntry(
    val timestamp: Long,
    val type: DailyEntryType,
    val label: String,      // "분유 120ml", "소변", "낮잠 시작 → 1시간 20분" 등
    val icon: String,       // 이모지: "🍼", "💧", "💤"
)
