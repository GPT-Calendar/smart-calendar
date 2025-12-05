package com.example.voicereminder.data.dao

import androidx.room.*
import com.example.voicereminder.data.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for alarm database operations
 */
@Dao
interface AlarmDao {
    
    /**
     * Insert a new alarm into the database
     * @return The ID of the inserted alarm
     */
    @Insert
    suspend fun insert(alarm: AlarmEntity): Long
    
    /**
     * Update an existing alarm
     */
    @Update
    suspend fun update(alarm: AlarmEntity)
    
    /**
     * Delete an alarm from the database
     */
    @Delete
    suspend fun delete(alarm: AlarmEntity)
    
    /**
     * Delete an alarm by ID
     */
    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteById(alarmId: Long)
    
    /**
     * Get a specific alarm by its ID
     */
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?
    
    /**
     * Get all alarms ordered by time
     */
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    suspend fun getAllAlarms(): List<AlarmEntity>
    
    /**
     * Get all alarms as a Flow for reactive updates
     */
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>>
    
    /**
     * Get enabled alarms only
     */
    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY hour ASC, minute ASC")
    suspend fun getEnabledAlarms(): List<AlarmEntity>
    
    /**
     * Get enabled alarms as a Flow
     */
    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledAlarmsFlow(): Flow<List<AlarmEntity>>
    
    /**
     * Get the next alarm to trigger (enabled, with calculated next trigger time)
     */
    @Query("""
        SELECT * FROM alarms 
        WHERE isEnabled = 1 AND nextTrigger IS NOT NULL
        ORDER BY nextTrigger ASC
        LIMIT 1
    """)
    suspend fun getNextAlarm(): AlarmEntity?
    
    /**
     * Get the next alarm as a Flow
     */
    @Query("""
        SELECT * FROM alarms 
        WHERE isEnabled = 1 AND nextTrigger IS NOT NULL
        ORDER BY nextTrigger ASC
        LIMIT 1
    """)
    fun getNextAlarmFlow(): Flow<AlarmEntity?>
    
    /**
     * Toggle alarm enabled state
     */
    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id = :alarmId")
    suspend fun setEnabled(alarmId: Long, enabled: Boolean)
    
    /**
     * Update next trigger time
     */
    @Query("UPDATE alarms SET nextTrigger = :nextTrigger WHERE id = :alarmId")
    suspend fun updateNextTrigger(alarmId: Long, nextTrigger: Long?)
    
    /**
     * Update last triggered time and reset snooze count
     */
    @Query("UPDATE alarms SET lastTriggered = :triggeredAt, snoozeCount = 0 WHERE id = :alarmId")
    suspend fun markAsTriggered(alarmId: Long, triggeredAt: Long)
    
    /**
     * Increment snooze count
     */
    @Query("UPDATE alarms SET snoozeCount = snoozeCount + 1 WHERE id = :alarmId")
    suspend fun incrementSnoozeCount(alarmId: Long)
    
    /**
     * Get alarms that repeat on a specific day
     */
    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND repeatDays LIKE '%' || :day || '%'")
    suspend fun getAlarmsForDay(day: Int): List<AlarmEntity>
    
    /**
     * Get count of enabled alarms
     */
    @Query("SELECT COUNT(*) FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledCount(): Int
    
    /**
     * Get count of all alarms
     */
    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getTotalCount(): Int
    
    /**
     * Disable all alarms (for emergency/testing)
     */
    @Query("UPDATE alarms SET isEnabled = 0")
    suspend fun disableAllAlarms()
}
