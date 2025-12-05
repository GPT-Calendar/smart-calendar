package com.example.voicereminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a savings goal.
 * Users can create goals with target amounts and track progress.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                   // Goal name (e.g., "Emergency Fund", "Vacation")
    val targetAmount: Double,           // Target amount in AFN
    val currentAmount: Double = 0.0,    // Current saved amount
    val deadline: Long? = null,         // Optional deadline timestamp
    val isCompleted: Boolean = false,   // Whether goal has been reached
    val iconName: String = "savings",   // Material icon name for display
    val color: String = "#4CAF50",      // Hex color for display
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
