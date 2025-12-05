package com.example.voicereminder.domain.models

data class AIInsight(
    val message: String,
    val type: InsightType,
    val timestamp: Long
)

enum class InsightType {
    SPENDING_INCREASE,
    SPENDING_DECREASE,
    BUDGET_WARNING,
    SAVING_TIP
}
