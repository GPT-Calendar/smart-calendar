package com.example.voicereminder.data

import kotlinx.serialization.Serializable

/**
 * Enum representing the type of location for a reminder
 */
@Serializable
enum class LocationType {
    SPECIFIC_PLACE,  // Home, work, custom address
    GENERIC_CATEGORY // Any store, pharmacy, etc.
}
