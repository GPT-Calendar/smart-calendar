package com.example.voicereminder.domain.models

data class FinanceData(
    val summary: FinanceSummary?,
    val quickStats: List<QuickStat>?,
    val transactions: List<Transaction>?,
    val insight: AIInsight?
)
