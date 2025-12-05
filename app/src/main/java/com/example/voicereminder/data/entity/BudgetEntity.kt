package com.example.voicereminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a budget for a specific category and month.
 * Users can set monthly spending limits per category.
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,           // Category name (e.g., "FOOD", "TRANSPORT")
    val monthlyLimit: Double,       // Budget limit in AFN
    val month: String,              // Month in "yyyy-MM" format (e.g., "2025-11")
    val isActive: Boolean = true,   // Whether this budget is currently active
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
