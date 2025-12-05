package com.example.voicereminder.data

import kotlinx.serialization.Serializable

/**
 * Enum representing the trigger type for location-based reminders
 */
@Serializable
enum class GeofenceTriggerType {
    ENTER,  // Trigger when entering the location
    EXIT,   // Trigger when leaving the location
    BOTH    // Trigger on both enter and exit
}

/**
 * Enum representing the recurrence pattern for location reminders
 */
@Serializable
enum class LocationRecurrence {
    ONCE,           // One-time reminder (default)
    EVERY_TIME,     // Trigger every time user enters/exits
    DAILY,          // Once per day at this location
    WEEKDAYS,       // Only on weekdays
    WEEKENDS        // Only on weekends
}

/**
 * Data class representing time constraints for hybrid reminders
 */
@Serializable
data class TimeConstraint(
    val startHour: Int? = null,      // Start hour (0-23)
    val startMinute: Int? = null,    // Start minute (0-59)
    val endHour: Int? = null,        // End hour (0-23)
    val endMinute: Int? = null,      // End minute (0-59)
    val daysOfWeek: List<Int>? = null // 1=Monday, 7=Sunday
)

/**
 * Data class representing location information for a location-based reminder
 * This will be serialized to JSON for storage in the database
 */
@Serializable
data class LocationData(
    val locationType: LocationType,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Float = 100f, // meters (user-configurable)
    val placeName: String? = null,
    val placeCategory: PlaceCategory? = null,
    
    // Enhanced features
    val triggerType: GeofenceTriggerType = GeofenceTriggerType.ENTER,
    val recurrence: LocationRecurrence = LocationRecurrence.ONCE,
    val timeConstraint: TimeConstraint? = null, // For hybrid time+location reminders
    val lastTriggeredAt: Long? = null, // Timestamp of last trigger (for recurring)
    val triggerCount: Int = 0, // Number of times triggered
    val snoozedUntil: Long? = null, // Snooze timestamp
    val snoozeCount: Int = 0 // Number of times snoozed
)
