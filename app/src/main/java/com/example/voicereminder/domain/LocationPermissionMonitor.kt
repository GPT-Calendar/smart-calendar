package com.example.voicereminder.domain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.voicereminder.R
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Monitors location permission changes and manages location-based reminders accordingly
 * Detects when permissions are revoked and disables location reminders
 * Re-enables location reminders when permissions are re-granted
 */
class LocationPermissionMonitor(
    private val context: Context,
    private val database: ReminderDatabase,
    private val locationServiceManager: LocationServiceManager,
    private val geofenceManager: GeofenceManager
) {
    
    companion object {
        private const val TAG = "LocationPermissionMonitor"
        private const val PREFS_NAME = "location_permission_prefs"
        private const val KEY_LAST_PERMISSION_STATE = "last_permission_state"
        private const val KEY_REMINDERS_DISABLED_COUNT = "reminders_disabled_count"
        private const val NOTIFICATION_ID_PERMISSION_REVOKED = 9001
        private const val NOTIFICATION_ID_PERMISSION_GRANTED = 9002
        private const val NOTIFICATION_CHANNEL_ID = "location_permission_notifications"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if location permissions have changed since last check
     * If permissions were revoked, disable location reminders
     * If permissions were granted, re-enable location reminders
     */
    suspend fun checkPermissionChanges(): Unit = withContext(Dispatchers.IO) {
        try {
            val currentPermissionState = locationServiceManager.hasLocationPermission()
            val lastPermissionState = prefs.getBoolean(KEY_LAST_PERMISSION_STATE, true)
            
            // Check if permission state changed
            when {
                currentPermissionState != lastPermissionState -> {
                    Log.d(TAG, "Location permission state changed: $lastPermissionState -> $currentPermissionState")

                    if (!currentPermissionState) {
                        // Permissions were revoked
                        handlePermissionRevoked()
                    } else {
                        // Permissions were granted
                        handlePermissionGranted()
                    }

                    // Update stored permission state
                    prefs.edit().putBoolean(KEY_LAST_PERMISSION_STATE, currentPermissionState).apply()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permission changes", e)
        }
    }
    
    /**
     * Handle the case when location permissions are revoked
     * Disables all active location-based reminders and shows notification
     */
    private suspend fun handlePermissionRevoked() {
        try {
            Log.w(TAG, "Location permissions revoked - disabling location reminders")
            
            // Get all active location-based reminders
            val activeLocationReminders = database.reminderDao().getActiveLocationReminders()
            
            if (activeLocationReminders.isEmpty()) {
                Log.d(TAG, "No active location reminders to disable")
                return
            }
            
            Log.d(TAG, "Disabling ${activeLocationReminders.size} location reminders")
            
            // Remove all geofences
            geofenceManager.removeAllGeofences()
            
            // Mark all location reminders as disabled (using CANCELLED status)
            var disabledCount = 0
            for (reminder in activeLocationReminders) {
                try {
                    val updatedReminder = reminder.copy(status = ReminderStatus.CANCELLED)
                    database.reminderDao().update(updatedReminder)
                    disabledCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Error disabling reminder ${reminder.id}", e)
                }
            }
            
            // Store the count of disabled reminders
            prefs.edit().putInt(KEY_REMINDERS_DISABLED_COUNT, disabledCount).apply()
            
            Log.d(TAG, "Disabled $disabledCount location reminders due to permission revocation")
            
            // Show notification to user
            showPermissionRevokedNotification(disabledCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling permission revocation", e)
        }
    }
    
    /**
     * Handle the case when location permissions are granted
     * Re-enables previously disabled location-based reminders and shows notification
     */
    private suspend fun handlePermissionGranted() {
        try {
            Log.d(TAG, "Location permissions granted - checking for disabled reminders")
            
            // Get the count of reminders that were disabled
            val disabledCount = prefs.getInt(KEY_REMINDERS_DISABLED_COUNT, 0)
            
            if (disabledCount == 0) {
                Log.d(TAG, "No reminders were previously disabled")
                return
            }
            
            // Get all cancelled location-based reminders (these were disabled due to permission revocation)
            val cancelledLocationReminders = database.reminderDao().getCancelledLocationReminders()
            
            if (cancelledLocationReminders.isEmpty()) {
                Log.d(TAG, "No cancelled location reminders to re-enable")
                prefs.edit().putInt(KEY_REMINDERS_DISABLED_COUNT, 0).apply()
                return
            }
            
            Log.d(TAG, "Re-enabling ${cancelledLocationReminders.size} location reminders")
            
            // Re-enable the reminders by changing status back to PENDING
            var reEnabledCount = 0
            for (reminder in cancelledLocationReminders) {
                try {
                    val updatedReminder = reminder.copy(status = ReminderStatus.PENDING)
                    database.reminderDao().update(updatedReminder)
                    reEnabledCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Error re-enabling reminder ${reminder.id}", e)
                }
            }
            
            Log.d(TAG, "Re-enabled $reEnabledCount location reminders")
            
            // Clear the disabled count
            prefs.edit().putInt(KEY_REMINDERS_DISABLED_COUNT, 0).apply()
            
            // Show notification to user
            showPermissionGrantedNotification(reEnabledCount)
            
            // Note: Geofences will be re-registered when the app is next opened
            // or by the BootReceiver on next device restart
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling permission grant", e)
        }
    }
    
    /**
     * Show notification when location permissions are revoked
     */
    private fun showPermissionRevokedNotification(disabledCount: Int) {
        try {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Location Reminders Disabled")
                .setContentText("$disabledCount location-based reminder${if (disabledCount != 1) "s" else ""} disabled due to missing location permission.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Location permission was revoked. $disabledCount location-based reminder${if (disabledCount != 1) "s have" else " has"} been disabled. " +
                            "Grant location permission in app settings to re-enable location reminders."
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            val notificationManager = NotificationManagerCompat.from(context)
            
            // Create notification channel if needed (Android O+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Location Permission Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about location permission changes"
                }
                
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                systemNotificationManager?.createNotificationChannel(channel)
            }
            
            notificationManager.notify(NOTIFICATION_ID_PERMISSION_REVOKED, notification)
            Log.d(TAG, "Permission revoked notification shown")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing permission revoked notification", e)
        }
    }
    
    /**
     * Show notification when location permissions are granted
     */
    private fun showPermissionGrantedNotification(reEnabledCount: Int) {
        try {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Location Reminders Re-enabled")
                .setContentText("$reEnabledCount location-based reminder${if (reEnabledCount != 1) "s" else ""} re-enabled.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Location permission granted. $reEnabledCount location-based reminder${if (reEnabledCount != 1) "s have" else " has"} been re-enabled. " +
                            "Your location reminders will now trigger when you reach the specified locations."
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            val notificationManager = NotificationManagerCompat.from(context)
            
            // Create notification channel if needed (Android O+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Location Permission Notifications",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about location permission changes"
                }
                
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                systemNotificationManager?.createNotificationChannel(channel)
            }
            
            notificationManager.notify(NOTIFICATION_ID_PERMISSION_GRANTED, notification)
            Log.d(TAG, "Permission granted notification shown")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing permission granted notification", e)
        }
    }
    
    /**
     * Initialize the permission monitor
     * Should be called when the app starts
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                // Store initial permission state if not already stored
                if (!prefs.contains(KEY_LAST_PERMISSION_STATE)) {
                    val currentPermissionState = locationServiceManager.hasLocationPermission()
                    prefs.edit().putBoolean(KEY_LAST_PERMISSION_STATE, currentPermissionState).apply()
                    Log.d(TAG, "Initialized permission monitor with state: $currentPermissionState")
                }
                
                // Check for permission changes on initialization
                checkPermissionChanges()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing permission monitor", e)
            }
        }
    }
}
