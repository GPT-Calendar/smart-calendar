package com.example.voicereminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.voicereminder.data.LocationData
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderType
import com.example.voicereminder.domain.GeofenceManager
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.domain.NotificationScheduler
import com.example.voicereminder.domain.models.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * BroadcastReceiver that reschedules all active reminders after device boot
 * Ensures reminders persist across device restarts
 * Also re-registers geofences for location-based reminders
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Verify this is a BOOT_COMPLETED action
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d(TAG, "Device boot completed - rescheduling reminders")

        // Use goAsync to allow background work
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get database instance
                val database = ReminderDatabase.getDatabase(context)
                
                // Fetch all active (pending) reminders from database
                val activeReminderEntities = database.reminderDao().getActiveReminders()
                
                // Convert entities to domain models
                val activeReminders = activeReminderEntities.map { it.toDomain() }
                
                // Initialize NotificationScheduler
                val notificationScheduler = NotificationScheduler(context)
                
                // Reschedule all pending time-based reminders
                notificationScheduler.rescheduleAllReminders(activeReminders)
                
                Log.d(TAG, "Rescheduled ${activeReminders.size} time-based reminders")
                
                // Re-register geofences for location-based reminders
                reRegisterGeofences(context, database)
                
                // Reschedule all enabled alarms
                rescheduleAlarms(context)
                
            } catch (e: Exception) {
                // Log error but don't crash
                Log.e(TAG, "Error rescheduling reminders after boot", e)
                e.printStackTrace()
            } finally {
                // Signal that the async work is complete
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Re-register all geofences for active location-based reminders
     * @param context Application context
     * @param database Database instance
     */
    private suspend fun reRegisterGeofences(context: Context, database: ReminderDatabase) {
        try {
            // Fetch all active location-based reminders
            val locationReminders = database.reminderDao().getActiveLocationReminders()
            
            if (locationReminders.isEmpty()) {
                Log.d(TAG, "No active location-based reminders to re-register")
                return
            }
            
            Log.d(TAG, "Re-registering geofences for ${locationReminders.size} location-based reminders")
            
            // Initialize GeofenceManager
            val geofenceManager = GeofenceManager(context)
            
            // Re-register each geofence
            var successCount = 0
            var failureCount = 0
            
            for (reminderEntity in locationReminders) {
                try {
                    // Skip if no geofence ID or location data
                    if (reminderEntity.geofenceId == null || reminderEntity.locationData == null) {
                        Log.w(TAG, "Skipping reminder ${reminderEntity.id} - missing geofence data")
                        failureCount++
                        continue
                    }
                    
                    // Parse location data from JSON
                    val locationData = Json.decodeFromString<LocationData>(reminderEntity.locationData)
                    
                    // Skip if no coordinates
                    if (locationData.latitude == null || locationData.longitude == null) {
                        Log.w(TAG, "Skipping reminder ${reminderEntity.id} - missing coordinates")
                        failureCount++
                        continue
                    }
                    
                    // Re-register the geofence
                    val success = geofenceManager.registerGeofence(
                        geofenceId = reminderEntity.geofenceId,
                        latitude = locationData.latitude,
                        longitude = locationData.longitude,
                        radius = locationData.radius
                    )
                    
                    if (success) {
                        Log.d(TAG, "Successfully re-registered geofence for reminder ${reminderEntity.id}")
                        successCount++
                    } else {
                        Log.e(TAG, "Failed to re-register geofence for reminder ${reminderEntity.id}")
                        failureCount++
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error re-registering geofence for reminder ${reminderEntity.id}", e)
                    failureCount++
                }
            }
            
            Log.d(TAG, "Geofence re-registration complete: $successCount succeeded, $failureCount failed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error re-registering geofences", e)
        }
    }
    
    /**
     * Reschedule all enabled alarms after boot
     * @param context Application context
     */
    private suspend fun rescheduleAlarms(context: Context) {
        try {
            Log.d(TAG, "Rescheduling alarms after boot")
            val alarmManager = EnhancedAlarmManager.getInstance(context)
            alarmManager.rescheduleAllAlarms()
            Log.d(TAG, "Alarms rescheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarms", e)
        }
    }
}
