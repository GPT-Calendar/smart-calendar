package com.example.voicereminder.domain.models

/**
 * Domain model representing a savings goal
 */
data class SavingsGoal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null,
    val isCompleted: Boolean = false,
    val iconName: String = "savings",
    val color: String = "#4CAF50"
) {
    /**
     * Calculate remaining amount to reach goal
     */
    val remaining: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
    
    /**
     * Calculate progress percentage (0-100)
     */
    val progressPercentage: Float
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toFloat().coerceAtMost(100f) else 0f
    
    /**
     * Check if goal is overdue (past deadline and not completed)
     */
    val isOverdue: Boolean
        get() = deadline != null && !isCompleted && System.currentTimeMillis() > deadline
    
    /**
     * Get days remaining until deadline
     */
    val daysRemaining: Int?
        get() {
            if (deadline == null || isCompleted) return null
            val diff = deadline - System.currentTimeMillis()
            return if (diff > 0) (diff / (24 * 60 * 60 * 1000)).toInt() else 0
        }
    
    /**
     * Get status for display
     */
    val status: GoalStatus
        get() = when {
            isCompleted -> GoalStatus.COMPLETED
            isOverdue -> GoalStatus.OVERDUE
            progressPercentage >= 75f -> GoalStatus.ALMOST_THERE
            progressPercentage >= 50f -> GoalStatus.HALFWAY
            else -> GoalStatus.IN_PROGRESS
        }
}

/**
 * Goal status for UI display
 */
enum class GoalStatus {
    IN_PROGRESS,    // Less than 50%
    HALFWAY,        // 50-75%
    ALMOST_THERE,   // 75-99%
    COMPLETED,      // 100%
    OVERDUE         // Past deadline
}

/**
 * Data class for creating/updating a savings goal
 */
data class SavingsGoalInput(
    val name: String,
    val targetAmount: Double,
    val deadline: Long? = null,
    val iconName: String = "savings",
    val color: String = "#4CAF50"
)
