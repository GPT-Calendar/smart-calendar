package com.example.voicereminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a reminder in the database
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,
    val scheduledTime: Long?, // Unix timestamp in milliseconds (nullable for location-based reminders)
    val status: ReminderStatus,
    val createdAt: Long, // Unix timestamp in milliseconds
    
    // Fields for location-based reminders
    val reminderType: ReminderType = ReminderType.TIME_BASED,
    val locationData: String? = null, // JSON string containing location info
    val geofenceId: String? = null, // Unique ID for the geofence
    
    // Enhanced fields for priority, category, and recurrence
    val priority: Priority = Priority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val recurrenceRule: String? = null, // JSON string for RecurrenceRule
    val snoozeCount: Int = 0, // Number of times snoozed
    val originalScheduledTime: Long? = null, // Original time before snooze
    val notes: String? = null // Additional notes
)
