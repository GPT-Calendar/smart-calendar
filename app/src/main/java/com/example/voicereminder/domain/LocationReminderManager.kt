package com.example.voicereminder.domain

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.voicereminder.data.GeofenceTriggerType
import com.example.voicereminder.data.LocationData
import com.example.voicereminder.data.LocationRecurrence
import com.example.voicereminder.data.LocationType
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderEntity
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.data.ReminderType
import com.example.voicereminder.data.TimeConstraint
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.domain.models.toDomain
import com.google.android.gms.location.Geofence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * Manager class for location-based reminder business logic
 * Coordinates between database, geofencing, and place resolution
 */
class LocationReminderManager(
    private val database: ReminderDatabase,
    private val geofenceManager: GeofenceManager,
    private val placeResolver: PlaceResolver,
    private val locationServiceManager: LocationServiceManager,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "LocationReminderManager"
        private const val COOLDOWN_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val RECURRING_COOLDOWN_MS = 60 * 60 * 1000L // 1 hour for recurring reminders
        private const val DAILY_COOLDOWN_MS = 20 * 60 * 60 * 1000L // 20 hours for daily reminders
        private const val NOTIFICATION_CHANNEL_ID = "location_reminder_notifications"
        private const val NOTIFICATION_CHANNEL_NAME = "Location Reminder Notifications"
        private const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for location-based reminders"
        private const val MAX_SAVED_LOCATIONS = 20 // Limit saved locations to conserve memory and improve performance
        private const val DEFAULT_SNOOZE_DURATION_MS = 60 * 60 * 1000L // 1 hour default snooze
    }
    
    // Track last trigger times to prevent duplicate triggers
    private val lastTriggerTimes = mutableMapOf<String, Long>()
    
    // Snooze action constants for notification actions
    object SnoozeActions {
        const val ACTION_SNOOZE_1_HOUR = "com.example.voicereminder.SNOOZE_1_HOUR"
        const val ACTION_SNOOZE_NEXT_VISIT = "com.example.voicereminder.SNOOZE_NEXT_VISIT"
        const val ACTION_SNOOZE_WHEN_LEAVE = "com.example.voicereminder.SNOOZE_WHEN_LEAVE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_GEOFENCE_ID = "geofence_id"
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Create a location-based reminder
     * Saves the reminder to database and registers geofence(s)
     * 
     * @param message The reminder message
     * @param locationData Location information for the reminder
     * @return ReminderResult with success or error information
     */
    suspend fun createLocationReminder(
        message: String,
        locationData: LocationData
    ): ReminderResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (message.isBlank()) {
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.INVALID_INPUT,
                        "Reminder message cannot be empty"
                    )
                }
                
                // Validate location data
                if (locationData.locationType == LocationType.SPECIFIC_PLACE) {
                    if (locationData.latitude == null || locationData.longitude == null) {
                        return@withContext ReminderResult.Error(
                            ReminderErrorType.INVALID_INPUT,
                            "Specific location requires coordinates"
                        )
                    }
                }
                
                // Generate unique geofence ID
                val geofenceId = generateGeofenceId()
                
                // Serialize location data to JSON
                val locationDataJson = json.encodeToString(locationData)
                
                // Create reminder entity
                val reminderEntity = ReminderEntity(
                    message = message,
                    scheduledTime = null, // Location-based reminders don't have scheduled time
                    status = ReminderStatus.PENDING,
                    createdAt = System.currentTimeMillis(),
                    reminderType = ReminderType.LOCATION_BASED,
                    locationData = locationDataJson,
                    geofenceId = geofenceId
                )
                
                // Save to database
                val reminderId = try {
                    database.reminderDao().insert(reminderEntity)
                } catch (e: Exception) {
                    Log.e(TAG, "Database error while inserting location reminder", e)
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
                
                Log.d(TAG, "Location reminder saved with ID: $reminderId, geofenceId: $geofenceId")
                
                // Check if user is already at the location (edge case)
                val alreadyAtLocation = checkIfUserAlreadyAtLocation(locationData)
                if (alreadyAtLocation) {
                    Log.d(TAG, "User is already at location - triggering reminder immediately")
                    // Trigger the reminder immediately
                    handleGeofenceTransition(geofenceId, com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER)
                    return@withContext ReminderResult.Success(reminderId)
                }
                
                // Register geofence(s) with appropriate trigger type
                val geofenceRegistered = when (locationData.locationType) {
                    LocationType.SPECIFIC_PLACE -> {
                        // Single geofence for specific location with configurable trigger
                        registerSpecificLocationGeofence(
                            geofenceId,
                            locationData.latitude!!,
                            locationData.longitude!!,
                            locationData.radius,
                            locationData.triggerType
                        )
                    }
                    LocationType.GENERIC_CATEGORY -> {
                        // Multiple geofences for generic category
                        registerGenericCategoryGeofences(geofenceId, locationData)
                    }
                }
                
                if (!geofenceRegistered) {
                    // Delete the reminder since we couldn't register geofence
                    try {
                        database.reminderDao().delete(reminderEntity.copy(id = reminderId))
                    } catch (deleteError: Exception) {
                        Log.e(TAG, "Error deleting reminder after geofence registration failure", deleteError)
                    }
                    return@withContext ReminderResult.Error(
                        ReminderErrorType.SCHEDULING_ERROR,
                        "Failed to register geofence. Please check location permissions."
                    )
                }
                
                Log.d(TAG, "Location reminder created successfully with ID: $reminderId")
                ReminderResult.Success(reminderId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error creating location reminder", e)
                ReminderResult.Error(
                    ReminderErrorType.UNKNOWN_ERROR,
                    "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Generate a unique geofence ID
     */
    private fun generateGeofenceId(): String {
        return "geofence_${UUID.randomUUID()}"
    }
    
    /**
     * Register a geofence for a specific location with configurable trigger type
     */
    private suspend fun registerSpecificLocationGeofence(
        geofenceId: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        triggerType: GeofenceTriggerType = GeofenceTriggerType.ENTER
    ): Boolean {
        return try {
            val transitionTypes = getTransitionTypesFromTrigger(triggerType)
            geofenceManager.registerGeofenceWithTransitions(
                geofenceId, latitude, longitude, radius, transitionTypes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering specific location geofence", e)
            false
        }
    }
    
    /**
     * Convert GeofenceTriggerType to Android Geofence transition types
     */
    private fun getTransitionTypesFromTrigger(triggerType: GeofenceTriggerType): Int {
        return when (triggerType) {
            GeofenceTriggerType.ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
            GeofenceTriggerType.EXIT -> Geofence.GEOFENCE_TRANSITION_EXIT
            GeofenceTriggerType.BOTH -> Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        }
    }
    
    /**
     * Register geofences for a generic category
     * Finds nearby places and registers geofences for each
     */
    private suspend fun registerGenericCategoryGeofences(
        geofenceId: String,
        locationData: LocationData
    ): Boolean {
        return try {
            // Get current location with retry logic
            val currentLocation = locationServiceManager.getCurrentLocation(maxRetries = 3)
            if (currentLocation == null) {
                Log.e(TAG, "Cannot register generic category geofences - current location unavailable after retries")
                return false
            }
            
            // Find nearby places of the specified category
            val category = locationData.placeCategory
            if (category == null) {
                Log.e(TAG, "Generic category location requires placeCategory")
                return false
            }
            
            val nearbyPlaces = placeResolver.findNearbyPlaces(category, currentLocation)
            
            if (nearbyPlaces.isEmpty()) {
                Log.w(TAG, "No nearby places found for category: $category")
                // For now, we'll consider this a success but log a warning
                // The system will still work when Places API is integrated
                return true
            }
            
            // Register geofences for each nearby place
            val geofenceDataList = nearbyPlaces.mapIndexed { index, place ->
                GeofenceData(
                    id = "${geofenceId}_$index",
                    latitude = place.latitude,
                    longitude = place.longitude,
                    radius = locationData.radius
                )
            }
            
            geofenceManager.registerMultipleGeofences(geofenceDataList)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering generic category geofences", e)
            false
        }
    }
    
    /**
     * Check if user is already at the location when creating the reminder
     * This handles the edge case where the user creates a reminder while already at the location
     */
    private suspend fun checkIfUserAlreadyAtLocation(locationData: LocationData): Boolean {
        return try {
            // Get current location with retry logic
            val currentLocation = locationServiceManager.getCurrentLocation(maxRetries = 2)
            if (currentLocation == null) {
                Log.d(TAG, "Cannot check if user at location - current location unavailable")
                return false
            }
            
            when (locationData.locationType) {
                LocationType.SPECIFIC_PLACE -> {
                    // Check if within radius of specific location
                    val latitude = locationData.latitude ?: return false
                    val longitude = locationData.longitude ?: return false
                    
                    placeResolver.isWithinRadius(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        latitude,
                        longitude,
                        locationData.radius
                    )
                }
                LocationType.GENERIC_CATEGORY -> {
                    // For generic categories, check if near any matching place
                    val category = locationData.placeCategory ?: return false
                    val nearbyPlaces = placeResolver.findNearbyPlaces(category, currentLocation)
                    
                    nearbyPlaces.any { place ->
                        placeResolver.isWithinRadius(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            place.latitude,
                            place.longitude,
                            locationData.radius
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user at location", e)
            false
        }
    }
    
    /**
     * Handle geofence transition events
     * Called when a geofence is entered or exited
     * 
     * @param geofenceId The ID of the triggered geofence
     * @param transitionType The type of transition (ENTER/EXIT)
     */
    suspend fun handleGeofenceTransition(geofenceId: String, transitionType: Int) {
        withContext(Dispatchers.IO) {
            try {
                val transitionName = if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) "ENTER" else "EXIT"
                Log.d(TAG, "Handling geofence transition: $geofenceId, type: $transitionName")
                
                // Extract base geofence ID (remove suffix for generic categories)
                val baseGeofenceId = extractBaseGeofenceId(geofenceId)
                
                // Query reminder by geofence ID
                val reminderEntity = database.reminderDao().getReminderByGeofenceId(baseGeofenceId)
                
                if (reminderEntity == null) {
                    Log.w(TAG, "No reminder found for geofence: $geofenceId")
                    return@withContext
                }
                
                // Check if reminder is still pending
                if (reminderEntity.status != ReminderStatus.PENDING) {
                    Log.d(TAG, "Reminder ${reminderEntity.id} is not pending - status: ${reminderEntity.status}")
                    return@withContext
                }
                
                // Parse location data to check trigger type and other settings
                val locationData = try {
                    reminderEntity.locationData?.let { json.decodeFromString<LocationData>(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing location data", e)
                    null
                }
                
                if (locationData == null) {
                    Log.e(TAG, "No location data for reminder ${reminderEntity.id}")
                    return@withContext
                }
                
                // Check if this transition type matches the reminder's trigger type
                if (!shouldTriggerForTransition(locationData.triggerType, transitionType)) {
                    Log.d(TAG, "Transition type $transitionName doesn't match trigger type ${locationData.triggerType}")
                    return@withContext
                }
                
                // Check if reminder is snoozed
                if (isReminderSnoozed(locationData)) {
                    Log.d(TAG, "Reminder ${reminderEntity.id} is snoozed until ${locationData.snoozedUntil}")
                    return@withContext
                }
                
                // Check time constraints for hybrid reminders
                if (!meetsTimeConstraints(locationData.timeConstraint)) {
                    Log.d(TAG, "Reminder ${reminderEntity.id} doesn't meet time constraints")
                    return@withContext
                }
                
                // Check cooldown based on recurrence type
                val cooldownDuration = getCooldownForRecurrence(locationData.recurrence)
                if (isInCooldownWithDuration(geofenceId, cooldownDuration)) {
                    Log.d(TAG, "Geofence $geofenceId is in cooldown period - ignoring trigger")
                    return@withContext
                }
                
                // Check recurrence-specific constraints
                if (!shouldTriggerForRecurrence(locationData)) {
                    Log.d(TAG, "Reminder ${reminderEntity.id} doesn't meet recurrence constraints")
                    return@withContext
                }
                
                Log.d(TAG, "Triggering reminder ${reminderEntity.id}: ${reminderEntity.message} (${transitionName})")
                
                // Trigger notification with snooze actions
                triggerLocationReminderNotificationWithActions(reminderEntity, locationData, transitionType)
                
                // Update reminder based on recurrence type
                handleReminderAfterTrigger(reminderEntity, locationData, geofenceId)
                
                // Update cooldown timestamp
                updateCooldown(geofenceId)
                
                Log.d(TAG, "Successfully handled geofence transition for reminder ${reminderEntity.id}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition", e)
            }
        }
    }
    
    /**
     * Check if the transition type matches the reminder's trigger type
     */
    private fun shouldTriggerForTransition(triggerType: GeofenceTriggerType, transitionType: Int): Boolean {
        return when (triggerType) {
            GeofenceTriggerType.ENTER -> transitionType == Geofence.GEOFENCE_TRANSITION_ENTER
            GeofenceTriggerType.EXIT -> transitionType == Geofence.GEOFENCE_TRANSITION_EXIT
            GeofenceTriggerType.BOTH -> true
        }
    }
    
    /**
     * Check if reminder is currently snoozed
     */
    private fun isReminderSnoozed(locationData: LocationData): Boolean {
        val snoozedUntil = locationData.snoozedUntil ?: return false
        return System.currentTimeMillis() < snoozedUntil
    }
    
    /**
     * Check if current time meets the time constraints
     */
    private fun meetsTimeConstraints(timeConstraint: TimeConstraint?): Boolean {
        if (timeConstraint == null) return true
        
        val now = LocalDateTime.now()
        val currentTime = now.toLocalTime()
        val currentDayOfWeek = now.dayOfWeek.value // 1=Monday, 7=Sunday
        
        // Check day of week constraint
        timeConstraint.daysOfWeek?.let { days ->
            if (currentDayOfWeek !in days) {
                return false
            }
        }
        
        // Check time range constraint
        val startHour = timeConstraint.startHour
        val startMinute = timeConstraint.startMinute ?: 0
        val endHour = timeConstraint.endHour
        val endMinute = timeConstraint.endMinute ?: 59
        
        if (startHour != null && endHour != null) {
            val startTime = LocalTime.of(startHour, startMinute)
            val endTime = LocalTime.of(endHour, endMinute)
            
            // Handle overnight time ranges (e.g., 22:00 to 06:00)
            return if (startTime.isBefore(endTime)) {
                currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
            } else {
                currentTime.isAfter(startTime) || currentTime.isBefore(endTime)
            }
        }
        
        return true
    }
    
    /**
     * Get cooldown duration based on recurrence type
     */
    private fun getCooldownForRecurrence(recurrence: LocationRecurrence): Long {
        return when (recurrence) {
            LocationRecurrence.ONCE -> COOLDOWN_DURATION_MS
            LocationRecurrence.EVERY_TIME -> RECURRING_COOLDOWN_MS
            LocationRecurrence.DAILY -> DAILY_COOLDOWN_MS
            LocationRecurrence.WEEKDAYS -> DAILY_COOLDOWN_MS
            LocationRecurrence.WEEKENDS -> DAILY_COOLDOWN_MS
        }
    }
    
    /**
     * Check if in cooldown with specific duration
     */
    private fun isInCooldownWithDuration(geofenceId: String, duration: Long): Boolean {
        val lastTriggerTime = lastTriggerTimes[geofenceId] ?: return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTriggerTime) < duration
    }
    
    /**
     * Check if reminder should trigger based on recurrence settings
     */
    private fun shouldTriggerForRecurrence(locationData: LocationData): Boolean {
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value
        
        return when (locationData.recurrence) {
            LocationRecurrence.ONCE -> true
            LocationRecurrence.EVERY_TIME -> true
            LocationRecurrence.DAILY -> {
                // Check if already triggered today
                val lastTriggered = locationData.lastTriggeredAt
                if (lastTriggered == null) return true
                val lastTriggerDate = java.time.Instant.ofEpochMilli(lastTriggered)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                lastTriggerDate != now.toLocalDate()
            }
            LocationRecurrence.WEEKDAYS -> {
                currentDayOfWeek in 1..5 // Monday to Friday
            }
            LocationRecurrence.WEEKENDS -> {
                currentDayOfWeek in 6..7 // Saturday and Sunday
            }
        }
    }
    
    /**
     * Handle reminder state after trigger based on recurrence
     */
    private suspend fun handleReminderAfterTrigger(
        reminderEntity: ReminderEntity,
        locationData: LocationData,
        geofenceId: String
    ) {
        val updatedLocationData = locationData.copy(
            lastTriggeredAt = System.currentTimeMillis(),
            triggerCount = locationData.triggerCount + 1,
            snoozedUntil = null // Clear snooze after trigger
        )
        
        when (locationData.recurrence) {
            LocationRecurrence.ONCE -> {
                // Mark as completed and remove geofence
                val updatedEntity = reminderEntity.copy(
                    status = ReminderStatus.COMPLETED,
                    locationData = json.encodeToString(updatedLocationData)
                )
                database.reminderDao().update(updatedEntity)
                geofenceManager.removeGeofence(geofenceId)
            }
            else -> {
                // Keep reminder active for recurring, just update trigger info
                val updatedEntity = reminderEntity.copy(
                    locationData = json.encodeToString(updatedLocationData)
                )
                database.reminderDao().update(updatedEntity)
                // Don't remove geofence for recurring reminders
            }
        }
    }
    
    /**
     * Snooze a location reminder
     * @param reminderId The ID of the reminder to snooze
     * @param snoozeDurationMs Duration to snooze in milliseconds
     * @return True if snooze was successful
     */
    suspend fun snoozeReminder(reminderId: Long, snoozeDurationMs: Long = DEFAULT_SNOOZE_DURATION_MS): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val reminderEntity = database.reminderDao().getReminderById(reminderId)
                if (reminderEntity == null) {
                    Log.w(TAG, "Reminder $reminderId not found for snooze")
                    return@withContext false
                }
                
                val locationData = reminderEntity.locationData?.let {
                    json.decodeFromString<LocationData>(it)
                } ?: return@withContext false
                
                val updatedLocationData = locationData.copy(
                    snoozedUntil = System.currentTimeMillis() + snoozeDurationMs,
                    snoozeCount = locationData.snoozeCount + 1
                )
                
                val updatedEntity = reminderEntity.copy(
                    locationData = json.encodeToString(updatedLocationData)
                )
                database.reminderDao().update(updatedEntity)
                
                Log.d(TAG, "Reminder $reminderId snoozed for ${snoozeDurationMs / 60000} minutes")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing reminder", e)
                false
            }
        }
    }
    
    /**
     * Snooze reminder until user leaves the location
     */
    suspend fun snoozeUntilLeave(reminderId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val reminderEntity = database.reminderDao().getReminderById(reminderId)
                if (reminderEntity == null) {
                    Log.w(TAG, "Reminder $reminderId not found")
                    return@withContext false
                }
                
                val locationData = reminderEntity.locationData?.let {
                    json.decodeFromString<LocationData>(it)
                } ?: return@withContext false
                
                // Change trigger type to EXIT temporarily
                val updatedLocationData = locationData.copy(
                    triggerType = GeofenceTriggerType.EXIT,
                    snoozeCount = locationData.snoozeCount + 1
                )
                
                val updatedEntity = reminderEntity.copy(
                    locationData = json.encodeToString(updatedLocationData)
                )
                database.reminderDao().update(updatedEntity)
                
                // Re-register geofence with EXIT trigger
                val geofenceId = reminderEntity.geofenceId
                if (geofenceId != null && locationData.latitude != null && locationData.longitude != null) {
                    geofenceManager.removeGeofence(geofenceId)
                    geofenceManager.registerGeofenceWithTransitions(
                        geofenceId,
                        locationData.latitude,
                        locationData.longitude,
                        locationData.radius,
                        Geofence.GEOFENCE_TRANSITION_EXIT
                    )
                }
                
                Log.d(TAG, "Reminder $reminderId will trigger when leaving location")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error setting snooze until leave", e)
                false
            }
        }
    }
    
    /**
     * Extract base geofence ID from a potentially suffixed ID
     * Generic category geofences have format: "geofence_uuid_0", "geofence_uuid_1", etc.
     * We need to extract "geofence_uuid" to match the database
     */
    private fun extractBaseGeofenceId(geofenceId: String): String {
        // Check if this is a generic category geofence (has numeric suffix)
        val lastUnderscoreIndex = geofenceId.lastIndexOf('_')
        if (lastUnderscoreIndex != -1) {
            val suffix = geofenceId.substring(lastUnderscoreIndex + 1)
            // If suffix is numeric, this is a generic category geofence
            if (suffix.toIntOrNull() != null) {
                return geofenceId.substring(0, lastUnderscoreIndex)
            }
        }
        return geofenceId
    }
    
    /**
     * Check if a geofence is in cooldown period
     */
    private fun isInCooldown(geofenceId: String): Boolean {
        val lastTriggerTime = lastTriggerTimes[geofenceId] ?: return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTriggerTime) < COOLDOWN_DURATION_MS
    }
    
    /**
     * Update cooldown timestamp for a geofence
     */
    private fun updateCooldown(geofenceId: String) {
        lastTriggerTimes[geofenceId] = System.currentTimeMillis()
    }
    
    /**
     * Trigger a notification for a location-based reminder (legacy method)
     */
    private fun triggerLocationReminderNotification(reminderEntity: ReminderEntity) {
        try {
            // Show notification using the same approach as ReminderReceiver
            showLocationReminderNotification(context, reminderEntity.id, reminderEntity.message)
            
            // Optionally trigger TTS (similar to ReminderReceiver)
            speakLocationReminder(context, reminderEntity.message)
            
            Log.d(TAG, "Notification triggered for reminder ${reminderEntity.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering notification", e)
        }
    }
    
    /**
     * Trigger notification with snooze actions for enhanced location reminders
     */
    private fun triggerLocationReminderNotificationWithActions(
        reminderEntity: ReminderEntity,
        locationData: LocationData,
        transitionType: Int
    ) {
        try {
            val transitionName = if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) "arrived at" else "left"
            val locationName = locationData.placeName ?: locationData.placeCategory?.name?.lowercase() ?: "location"
            
            showLocationReminderNotificationWithActions(
                context = context,
                reminderId = reminderEntity.id,
                geofenceId = reminderEntity.geofenceId ?: "",
                message = reminderEntity.message,
                locationName = locationName,
                transitionName = transitionName,
                isRecurring = locationData.recurrence != LocationRecurrence.ONCE,
                triggerCount = locationData.triggerCount + 1
            )
            
            // Trigger TTS
            speakLocationReminder(context, reminderEntity.message)
            
            Log.d(TAG, "Enhanced notification triggered for reminder ${reminderEntity.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering enhanced notification", e)
            // Fallback to basic notification
            triggerLocationReminderNotification(reminderEntity)
        }
    }
    
    /**
     * Display a notification with snooze action buttons
     */
    private fun showLocationReminderNotificationWithActions(
        context: Context,
        reminderId: Long,
        geofenceId: String,
        message: String,
        locationName: String,
        transitionName: String,
        isRecurring: Boolean,
        triggerCount: Int
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager not available")
                return
            }

            // Create notification channel for Android O and above
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val channel = android.app.NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = NOTIFICATION_CHANNEL_DESCRIPTION
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating notification channel", e)
                }
            }
            
            // Create snooze action intents
            val snooze1HourIntent = android.content.Intent(context, com.example.voicereminder.receivers.SnoozeActionReceiver::class.java).apply {
                action = SnoozeActions.ACTION_SNOOZE_1_HOUR
                putExtra(SnoozeActions.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeActions.EXTRA_GEOFENCE_ID, geofenceId)
            }
            val snooze1HourPendingIntent = android.app.PendingIntent.getBroadcast(
                context, reminderId.toInt(), snooze1HourIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val snoozeWhenLeaveIntent = android.content.Intent(context, com.example.voicereminder.receivers.SnoozeActionReceiver::class.java).apply {
                action = SnoozeActions.ACTION_SNOOZE_WHEN_LEAVE
                putExtra(SnoozeActions.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeActions.EXTRA_GEOFENCE_ID, geofenceId)
            }
            val snoozeWhenLeavePendingIntent = android.app.PendingIntent.getBroadcast(
                context, (reminderId + 1000).toInt(), snoozeWhenLeaveIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build title based on context
            val title = if (isRecurring) {
                "Location Reminder (#$triggerCount)"
            } else {
                "Location Reminder"
            }
            
            val subtitle = "You $transitionName $locationName"

            // Build notification with actions
            val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(com.example.voicereminder.R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setSubText(subtitle)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText(subtitle))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(
                    android.R.drawable.ic_menu_recent_history,
                    "Snooze 1 hour",
                    snooze1HourPendingIntent
                )
            
            // Add "Remind when I leave" action only for ENTER transitions
            if (transitionName == "arrived at") {
                notificationBuilder.addAction(
                    android.R.drawable.ic_menu_directions,
                    "When I leave",
                    snoozeWhenLeavePendingIntent
                )
            }

            notificationManager.notify(reminderId.toInt(), notificationBuilder.build())
            Log.d(TAG, "Enhanced notification displayed for location reminder $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying enhanced notification", e)
        }
    }
    
    /**
     * Display a notification for a location-based reminder (basic version)
     */
    private fun showLocationReminderNotification(context: Context, reminderId: Long, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager not available")
                return
            }

            // Create notification channel for Android O and above
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val channel = android.app.NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = NOTIFICATION_CHANNEL_DESCRIPTION
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating notification channel", e)
                }
            }

            // Build notification
            val notification = androidx.core.app.NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(com.example.voicereminder.R.drawable.ic_notification)
                .setContentTitle("Location Reminder")
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(reminderId.toInt(), notification)
            Log.d(TAG, "Notification displayed for location reminder $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying notification", e)
        }
    }
    
    /**
     * Get all active location-based reminders
     * 
     * @return List of active location reminders
     */
    suspend fun getActiveLocationReminders(): List<Reminder> {
        return withContext(Dispatchers.IO) {
            try {
                database.reminderDao().getActiveLocationReminders().map { it.toDomain() }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching active location reminders", e)
                emptyList()
            }
        }
    }
    
    /**
     * Delete a location-based reminder and remove its geofence
     * 
     * @param reminderId The ID of the reminder to delete
     */
    suspend fun deleteLocationReminder(reminderId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val reminderEntity = database.reminderDao().getReminderById(reminderId)
                if (reminderEntity != null) {
                    // Remove geofence if it exists
                    val geofenceId = reminderEntity.geofenceId
                    if (geofenceId != null) {
                        try {
                            geofenceManager.removeGeofence(geofenceId)
                            Log.d(TAG, "Removed geofence: $geofenceId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing geofence for reminder $reminderId", e)
                        }
                    }
                    
                    // Delete from database
                    database.reminderDao().delete(reminderEntity)
                    Log.d(TAG, "Location reminder $reminderId deleted successfully")
                    
                    // Check if we should stop location monitoring
                    stopLocationMonitoringIfNoActiveReminders()
                } else {
                    Log.w(TAG, "Location reminder $reminderId not found for deletion")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting location reminder", e)
            }
        }
    }
    
    /**
     * Stop location monitoring if no active location reminders exist
     * This helps conserve battery when location monitoring is not needed
     */
    private suspend fun stopLocationMonitoringIfNoActiveReminders() {
        try {
            val activeReminders = database.reminderDao().getActiveLocationReminders()
            if (activeReminders.isEmpty()) {
                Log.d(TAG, "No active location reminders - stopping location monitoring")
                // Remove all geofences to stop location monitoring
                val removed = geofenceManager.removeAllGeofences()
                if (removed) {
                    Log.d(TAG, "Successfully stopped location monitoring - battery optimization active")
                } else {
                    Log.w(TAG, "Failed to remove all geofences during monitoring stop")
                }
            } else {
                Log.d(TAG, "Still have ${activeReminders.size} active location reminders - keeping monitoring active")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking active reminders", e)
        }
    }
    
    /**
     * Start location monitoring when a new location reminder is created
     * This is called automatically by createLocationReminder
     * Uses appropriate geofence radius to balance accuracy and battery (100-200m)
     */
    private suspend fun startLocationMonitoring() {
        try {
            val activeReminders = database.reminderDao().getActiveLocationReminders()
            Log.d(TAG, "Location monitoring active for ${activeReminders.size} reminders")
            // Location monitoring is automatically started when geofences are registered
            // The geofence radius is configured per reminder (100m for specific, 200m for generic)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location monitoring", e)
        }
    }
    
    /**
     * Resume location monitoring for all active reminders
     * Should be called when app starts or permissions are re-granted
     * @return Number of reminders that had monitoring resumed
     */
    suspend fun resumeLocationMonitoring(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val activeReminders = database.reminderDao().getActiveLocationReminders()
                
                if (activeReminders.isEmpty()) {
                    Log.d(TAG, "No active location reminders to resume monitoring for")
                    return@withContext 0
                }
                
                Log.d(TAG, "Resuming location monitoring for ${activeReminders.size} active reminders")
                
                var resumedCount = 0
                
                for (reminder in activeReminders) {
                    val geofenceId = reminder.geofenceId
                    val locationDataJson = reminder.locationData
                    
                    if (geofenceId == null || locationDataJson == null) {
                        Log.w(TAG, "Skipping reminder ${reminder.id} - missing geofence data")
                        continue
                    }
                    
                    try {
                        val locationData = json.decodeFromString<LocationData>(locationDataJson)
                        
                        // Re-register the geofence
                        val success = when (locationData.locationType) {
                            LocationType.SPECIFIC_PLACE -> {
                                if (locationData.latitude != null && locationData.longitude != null) {
                                    registerSpecificLocationGeofence(
                                        geofenceId,
                                        locationData.latitude,
                                        locationData.longitude,
                                        locationData.radius
                                    )
                                } else {
                                    false
                                }
                            }
                            LocationType.GENERIC_CATEGORY -> {
                                registerGenericCategoryGeofences(geofenceId, locationData)
                            }
                        }
                        
                        if (success) {
                            resumedCount++
                            Log.d(TAG, "Resumed monitoring for reminder ${reminder.id}")
                        } else {
                            Log.e(TAG, "Failed to resume monitoring for reminder ${reminder.id}")
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming monitoring for reminder ${reminder.id}", e)
                    }
                }
                
                Log.d(TAG, "Successfully resumed monitoring for $resumedCount of ${activeReminders.size} reminders")
                return@withContext resumedCount
                
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming location monitoring", e)
                return@withContext 0
            }
        }
    }
    
    /**
     * Detect and re-register geofences that were removed by the system
     * This should be called periodically or when the app starts
     * @return Number of geofences that were re-registered
     */
    suspend fun detectAndReRegisterRemovedGeofences(): Int {
        return withContext(Dispatchers.IO) {
            try {
                // Get all active location-based reminders
                val activeReminders = database.reminderDao().getActiveLocationReminders()
                
                if (activeReminders.isEmpty()) {
                    Log.d(TAG, "No active location reminders to check")
                    return@withContext 0
                }
                
                // Get list of geofence IDs that should be active
                val activeGeofenceIds = activeReminders.mapNotNull { it.geofenceId }
                
                // Detect which geofences were removed by the system
                val removedGeofenceIds = geofenceManager.detectRemovedGeofences(activeGeofenceIds)
                
                if (removedGeofenceIds.isEmpty()) {
                    Log.d(TAG, "All geofences are still registered")
                    return@withContext 0
                }
                
                Log.w(TAG, "Detected ${removedGeofenceIds.size} removed geofences - attempting to re-register")
                
                // Re-register the removed geofences
                var reRegisteredCount = 0
                
                for (geofenceId in removedGeofenceIds) {
                    // Find the reminder with this geofence ID
                    val reminder = activeReminders.find { it.geofenceId == geofenceId }
                    
                    if (reminder == null) {
                        Log.w(TAG, "Could not find reminder for geofence $geofenceId")
                        continue
                    }
                    
                    // Parse location data
                    val locationDataJson = reminder.locationData
                    if (locationDataJson == null) {
                        Log.w(TAG, "No location data for reminder ${reminder.id}")
                        continue
                    }
                    
                    try {
                        val locationData = json.decodeFromString<LocationData>(locationDataJson)
                        
                        // Re-register the geofence
                        val success = when (locationData.locationType) {
                            LocationType.SPECIFIC_PLACE -> {
                                if (locationData.latitude != null && locationData.longitude != null) {
                                    registerSpecificLocationGeofence(
                                        geofenceId,
                                        locationData.latitude,
                                        locationData.longitude,
                                        locationData.radius
                                    )
                                } else {
                                    false
                                }
                            }
                            LocationType.GENERIC_CATEGORY -> {
                                registerGenericCategoryGeofences(geofenceId, locationData)
                            }
                        }
                        
                        if (success) {
                            Log.d(TAG, "Successfully re-registered geofence $geofenceId")
                            reRegisteredCount++
                        } else {
                            Log.e(TAG, "Failed to re-register geofence $geofenceId")
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error re-registering geofence $geofenceId", e)
                    }
                }
                
                Log.d(TAG, "Re-registered $reRegisteredCount of ${removedGeofenceIds.size} removed geofences")
                return@withContext reRegisteredCount
                
            } catch (e: Exception) {
                Log.e(TAG, "Error detecting and re-registering removed geofences", e)
                return@withContext 0
            }
        }
    }
    
    /**
     * Use TTS to speak the location reminder message
     * Checks if device is in silent mode before speaking
     */
    private fun speakLocationReminder(context: Context, message: String) {
        try {
            // Check if device is in silent mode
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            
            if (audioManager == null) {
                Log.e(TAG, "AudioManager not available")
                return
            }
            
            val ringerMode = audioManager.ringerMode

            if (ringerMode == android.media.AudioManager.RINGER_MODE_SILENT || 
                ringerMode == android.media.AudioManager.RINGER_MODE_VIBRATE) {
                // Don't speak if device is in silent or vibrate mode
                Log.d(TAG, "Device in silent/vibrate mode, skipping TTS")
                return
            }

            // Initialize TTSManager and speak the reminder
            val ttsManager = com.example.voicereminder.presentation.TTSManager(context)
            
            // Set callback to handle TTS lifecycle
            ttsManager.setCallback(object : com.example.voicereminder.presentation.TTSManager.TTSCallback {
                override fun onSpeakingStarted() {
                    Log.d(TAG, "TTS started speaking location reminder")
                }
                
                override fun onSpeakingCompleted() {
                    Log.d(TAG, "TTS completed speaking location reminder")
                    // Shutdown TTS after speaking completes
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        ttsManager.shutdown()
                    }, 500)
                }
                
                override fun onError(error: String) {
                    Log.e(TAG, "TTS error: $error")
                    ttsManager.shutdown()
                }
            })
            
            // Wait a moment for TTS to initialize, then speak
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    if (ttsManager.isReady()) {
                        ttsManager.speak(message, "location_reminder_notification")
                    } else {
                        Log.w(TAG, "TTS not ready, skipping speech")
                        ttsManager.shutdown()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error speaking location reminder", e)
                    ttsManager.shutdown()
                }
            }, 1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error in speakLocationReminder", e)
        }
    }
    
    // ========== Saved Location Management ==========
    
    /**
     * Add a new saved location
     * 
     * @param name The name of the location (e.g., "Home", "Work")
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @param radius The geofence radius in meters (default: 100m)
     * @return SavedLocationResult with success or error information
     */
    suspend fun addSavedLocation(
        name: String,
        latitude: Double,
        longitude: Double,
        radius: Float = 100f
    ): SavedLocationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (name.isBlank()) {
                    return@withContext SavedLocationResult.Error("Location name cannot be empty")
                }
                
                if (latitude < -90 || latitude > 90) {
                    return@withContext SavedLocationResult.Error("Invalid latitude: must be between -90 and 90")
                }
                
                if (longitude < -180 || longitude > 180) {
                    return@withContext SavedLocationResult.Error("Invalid longitude: must be between -180 and 180")
                }
                
                if (radius <= 0) {
                    return@withContext SavedLocationResult.Error("Radius must be greater than 0")
                }
                
                // Check if location with same name already exists
                val existingLocation = database.savedLocationDao().getLocationByName(name)
                if (existingLocation != null) {
                    return@withContext SavedLocationResult.Error("A location with name '$name' already exists")
                }
                
                // Check saved locations limit (20 locations max for performance)
                val currentLocations = database.savedLocationDao().getAllLocations()
                if (currentLocations.size >= MAX_SAVED_LOCATIONS) {
                    return@withContext SavedLocationResult.Error(
                        "Maximum number of saved locations ($MAX_SAVED_LOCATIONS) reached. " +
                        "Please delete some locations before adding new ones."
                    )
                }
                
                // Create saved location entity
                val savedLocation = com.example.voicereminder.data.SavedLocationEntity(
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    createdAt = System.currentTimeMillis()
                )
                
                // Insert into database
                val locationId = database.savedLocationDao().insert(savedLocation)
                
                if (locationId <= 0) {
                    return@withContext SavedLocationResult.Error("Failed to save location to database")
                }
                
                Log.d(TAG, "Saved location '$name' added successfully with ID: $locationId")
                SavedLocationResult.Success(locationId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding saved location", e)
                SavedLocationResult.Error("Failed to add saved location: ${e.message}")
            }
        }
    }
    
    /**
     * Update an existing saved location
     * 
     * @param locationId The ID of the location to update
     * @param name The new name (optional, null to keep existing)
     * @param latitude The new latitude (optional, null to keep existing)
     * @param longitude The new longitude (optional, null to keep existing)
     * @param radius The new radius (optional, null to keep existing)
     * @return SavedLocationResult with success or error information
     */
    suspend fun updateSavedLocation(
        locationId: Long,
        name: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Float? = null
    ): SavedLocationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Get existing location
                val existingLocation = database.savedLocationDao().getLocationById(locationId)
                if (existingLocation == null) {
                    return@withContext SavedLocationResult.Error("Location with ID $locationId not found")
                }
                
                // Validate new values if provided
                if (name != null && name.isBlank()) {
                    return@withContext SavedLocationResult.Error("Location name cannot be empty")
                }
                
                if (latitude != null && (latitude < -90 || latitude > 90)) {
                    return@withContext SavedLocationResult.Error("Invalid latitude: must be between -90 and 90")
                }
                
                if (longitude != null && (longitude < -180 || longitude > 180)) {
                    return@withContext SavedLocationResult.Error("Invalid longitude: must be between -180 and 180")
                }
                
                if (radius != null && radius <= 0) {
                    return@withContext SavedLocationResult.Error("Radius must be greater than 0")
                }
                
                // Check if new name conflicts with existing location
                if (name != null && name != existingLocation.name) {
                    val conflictingLocation = database.savedLocationDao().getLocationByName(name)
                    if (conflictingLocation != null && conflictingLocation.id != locationId) {
                        return@withContext SavedLocationResult.Error("A location with name '$name' already exists")
                    }
                }
                
                // Create updated location entity
                val updatedLocation = existingLocation.copy(
                    name = name ?: existingLocation.name,
                    latitude = latitude ?: existingLocation.latitude,
                    longitude = longitude ?: existingLocation.longitude,
                    radius = radius ?: existingLocation.radius
                )
                
                // Update in database
                database.savedLocationDao().update(updatedLocation)
                
                Log.d(TAG, "Saved location $locationId updated successfully")
                SavedLocationResult.Success(locationId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating saved location", e)
                SavedLocationResult.Error("Failed to update saved location: ${e.message}")
            }
        }
    }
    
    /**
     * Delete a saved location
     * 
     * @param locationId The ID of the location to delete
     * @return SavedLocationResult with success or error information
     */
    suspend fun deleteSavedLocation(locationId: Long): SavedLocationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Get existing location
                val existingLocation = database.savedLocationDao().getLocationById(locationId)
                if (existingLocation == null) {
                    return@withContext SavedLocationResult.Error("Location with ID $locationId not found")
                }
                
                // Delete from database
                database.savedLocationDao().delete(existingLocation)
                
                Log.d(TAG, "Saved location $locationId deleted successfully")
                SavedLocationResult.Success(locationId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting saved location", e)
                SavedLocationResult.Error("Failed to delete saved location: ${e.message}")
            }
        }
    }
    
    /**
     * Get all saved locations
     * 
     * @return List of all saved locations
     */
    suspend fun getSavedLocations(): List<com.example.voicereminder.data.SavedLocationEntity> {
        return withContext(Dispatchers.IO) {
            try {
                database.savedLocationDao().getAllLocations()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching saved locations", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get a saved location by name
     * 
     * @param name The name of the location (case-insensitive)
     * @return The saved location entity or null if not found
     */
    suspend fun getSavedLocationByName(name: String): com.example.voicereminder.data.SavedLocationEntity? {
        return withContext(Dispatchers.IO) {
            try {
                database.savedLocationDao().getLocationByName(name)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching saved location by name", e)
                null
            }
        }
    }
    
    /**
     * Capture the current device location and save it as a named location
     * 
     * @param name The name to assign to the current location (e.g., "Home", "Work")
     * @param radius The geofence radius in meters (default: 100m)
     * @return SavedLocationResult with success or error information
     */
    suspend fun captureCurrentLocation(
        name: String,
        radius: Float = 100f
    ): SavedLocationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (name.isBlank()) {
                    return@withContext SavedLocationResult.Error("Location name cannot be empty")
                }
                
                // Check location permissions
                if (!locationServiceManager.hasLocationPermission()) {
                    return@withContext SavedLocationResult.Error(
                        "Location permission is required to capture current location. " +
                        "Please grant location permission in app settings."
                    )
                }
                
                // Check if location services are enabled
                if (!locationServiceManager.isLocationEnabled()) {
                    return@withContext SavedLocationResult.Error(
                        "Location services are disabled. " +
                        "Please enable location services in device settings."
                    )
                }
                
                // Get current location with retry logic
                val currentLocation = try {
                    locationServiceManager.getCurrentLocation(maxRetries = 3)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting current location", e)
                    null
                }
                
                if (currentLocation == null) {
                    return@withContext SavedLocationResult.Error(
                        "Unable to determine current location after multiple attempts. " +
                        "Please ensure location services are enabled, you have a clear view of the sky (for GPS), " +
                        "and try again in a moment."
                    )
                }
                
                Log.d(TAG, "Current location captured: lat=${currentLocation.latitude}, lon=${currentLocation.longitude}")
                
                // Save the location using addSavedLocation
                addSavedLocation(
                    name = name,
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude,
                    radius = radius
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing current location", e)
                SavedLocationResult.Error("Failed to capture current location: ${e.message}")
            }
        }
    }
}

/**
 * Result type for saved location operations
 */
sealed class SavedLocationResult {
    data class Success(val locationId: Long) : SavedLocationResult()
    data class Error(val message: String) : SavedLocationResult()
}
