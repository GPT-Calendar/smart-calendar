package com.example.voicereminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reminder database operations
 */
@Dao
interface ReminderDao {
    
    /**
     * Insert a new reminder into the database
     * @return The ID of the inserted reminder
     */
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long
    
    /**
     * Get all reminders (regardless of status) ordered by scheduled time
     * @return List of all reminders
     */
    @Query("SELECT * FROM reminders ORDER BY scheduledTime ASC")
    suspend fun getAllReminders(): List<ReminderEntity>

    /**
     * Get all active (pending) reminders ordered by scheduled time
     * @return List of pending reminders
     */
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' ORDER BY scheduledTime ASC")
    suspend fun getActiveReminders(): List<ReminderEntity>
    
    /**
     * Get all active (pending) reminders as a Flow for reactive updates
     * @return Flow of pending reminders that updates automatically when data changes
     */
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' ORDER BY scheduledTime ASC")
    fun getActiveRemindersFlow(): Flow<List<ReminderEntity>>
    
    /**
     * Get all reminders (regardless of status) as a Flow for reactive updates
     * @return Flow of all reminders that updates automatically when data changes
     */
    @Query("SELECT * FROM reminders ORDER BY scheduledTime ASC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>
    
    /**
     * Get a specific reminder by its ID
     * @param id The reminder ID
     * @return The reminder entity or null if not found
     */
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?
    
    /**
     * Update an existing reminder
     */
    @Update
    suspend fun update(reminder: ReminderEntity)
    
    /**
     * Delete a reminder from the database
     */
    @Delete
    suspend fun delete(reminder: ReminderEntity)
    
    /**
     * Get all active location-based reminders
     * @return List of pending location-based reminders
     */
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND reminderType = 'LOCATION_BASED' ORDER BY createdAt ASC")
    suspend fun getActiveLocationReminders(): List<ReminderEntity>
    
    /**
     * Get all active location-based reminders as a Flow for reactive updates
     * @return Flow of pending location-based reminders that updates automatically when data changes
     */
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND reminderType = 'LOCATION_BASED' ORDER BY createdAt ASC")
    fun getActiveLocationRemindersFlow(): Flow<List<ReminderEntity>>
    
    /**
     * Get a reminder by its geofence ID
     * @param geofenceId The geofence ID
     * @return The reminder entity or null if not found
     */
    @Query("SELECT * FROM reminders WHERE geofenceId = :geofenceId")
    suspend fun getReminderByGeofenceId(geofenceId: String): ReminderEntity?
    
    /**
     * Get all active time-based reminders
     * @return List of pending time-based reminders
     */
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND reminderType = 'TIME_BASED' ORDER BY scheduledTime ASC")
    suspend fun getActiveTimeReminders(): List<ReminderEntity>
    
    /**
     * Get all cancelled location-based reminders
     * Used to re-enable reminders when location permissions are re-granted
     * @return List of cancelled location-based reminders
     */
    @Query("SELECT * FROM reminders WHERE status = 'CANCELLED' AND reminderType = 'LOCATION_BASED' ORDER BY createdAt ASC")
    suspend fun getCancelledLocationReminders(): List<ReminderEntity>
    
    /**
     * Get count of pending reminders (for widget badge)
     * @return Count of pending reminders
     */
    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'PENDING'")
    suspend fun getPendingRemindersCount(): Int
    
    /**
     * Get upcoming reminders for widget display
     * @param limit Maximum number of reminders to return
     * @return List of upcoming pending reminders
     */
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND scheduledTime > :now ORDER BY scheduledTime ASC LIMIT :limit")
    suspend fun getUpcomingReminders(limit: Int, now: Long = System.currentTimeMillis()): List<ReminderEntity>
}
