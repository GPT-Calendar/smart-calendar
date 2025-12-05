package com.example.voicereminder.data

import androidx.compose.ui.graphics.Color

/**
 * Priority levels for reminders and tasks
 */
enum class Priority {
    HIGH,
    MEDIUM,
    LOW;
    
    companion object {
        /**
         * Get the color associated with a priority level
         */
        fun getColor(priority: Priority): Color {
            return when (priority) {
                HIGH -> Color(0xFFD32F2F)    // Red
                MEDIUM -> Color(0xFFFF9800)  // Orange
                LOW -> Color(0xFF4CAF50)     // Green
            }
        }
        
        /**
         * Get the display name for a priority level
         */
        fun getDisplayName(priority: Priority): String {
            return when (priority) {
                HIGH -> "High"
                MEDIUM -> "Medium"
                LOW -> "Low"
            }
        }
        
        /**
         * Parse priority from string (case-insensitive)
         */
        fun fromString(value: String): Priority {
            return when (value.uppercase()) {
                "HIGH" -> HIGH
                "MEDIUM" -> MEDIUM
                "LOW" -> LOW
                else -> MEDIUM
            }
        }
    }
}
