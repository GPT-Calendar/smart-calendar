package com.example.voicereminder.domain.models

data class FinanceSummary(
    val totalSpent: Double,
    val totalReceived: Double,
    val trendPercentage: Double,
    val trendDirection: TrendDirection,
    val month: String
)

enum class TrendDirection {
    UP, DOWN, FLAT, NEUTRAL
}
