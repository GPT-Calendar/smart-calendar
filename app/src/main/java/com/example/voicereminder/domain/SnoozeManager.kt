package com.example.voicereminder.domain

import android.content.Context
import android.util.Log
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.domain.models.SnoozeOption
import com.example.voicereminder.domain.models.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Manager for handling snooze and postpone operations for reminders and alarms
 */
class SnoozeManager(
    private val database: ReminderDatabase,
    private val context: Context
) {
    companion object {
        private const val TAG = "SnoozeManager"
        
        @Volatile
        private var INSTANCE: SnoozeManager? = null
        
        fun getInstance(context: Context): SnoozeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SnoozeManager(
                    ReminderDatabase.getDatabase(context),
                    context.applicationContext
                ).also { INSTANCE = it }
            }
        }
    }
    
    private val reminderDao = database.reminderDao()
    private val notificationScheduler = NotificationScheduler(context)
    private val enhancedAlarmManager by lazy { EnhancedAlarmManager.getInstance(context) }
    
    /**
     * Get available snooze options
     */
    fun getSnoozeOptions(): List<SnoozeOption> = SnoozeOption.DEFAULT_OPTIONS
    
    /**
     * Snooze a reminder for the specified duration
     * 
     * @param reminderId The ID of the reminder to snooze
     * @param minutes Duration in minutes to snooze
     * @return true if snooze was successful
     */
    suspend fun snoozeReminder(reminderId: Long, minutes: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val reminderEntity = reminderDao.getReminderById(reminderId)
                if (reminderEntity == null) {
                    Log.e(TAG, "Reminder not found: $reminderId")
                    return@withContext false
                }
                
                // Calculate new scheduled time
                val newScheduledTime = LocalDateTime.now().plusMinutes(minutes.toLong())
                val newScheduledTimeMillis = newScheduledTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                // Store original time if this is the first snooze
                val originalTime = reminderEntity.originalScheduledTime ?: reminderEntity.scheduledTime
                
                // Update the reminder
                val updatedEntity = reminderEntity.copy(
                    scheduledTime = newScheduledTimeMillis,
                    originalScheduledTime = originalTime,
                    snoozeCount = reminderEntity.snoozeCount + 1,
                    status = ReminderStatus.PENDING
                )
                reminderDao.update(updatedEntity)
                
                // Reschedule the notification
                val reminder = updatedEntity.toDomain()
                notificationScheduler.scheduleReminder(reminder)
                
                Log.d(TAG, "Reminder $reminderId snoozed for $minutes minutes (snooze count: ${updatedEntity.snoozeCount})")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing reminder", e)
                false
            }
        }
    }
    
    /**
     * Snooze an alarm for the specified duration
     * 
     * @param alarmId The ID of the alarm to snooze
     * @param minutes Duration in minutes to snooze
     * @return true if snooze was successful
     */
    suspend fun snoozeAlarm(alarmId: Long, minutes: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                enhancedAlarmManager.snoozeAlarm(alarmId, minutes)
                Log.d(TAG, "Alarm $alarmId snoozed for $minutes minutes")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing alarm", e)
                false
            }
        }
    }
    
    /**
     * Postpone a reminder to a specific time
     * 
     * @param reminderId The ID of the reminder to postpone
     * @param newTime The new scheduled time
     * @return true if postpone was successful
     */
    suspend fun postponeReminder(reminderId: Long, newTime: LocalDateTime): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val reminderEntity = reminderDao.getReminderById(reminderId)
                if (reminderEntity == null) {
                    Log.e(TAG, "Reminder not found: $reminderId")
                    return@withContext false
                }
                
                // Validate new time is in the future
                if (newTime.isBefore(LocalDateTime.now())) {
                    Log.e(TAG, "Cannot postpone to a past time")
                    return@withContext false
                }
                
                val newScheduledTimeMillis = newTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                // Store original time if not already stored
                val originalTime = reminderEntity.originalScheduledTime ?: reminderEntity.scheduledTime
                
                // Update the reminder
                val updatedEntity = reminderEntity.copy(
                    scheduledTime = newScheduledTimeMillis,
                    originalScheduledTime = originalTime,
                    status = ReminderStatus.PENDING
                )
                reminderDao.update(updatedEntity)
                
                // Reschedule the notification
                val reminder = updatedEntity.toDomain()
                notificationScheduler.scheduleReminder(reminder)
                
                Log.d(TAG, "Reminder $reminderId postponed to $newTime")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error postponing reminder", e)
                false
            }
        }
    }
    
    /**
     * Get the snooze history for a reminder
     * 
     * @param reminderId The ID of the reminder
     * @return Pair of (snoozeCount, originalScheduledTime) or null if not found
     */
    suspend fun getSnoozeInfo(reminderId: Long): Pair<Int, LocalDateTime?>? {
        return withContext(Dispatchers.IO) {
            try {
                val reminderEntity = reminderDao.getReminderById(reminderId)
                if (reminderEntity == null) {
                    return@withContext null
                }
                
                val originalTime = reminderEntity.originalScheduledTime?.let {
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(it),
                        ZoneId.systemDefault()
                    )
                }
                
                return@withContext Pair(reminderEntity.snoozeCount, originalTime)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting snooze info", e)
                null
            }
        }
    }
    
    /**
     * Reset snooze count for a reminder (e.g., when manually rescheduled)
     */
    suspend fun resetSnoozeCount(reminderId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val reminderEntity = reminderDao.getReminderById(reminderId)
                if (reminderEntity != null) {
                    val updatedEntity = reminderEntity.copy(
                        snoozeCount = 0,
                        originalScheduledTime = null
                    )
                    reminderDao.update(updatedEntity)
                    Log.d(TAG, "Snooze count reset for reminder $reminderId")
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting snooze count", e)
                Unit
            }
        }
    }
}
