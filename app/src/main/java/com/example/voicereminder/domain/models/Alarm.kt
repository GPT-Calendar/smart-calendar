package com.example.voicereminder.domain.models

import com.example.voicereminder.data.entity.AlarmEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Domain model representing an alarm
 */
data class Alarm(
    val id: Long = 0,
    val label: String = "Alarm",
    val hour: Int, // 0-23
    val minute: Int, // 0-59
    val isEnabled: Boolean = true,
    val repeatDays: List<Int> = emptyList(), // 1=Monday, 7=Sunday
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeCount: Int = 0,
    val snoozeDurationMinutes: Int = 5,
    val lastTriggered: LocalDateTime? = null,
    val nextTrigger: LocalDateTime? = null,
    val createdAt: LocalDateTime
) {
    /**
     * Get the alarm time as LocalTime
     */
    fun getTime(): LocalTime = LocalTime.of(hour, minute)
    
    /**
     * Get formatted time string (e.g., "7:30 AM")
     */
    fun getFormattedTime(): String {
        val time = getTime()
        return time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    
    /**
     * Get formatted time in 24-hour format
     */
    fun getFormattedTime24(): String {
        return String.format("%02d:%02d", hour, minute)
    }
    
    /**
     * Check if this is a repeating alarm
     */
    fun isRepeating(): Boolean = repeatDays.isNotEmpty()
    
    /**
     * Check if alarm repeats on a specific day
     */
    fun repeatsOn(dayOfWeek: DayOfWeek): Boolean {
        return repeatDays.contains(dayOfWeek.value)
    }
    
    /**
     * Get repeat days description
     */
    fun getRepeatDescription(): String {
        return when {
            repeatDays.isEmpty() -> "One time"
            repeatDays.sorted() == listOf(1, 2, 3, 4, 5) -> "Weekdays"
            repeatDays.sorted() == listOf(6, 7) -> "Weekends"
            repeatDays.sorted() == listOf(1, 2, 3, 4, 5, 6, 7) -> "Every day"
            else -> repeatDays.sorted().map { getDayAbbreviation(it) }.joinToString(", ")
        }
    }
    
    /**
     * Calculate the next trigger time from now
     */
    fun calculateNextTrigger(): LocalDateTime {
        val now = LocalDateTime.now()
        val alarmTime = LocalTime.of(hour, minute)
        var nextDateTime = now.with(alarmTime)
        
        // If the time has passed today, start from tomorrow
        if (nextDateTime.isBefore(now) || nextDateTime.isEqual(now)) {
            nextDateTime = nextDateTime.plusDays(1)
        }
        
        // If repeating, find the next valid day
        if (isRepeating()) {
            while (!repeatDays.contains(nextDateTime.dayOfWeek.value)) {
                nextDateTime = nextDateTime.plusDays(1)
            }
        }
        
        return nextDateTime
    }
    
    /**
     * Get time until next trigger as a human-readable string
     */
    fun getTimeUntilTrigger(): String {
        val next = nextTrigger ?: calculateNextTrigger()
        val now = LocalDateTime.now()
        
        val duration = java.time.Duration.between(now, next)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        
        return when {
            hours == 0L && minutes <= 1 -> "Less than a minute"
            hours == 0L -> "$minutes minutes"
            hours == 1L && minutes == 0L -> "1 hour"
            hours == 1L -> "1 hour $minutes min"
            hours < 24 && minutes == 0L -> "$hours hours"
            hours < 24 -> "$hours hours $minutes min"
            else -> {
                val days = hours / 24
                if (days == 1L) "1 day" else "$days days"
            }
        }
    }
    
    private fun getDayAbbreviation(day: Int): String {
        return when (day) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> ""
        }
    }
    
    companion object {
        val WEEKDAYS = listOf(1, 2, 3, 4, 5)
        val WEEKENDS = listOf(6, 7)
        val EVERY_DAY = listOf(1, 2, 3, 4, 5, 6, 7)
    }
}

/**
 * Extension function to convert AlarmEntity to domain Alarm
 */
fun AlarmEntity.toDomain(): Alarm {
    return Alarm(
        id = id,
        label = label,
        hour = hour,
        minute = minute,
        isEnabled = isEnabled,
        repeatDays = getRepeatDaysList(),
        soundUri = soundUri,
        vibrate = vibrate,
        snoozeCount = snoozeCount,
        snoozeDurationMinutes = snoozeDurationMinutes,
        lastTriggered = lastTriggered?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        },
        nextTrigger = nextTrigger?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        },
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault())
    )
}

/**
 * Extension function to convert domain Alarm to AlarmEntity
 */
fun Alarm.toEntity(): AlarmEntity {
    return AlarmEntity(
        id = id,
        label = label,
        hour = hour,
        minute = minute,
        isEnabled = isEnabled,
        repeatDays = if (repeatDays.isEmpty()) "" else repeatDays.sorted().joinToString(","),
        soundUri = soundUri,
        vibrate = vibrate,
        snoozeCount = snoozeCount,
        snoozeDurationMinutes = snoozeDurationMinutes,
        lastTriggered = lastTriggered?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        nextTrigger = nextTrigger?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}

/**
 * Snooze options for alarms and reminders
 */
data class SnoozeOption(
    val label: String,
    val minutes: Int
) {
    companion object {
        val DEFAULT_OPTIONS = listOf(
            SnoozeOption("5 minutes", 5),
            SnoozeOption("10 minutes", 10),
            SnoozeOption("15 minutes", 15),
            SnoozeOption("30 minutes", 30),
            SnoozeOption("1 hour", 60)
        )
    }
}
