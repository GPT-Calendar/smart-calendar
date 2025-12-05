package com.example.voicereminder.domain.models

import java.text.NumberFormat
import java.util.Locale

/**
 * Extension function to format Double values as currency strings.
 * Formats the number with thousand separators and 2 decimal places.
 * 
 * Example: 12450.0.formatCurrency() returns "12,450.00"
 */
fun Double.formatCurrency(): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    return formatter.format(this)
}
