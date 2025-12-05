package com.example.voicereminder.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Categories for organizing tasks and reminders
 */
enum class TaskCategory {
    WORK,
    PERSONAL,
    SHOPPING,
    HEALTH,
    FINANCE,
    HOME,
    CUSTOM;
    
    companion object {
        /**
         * Get the icon associated with a category
         */
        fun getIcon(category: TaskCategory): ImageVector {
            return when (category) {
                WORK -> Icons.Default.Work
                PERSONAL -> Icons.Default.Person
                SHOPPING -> Icons.Default.ShoppingCart
                HEALTH -> Icons.Default.Favorite
                FINANCE -> Icons.Default.AttachMoney
                HOME -> Icons.Default.Home
                CUSTOM -> Icons.Default.Label
            }
        }
        
        /**
         * Get the color associated with a category
         */
        fun getColor(category: TaskCategory): Color {
            return when (category) {
                WORK -> Color(0xFF1976D2)      // Blue
                PERSONAL -> Color(0xFF7B1FA2)  // Purple
                SHOPPING -> Color(0xFF388E3C)  // Green
                HEALTH -> Color(0xFFD32F2F)    // Red
                FINANCE -> Color(0xFFF57C00)   // Orange
                HOME -> Color(0xFF5D4037)      // Brown
                CUSTOM -> Color(0xFF607D8B)    // Blue Grey
            }
        }
        
        /**
         * Get the display name for a category
         */
        fun getDisplayName(category: TaskCategory): String {
            return when (category) {
                WORK -> "Work"
                PERSONAL -> "Personal"
                SHOPPING -> "Shopping"
                HEALTH -> "Health"
                FINANCE -> "Finance"
                HOME -> "Home"
                CUSTOM -> "Custom"
            }
        }
        
        /**
         * Parse category from string (case-insensitive)
         */
        fun fromString(value: String): TaskCategory {
            return when (value.uppercase()) {
                "WORK" -> WORK
                "PERSONAL" -> PERSONAL
                "SHOPPING" -> SHOPPING
                "HEALTH" -> HEALTH
                "FINANCE" -> FINANCE
                "HOME" -> HOME
                "CUSTOM" -> CUSTOM
                else -> PERSONAL
            }
        }
        
        /**
         * Get all categories as a list
         */
        fun all(): List<TaskCategory> = values().toList()
    }
}
