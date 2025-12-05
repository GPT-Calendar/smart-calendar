package com.example.voicereminder.domain

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderEntity
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.domain.models.toDomain
import com.example.voicereminder.domain.models.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Result class for reminder creation operations
 */
sealed class ReminderResult {
    data class Success(val reminderId: Long) : ReminderResult()
    data class Error(val errorType: ReminderErrorType, val message: String) : ReminderResult()
}

/**
 * Enum representing different types of reminder operation errors
 */
enum class ReminderErrorType {
    DATABASE_ERROR,
    SCHEDULING_ERROR,
    INVALID_INPUT,
    UNKNOWN_ERROR
}

/**
 * Core business logic for managing reminders
 * Coordinates between database operations and notification scheduling
 */
class ReminderManager(
    private val database: ReminderDatabase,
    private val context: Context,
    private val locationReminderManager: LocationReminderManager? = null
) {
    
    private val notificationScheduler = NotificationScheduler(context)
    private val alarmScheduler = AlarmScheduler(context)
    
    companion object {
        private const val TAG = "ReminderManager"
        
        @Volatile
        private var INSTANCE: ReminderManager? = null
        
        @Volatile
        private var TEST_INSTANCE: ReminderManager? = null
        
        /**
         * Get singleton instance of ReminderManager
         */
        fun getInstance(context: Context): ReminderManager {
            // Return test instance if set (for testing)
            TEST_INSTANCE?.let { return it }
            
            // Return or create singleton instance
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createInstance(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * Create a new ReminderManager instance with all dependencies
         */
        private fun createInstance(context: Context): ReminderManager {
            val database = ReminderDatabase.getDatabase(context)
            
            // Initialize LocationReminderManager dependencies
            val locationServiceManager = LocationServiceManager(context)
            val geofenceManager = GeofenceManager(context)
            val placeResolver = PlaceResolver(context, database)
            
            // Create LocationReminderManager
            val locationReminderManager = LocationReminderManager(
                database = database,
                geofenceManager = geofenceManager,
                placeResolver = placeResolver,
                locationServiceManager = locationServiceManager,
                context = context
            )
            
            return ReminderManager(
                database = database,
                context = context,
                locationReminderManager = locationReminderManager
            )
        }
        
        /**
         * Set test instance for testing purposes
         * Should only be called from test code
         */
        fun setTestInstance(instance: ReminderManager) {
            TEST_INSTANCE = instance
        }
        
        /**
         * Clear test instance after testing
         * Should only be called from test code
         */
        fun clearTestInstance() {
            TEST_INSTANCE = null
        }
    }
    
    /**
     * Create a new reminder and schedule its notification
     * 
     * @param time The scheduled time for the reminder
     * @param message The reminder message
     * @return The ID of the created reminder, or -1 if creation failed
     */
    suspend fun createReminder(time: LocalDateTime, message: String): Long {
        val result = createReminderWithError(time, message)
        return if (result is ReminderResult.Success) result.reminderId else -1L
    }

    /**
     * Create a new reminder with detailed error information
     * 
     * @param time The scheduled time for the reminder
     * @param message The reminder message
     * @return ReminderResult with either success or specific error type
     */
    suspend fun createReminderWithError(time: LocalDateTime, message: String): ReminderResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (message.isBlank()) {
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.INVALID_INPUT,
                        "Reminder message cannot be empty"
                    )
                }
                
                if (time.isBefore(LocalDateTime.now())) {
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.INVALID_INPUT,
                        "Scheduled time must be in the future"
                    )
                }
                
                // Create reminder entity
                val reminderEntity = ReminderEntity(
                    message = message,
                    scheduledTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    status = ReminderStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )
                
                // Save to database
                val reminderId = try {
                    database.reminderDao().insert(reminderEntity)
                } catch (e: Exception) {
                    Log.e(TAG, "Database error while inserting reminder", e)
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.DATABASE_ERROR,
                        "Failed to save reminder to database: ${e.message}"
                    )
                }
                
                if (reminderId <= 0) {
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.DATABASE_ERROR,
                        "Failed to save reminder to database"
                    )
                }
                
                // Create domain model with the generated ID
                val reminder = reminderEntity.copy(id = reminderId).toDomain()
                
                // Schedule notification
                val scheduled = try {
                    notificationScheduler.scheduleReminder(reminder)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling notification", e)
                    // Delete the reminder since we couldn't schedule it
                    try {
                        database.reminderDao().delete(reminderEntity.copy(id = reminderId))
                    } catch (deleteError: Exception) {
                        Log.e(TAG, "Error deleting reminder after scheduling failure", deleteError)
                    }
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.SCHEDULING_ERROR,
                        "Failed to schedule notification: ${e.message}"
                    )
                }
                
                if (!scheduled) {
                    // Delete the reminder since we couldn't schedule it
                    try {
                        database.reminderDao().delete(reminderEntity.copy(id = reminderId))
                    } catch (deleteError: Exception) {
                        Log.e(TAG, "Error deleting reminder after scheduling failure", deleteError)
                    }
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.SCHEDULING_ERROR,
                        "Failed to schedule notification. Please check alarm permissions."
                    )
                }
                
                Log.d(TAG, "Reminder created successfully with ID: $reminderId")
                ReminderResult.Success(reminderId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error creating reminder", e)
                ReminderResult.Error(
                    ReminderErrorType.UNKNOWN_ERROR,
                    "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get all reminders (active, completed, cancelled)
     * Includes both time-based and location-based reminders
     *
     * @return List of all reminders ordered by scheduled time
     */
    suspend fun getAllReminders(): List<Reminder> {
        return withContext(Dispatchers.IO) {
            try {
                // Get all reminders from database (includes both time-based and location-based)
                database.reminderDao().getAllReminders().map { it.toDomain() }
            } catch (e: Exception) {
                Log.e(TAG, "Database error while fetching all reminders", e)
                emptyList()
            }
        }
    }

    /**
     * Get all active (pending) reminders
     * Includes both time-based and location-based reminders
     *
     * @return List of active reminders ordered by scheduled time
     */
    suspend fun getActiveReminders(): List<Reminder> {
        return withContext(Dispatchers.IO) {
            try {
                // Get all active reminders from database (includes both time-based and location-based)
                database.reminderDao().getActiveReminders().map { it.toDomain() }
            } catch (e: Exception) {
                Log.e(TAG, "Database error while fetching active reminders", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get all active (pending) reminders as a Flow for reactive updates
     * 
     * @return Flow of active reminders that updates automatically when data changes
     */
    fun getActiveRemindersFlow(): Flow<List<Reminder>> {
        return database.reminderDao().getActiveRemindersFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get all reminders (regardless of status) as a Flow for reactive updates
     * 
     * @return Flow of all reminders that updates automatically when data changes
     */
    fun getAllRemindersFlow(): Flow<List<Reminder>> {
        return database.reminderDao().getAllRemindersFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Mark a reminder as completed
     * 
     * @param reminderId The ID of the reminder to mark as completed
     */
    suspend fun markAsCompleted(reminderId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val reminderEntity = database.reminderDao().getReminderById(reminderId)
                if (reminderEntity != null) {
                    val updatedEntity = reminderEntity.copy(status = ReminderStatus.COMPLETED)
                    database.reminderDao().update(updatedEntity)
                    Log.d(TAG, "Reminder $reminderId marked as completed")
                } else {
                    Log.w(TAG, "Reminder $reminderId not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Database error while marking reminder as completed", e)
            }
        }
    }
    
    /**
     * Delete a reminder and cancel its scheduled notification
     * Handles both time-based and location-based reminders
     * 
     * @param reminderId The ID of the reminder to delete
     */
    suspend fun deleteReminder(reminderId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val reminderEntity = database.reminderDao().getReminderById(reminderId)
                if (reminderEntity != null) {
                    // Check reminder type and handle accordingly
                    when (reminderEntity.reminderType) {
                        com.example.voicereminder.data.ReminderType.TIME_BASED -> {
                            // Cancel the scheduled notification for time-based reminders
                            try {
                                notificationScheduler.cancelReminder(reminderId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error canceling notification for reminder $reminderId", e)
                            }
                        }
                        com.example.voicereminder.data.ReminderType.LOCATION_BASED -> {
                            // Use LocationReminderManager to handle location reminder cleanup
                            if (locationReminderManager != null) {
                                try {
                                    locationReminderManager.deleteLocationReminder(reminderId)
                                    // LocationReminderManager handles both geofence removal and database deletion
                                    // So we return early to avoid duplicate deletion
                                    Log.d(TAG, "Location reminder $reminderId deleted via LocationReminderManager")
                                    return@withContext
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error deleting location reminder via LocationReminderManager", e)
                                    // Fall through to delete from database as fallback
                                }
                            } else {
                                Log.w(TAG, "LocationReminderManager not available, deleting location reminder from database only")
                            }
                        }
                    }
                    
                    // Delete from database
                    database.reminderDao().delete(reminderEntity)
                    Log.d(TAG, "Reminder $reminderId deleted successfully")
                } else {
                    Log.w(TAG, "Reminder $reminderId not found for deletion")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Database error while deleting reminder", e)
            }
        }
    }
    
    /**
     * Create a location-based reminder
     * Routes to LocationReminderManager for handling
     * 
     * @param message The reminder message
     * @param locationData Location information for the reminder
     * @return ReminderResult with success or error information
     */
    suspend fun createLocationReminder(
        message: String,
        locationData: com.example.voicereminder.data.LocationData
    ): ReminderResult {
        return if (locationReminderManager != null) {
            try {
                locationReminderManager.createLocationReminder(message, locationData)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating location reminder", e)
                ReminderResult.Error(
                    ReminderErrorType.UNKNOWN_ERROR,
                    "Failed to create location reminder: ${e.message}"
                )
            }
        } else {
            Log.e(TAG, "LocationReminderManager not available")
            ReminderResult.Error(
                ReminderErrorType.UNKNOWN_ERROR,
                "Location reminders are not available"
            )
        }
    }
    
    /**
     * Schedule a notification for a reminder
     * This is a public method that can be used to reschedule reminders
     * 
     * @param reminder The reminder to schedule
     * @return true if scheduling was successful, false otherwise
     */
    fun scheduleNotification(reminder: Reminder): Boolean {
        return notificationScheduler.scheduleReminder(reminder)
    }
    
    /**
     * Create a system alarm (appears in Clock app and calendar)
     * 
     * @param time The scheduled time for the alarm
     * @param label Optional label for the alarm
     * @return ReminderResult with success or error information
     */
    suspend fun createAlarm(time: LocalDateTime, label: String = "Alarm"): ReminderResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (time.isBefore(LocalDateTime.now())) {
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.INVALID_INPUT,
                        "Alarm time must be in the future"
                    )
                }
                
                // Create reminder entity to store in database (so it appears in calendar)
                val reminderEntity = ReminderEntity(
                    message = label.ifBlank { "Alarm" },
                    scheduledTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    status = ReminderStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )
                
                // Save to database
                val reminderId = try {
                    database.reminderDao().insert(reminderEntity)
                } catch (e: Exception) {
                    Log.e(TAG, "Database error while inserting alarm", e)
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.DATABASE_ERROR,
                        "Failed to save alarm to database: ${e.message}"
                    )
                }
                
                // Schedule the alarm
                val scheduled = try {
                    alarmScheduler.scheduleAlarm(time, label)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling alarm", e)
                    // Delete the reminder since we couldn't schedule it
                    try {
                        database.reminderDao().delete(reminderEntity.copy(id = reminderId))
                    } catch (deleteError: Exception) {
                        Log.e(TAG, "Error deleting alarm after scheduling failure", deleteError)
                    }
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.SCHEDULING_ERROR,
                        "Failed to schedule alarm: ${e.message}"
                    )
                }
                
                if (!scheduled) {
                    // Delete the reminder since we couldn't schedule it
                    try {
                        database.reminderDao().delete(reminderEntity.copy(id = reminderId))
                    } catch (deleteError: Exception) {
                        Log.e(TAG, "Error deleting alarm after scheduling failure", deleteError)
                    }
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.SCHEDULING_ERROR,
                        "Failed to schedule alarm. Please check alarm permissions."
                    )
                }
                
                Log.d(TAG, "Alarm scheduled successfully for $time with ID: $reminderId")
                ReminderResult.Success(reminderId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error creating alarm", e)
                ReminderResult.Error(
                    ReminderErrorType.UNKNOWN_ERROR,
                    "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Save a manual finance transaction
     * 
     * @param amount The transaction amount
     * @param currency The currency code (e.g., "ETB", "USD")
     * @param description Description of the transaction
     * @param isIncome True for income (credit), false for expense (debit)
     */
    suspend fun saveFinanceTransaction(
        amount: Double,
        currency: String,
        description: String,
        isIncome: Boolean
    ) {
        withContext(Dispatchers.IO) {
            try {
                val financeDatabase = com.example.voicereminder.data.FinanceDatabase.getDatabase(context)
                val repository = com.example.voicereminder.data.repository.FinanceRepositoryImpl(financeDatabase)
                
                val transaction = com.example.voicereminder.data.entity.FinanceTransaction(
                    amount = amount,
                    currency = currency,
                    description = description,
                    transactionType = if (isIncome) 
                        com.example.voicereminder.data.entity.TransactionType.CREDIT 
                    else 
                        com.example.voicereminder.data.entity.TransactionType.DEBIT,
                    timestamp = java.util.Date(),
                    bankName = "Manual Entry",
                    phoneNumber = "",
                    smsContent = "",
                    fromAccount = null
                )
                
                repository.insertTransaction(transaction)
                Log.d(TAG, "Finance transaction saved: $description - $amount $currency")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving finance transaction", e)
                throw e
            }
        }
    }
}
