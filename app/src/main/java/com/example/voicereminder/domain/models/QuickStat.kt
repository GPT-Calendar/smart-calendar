package com.example.voicereminder.domain.models

import androidx.compose.ui.graphics.vector.ImageVector

data class QuickStat(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val type: StatType
)

enum class StatType {
    WEEKLY_SPEND,
    BIGGEST_EXPENSE,
    TOP_CATEGORY
}
