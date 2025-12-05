package com.example.voicereminder.domain.models

/**
 * Domain model representing a budget for a category
 */
data class Budget(
    val id: Long = 0,
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val spent: Double = 0.0,
    val month: String,              // "yyyy-MM" format
    val isActive: Boolean = true
) {
    /**
     * Calculate remaining budget
     */
    val remaining: Double
        get() = (monthlyLimit - spent).coerceAtLeast(0.0)
    
    /**
     * Calculate percentage spent (0-100+)
     */
    val percentageSpent: Float
        get() = if (monthlyLimit > 0) ((spent / monthlyLimit) * 100).toFloat() else 0f
    
    /**
     * Check if budget is exceeded
     */
    val isExceeded: Boolean
        get() = spent > monthlyLimit
    
    /**
     * Check if approaching limit (80% or more)
     */
    val isApproachingLimit: Boolean
        get() = percentageSpent >= 80f && !isExceeded
    
    /**
     * Get status for display
     */
    val status: BudgetStatus
        get() = when {
            isExceeded -> BudgetStatus.EXCEEDED
            isApproachingLimit -> BudgetStatus.WARNING
            else -> BudgetStatus.NORMAL
        }
}

/**
 * Budget status for UI display
 */
enum class BudgetStatus {
    NORMAL,     // Under 80% spent
    WARNING,    // 80-100% spent
    EXCEEDED    // Over 100% spent
}

/**
 * Data class for creating/updating a budget
 */
data class BudgetInput(
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val month: String
)
