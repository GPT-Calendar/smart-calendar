package com.example.voicereminder.domain.models

import com.example.voicereminder.data.Priority
import com.example.voicereminder.data.RecurrenceRule
import com.example.voicereminder.data.ReminderEntity
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.data.ReminderType
import com.example.voicereminder.data.TaskCategory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Domain model representing a reminder
 */
data class Reminder(
    val id: Long,
    val message: String,
    val scheduledTime: LocalDateTime?,
    val status: ReminderStatus,
    val createdAt: LocalDateTime,
    val reminderType: ReminderType = ReminderType.TIME_BASED,
    val locationData: String? = null,
    val geofenceId: String? = null,
    // Enhanced fields
    val priority: Priority = Priority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val recurrenceRule: RecurrenceRule? = null,
    val snoozeCount: Int = 0,
    val originalScheduledTime: LocalDateTime? = null,
    val notes: String? = null
) {
    /**
     * Check if this is a recurring reminder
     */
    fun isRecurring(): Boolean = recurrenceRule?.isRecurring() == true
    
    /**
     * Check if this reminder has been snoozed
     */
    fun isSnoozed(): Boolean = snoozeCount > 0
}

/**
 * Extension function to convert ReminderEntity to domain Reminder
 */
fun ReminderEntity.toDomain(): Reminder {
    return Reminder(
        id = id,
        message = message,
        scheduledTime = scheduledTime?.let {
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(it),
                ZoneId.systemDefault()
            )
        },
        status = status,
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        ),
        reminderType = reminderType,
        locationData = locationData,
        geofenceId = geofenceId,
        priority = priority,
        category = category,
        recurrenceRule = RecurrenceRule.fromJson(recurrenceRule),
        snoozeCount = snoozeCount,
        originalScheduledTime = originalScheduledTime?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        },
        notes = notes
    )
}

/**
 * Extension function to convert domain Reminder to ReminderEntity
 */
fun Reminder.toEntity(): ReminderEntity {
    return ReminderEntity(
        id = id,
        message = message,
        scheduledTime = scheduledTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        status = status,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        reminderType = reminderType,
        locationData = locationData,
        geofenceId = geofenceId,
        priority = priority,
        category = category,
        recurrenceRule = recurrenceRule?.toJson(),
        snoozeCount = snoozeCount,
        originalScheduledTime = originalScheduledTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        notes = notes
    )
}
