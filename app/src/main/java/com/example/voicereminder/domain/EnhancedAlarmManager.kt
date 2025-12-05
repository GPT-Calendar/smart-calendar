package com.example.voicereminder.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.entity.AlarmEntity
import com.example.voicereminder.domain.models.Alarm
import com.example.voicereminder.domain.models.toDomain
import com.example.voicereminder.domain.models.toEntity
import com.example.voicereminder.receivers.AlarmTriggerReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Result class for alarm operations
 */
sealed class AlarmResult {
    data class Success(val alarmId: Long) : AlarmResult()
    data class Error(val message: String) : AlarmResult()
}

/**
 * Enhanced alarm manager for creating and managing alarms with repeat functionality
 */
class EnhancedAlarmManager(
    private val database: ReminderDatabase,
    private val context: Context
) {
    companion object {
        private const val TAG = "EnhancedAlarmManager"
        private const val ALARM_REQUEST_CODE_BASE = 10000
        
        @Volatile
        private var INSTANCE: EnhancedAlarmManager? = null
        
        fun getInstance(context: Context): EnhancedAlarmManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnhancedAlarmManager(
                    ReminderDatabase.getDatabase(context),
                    context.applicationContext
                ).also { INSTANCE = it }
            }
        }
    }
    
    private val alarmDao = database.alarmDao()
    private val systemAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Create a new alarm
     */
    suspend fun createAlarm(
        hour: Int,
        minute: Int,
        label: String = "Alarm",
        repeatDays: List<Int> = emptyList(),
        vibrate: Boolean = true,
        snoozeDurationMinutes: Int = 5
    ): AlarmResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (hour !in 0..23 || minute !in 0..59) {
                    return@withContext AlarmResult.Error("Invalid time")
                }
                
                val now = System.currentTimeMillis()
                val alarmEntity = AlarmEntity(
                    label = label.ifBlank { "Alarm" },
                    hour = hour,
                    minute = minute,
                    isEnabled = true,
                    repeatDays = if (repeatDays.isEmpty()) "" else repeatDays.sorted().joinToString(","),
                    vibrate = vibrate,
                    snoozeDurationMinutes = snoozeDurationMinutes,
                    createdAt = now
                )
                
                val alarmId = alarmDao.insert(alarmEntity)
                Log.d(TAG, "Alarm created with ID: $alarmId")
                
                // Calculate and set next trigger time
                val alarm = alarmEntity.copy(id = alarmId).toDomain()
                val nextTrigger = alarm.calculateNextTrigger()
                alarmDao.updateNextTrigger(alarmId, nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                
                // Schedule the system alarm
                scheduleSystemAlarm(alarmId, nextTrigger)
                
                AlarmResult.Success(alarmId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating alarm", e)
                AlarmResult.Error("Failed to create alarm: ${e.message}")
            }
        }
    }
    
    /**
     * Toggle alarm enabled state
     */
    suspend fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                alarmDao.setEnabled(alarmId, enabled)
                
                if (enabled) {
                    // Recalculate and schedule
                    val alarm = alarmDao.getAlarmById(alarmId)?.toDomain()
                    if (alarm != null) {
                        val nextTrigger = alarm.calculateNextTrigger()
                        alarmDao.updateNextTrigger(alarmId, nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        scheduleSystemAlarm(alarmId, nextTrigger)
                    }
                } else {
                    // Cancel the scheduled alarm
                    cancelSystemAlarm(alarmId)
                    alarmDao.updateNextTrigger(alarmId, null)
                }
                
                Log.d(TAG, "Alarm $alarmId ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling alarm", e)
            }
        }
    }
    
    /**
     * Delete an alarm
     */
    suspend fun deleteAlarm(alarmId: Long) {
        withContext(Dispatchers.IO) {
            try {
                cancelSystemAlarm(alarmId)
                alarmDao.deleteById(alarmId)
                Log.d(TAG, "Alarm $alarmId deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting alarm", e)
            }
        }
    }
    
    /**
     * Update an existing alarm
     */
    suspend fun updateAlarm(alarm: Alarm): AlarmResult {
        return withContext(Dispatchers.IO) {
            try {
                val entity = alarm.toEntity()
                alarmDao.update(entity)
                
                if (alarm.isEnabled) {
                    val nextTrigger = alarm.calculateNextTrigger()
                    alarmDao.updateNextTrigger(alarm.id, nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    scheduleSystemAlarm(alarm.id, nextTrigger)
                } else {
                    cancelSystemAlarm(alarm.id)
                }
                
                Log.d(TAG, "Alarm ${alarm.id} updated")
                AlarmResult.Success(alarm.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating alarm", e)
                AlarmResult.Error("Failed to update alarm: ${e.message}")
            }
        }
    }
    
    /**
     * Snooze an alarm
     */
    suspend fun snoozeAlarm(alarmId: Long, minutes: Int = 5) {
        withContext(Dispatchers.IO) {
            try {
                val alarm = alarmDao.getAlarmById(alarmId) ?: return@withContext
                
                // Increment snooze count
                alarmDao.incrementSnoozeCount(alarmId)
                
                // Schedule snooze
                val snoozeTime = LocalDateTime.now().plusMinutes(minutes.toLong())
                scheduleSystemAlarm(alarmId, snoozeTime, isSnooze = true)
                
                Log.d(TAG, "Alarm $alarmId snoozed for $minutes minutes")
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing alarm", e)
            }
        }
    }
    
    /**
     * Dismiss an alarm (mark as triggered and schedule next if repeating)
     */
    suspend fun dismissAlarm(alarmId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val alarm = alarmDao.getAlarmById(alarmId)?.toDomain() ?: return@withContext
                
                // Mark as triggered
                alarmDao.markAsTriggered(alarmId, System.currentTimeMillis())
                
                if (alarm.isRepeating()) {
                    // Schedule next occurrence
                    val nextTrigger = alarm.calculateNextTrigger()
                    alarmDao.updateNextTrigger(alarmId, nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    scheduleSystemAlarm(alarmId, nextTrigger)
                    Log.d(TAG, "Repeating alarm $alarmId scheduled for next occurrence: $nextTrigger")
                } else {
                    // One-time alarm - disable it
                    alarmDao.setEnabled(alarmId, false)
                    alarmDao.updateNextTrigger(alarmId, null)
                    Log.d(TAG, "One-time alarm $alarmId disabled after trigger")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing alarm", e)
            }
        }
    }
    
    /**
     * Get an alarm by ID
     */
    suspend fun getAlarmById(alarmId: Long): Alarm? {
        return withContext(Dispatchers.IO) {
            try {
                alarmDao.getAlarmById(alarmId)?.toDomain()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting alarm", e)
                null
            }
        }
    }
    
    /**
     * Get all alarms as a Flow
     */
    fun getAllAlarmsFlow(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarmsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get enabled alarms as a Flow
     */
    fun getEnabledAlarmsFlow(): Flow<List<Alarm>> {
        return alarmDao.getEnabledAlarmsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get the next alarm to trigger as a Flow
     */
    fun getNextAlarmFlow(): Flow<Alarm?> {
        return alarmDao.getNextAlarmFlow().map { entity ->
            entity?.toDomain()
        }
    }
    
    /**
     * Reschedule all enabled alarms (called on boot)
     */
    suspend fun rescheduleAllAlarms() {
        withContext(Dispatchers.IO) {
            try {
                val enabledAlarms = alarmDao.getEnabledAlarms()
                Log.d(TAG, "Rescheduling ${enabledAlarms.size} alarms")
                
                for (entity in enabledAlarms) {
                    val alarm = entity.toDomain()
                    val nextTrigger = alarm.calculateNextTrigger()
                    alarmDao.updateNextTrigger(alarm.id, nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    scheduleSystemAlarm(alarm.id, nextTrigger)
                }
                
                Log.d(TAG, "All alarms rescheduled")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms", e)
            }
        }
    }
    
    /**
     * Check if exact alarms can be scheduled
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            systemAlarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * Schedule a system alarm
     */
    private fun scheduleSystemAlarm(alarmId: Long, triggerTime: LocalDateTime, isSnooze: Boolean = false) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !systemAlarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                return
            }
            
            val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("is_snooze", isSnooze)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (ALARM_REQUEST_CODE_BASE + alarmId).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Use AlarmClockInfo for alarm-style notification
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, pendingIntent)
            systemAlarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            
            Log.d(TAG, "System alarm scheduled for $triggerTime (ID: $alarmId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling system alarm", e)
        }
    }
    
    /**
     * Cancel a system alarm
     */
    private fun cancelSystemAlarm(alarmId: Long) {
        try {
            val intent = Intent(context, AlarmTriggerReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (ALARM_REQUEST_CODE_BASE + alarmId).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            systemAlarmManager.cancel(pendingIntent)
            Log.d(TAG, "System alarm cancelled (ID: $alarmId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling system alarm", e)
        }
    }
}
