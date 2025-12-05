package com.example.voicereminder.data

import kotlinx.serialization.Serializable

/**
 * Enum representing predefined place categories for generic location reminders
 */
@Serializable
enum class PlaceCategory {
    STORE,
    GROCERY,
    PHARMACY,
    GAS_STATION,
    RESTAURANT,
    CUSTOM
}
