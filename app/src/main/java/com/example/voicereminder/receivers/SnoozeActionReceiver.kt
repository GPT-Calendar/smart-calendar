package com.example.voicereminder.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.domain.GeofenceManager
import com.example.voicereminder.domain.LocationReminderManager
import com.example.voicereminder.domain.LocationServiceManager
import com.example.voicereminder.domain.PlaceResolver
import com.example.voicereminder.domain.SnoozeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles snooze actions from reminder and alarm notifications
 */
class SnoozeActionReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SnoozeActionReceiver"
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
        private const val NEXT_VISIT_SNOOZE_MS = 4 * 60 * 60 * 1000L // 4 hours
        
        // New action constants for time-based reminders
        const val ACTION_SNOOZE_REMINDER = "com.example.voicereminder.ACTION_SNOOZE_REMINDER"
        const val ACTION_SNOOZE_ALARM = "com.example.voicereminder.ACTION_SNOOZE_ALARM"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_ITEM_TYPE = "item_type" // "reminder" or "alarm"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        
        const val TYPE_REMINDER = "reminder"
        const val TYPE_ALARM = "alarm"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // Check for new snooze actions first
        when (intent.action) {
            ACTION_SNOOZE_REMINDER, ACTION_SNOOZE_ALARM -> {
                handleGenericSnooze(context, intent)
                return
            }
        }
        
        // Handle legacy location reminder snooze actions
        val reminderId = intent.getLongExtra(LocationReminderManager.SnoozeActions.EXTRA_REMINDER_ID, -1)
        val geofenceId = intent.getStringExtra(LocationReminderManager.SnoozeActions.EXTRA_GEOFENCE_ID) ?: ""
        
        if (reminderId == -1L) {
            Log.e(TAG, "Invalid reminder ID in snooze action")
            return
        }
        
        Log.d(TAG, "Snooze action received: ${intent.action} for reminder $reminderId")
        
        // Dismiss the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(reminderId.toInt())
        
        // Use goAsync for background work
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ReminderDatabase.getDatabase(context)
                val geofenceManager = GeofenceManager(context)
                val placeResolver = PlaceResolver(context, database)
                val locationServiceManager = LocationServiceManager(context)
                
                val locationReminderManager = LocationReminderManager(
                    database = database,
                    geofenceManager = geofenceManager,
                    placeResolver = placeResolver,
                    locationServiceManager = locationServiceManager,
                    context = context
                )
                
                when (intent.action) {
                    LocationReminderManager.SnoozeActions.ACTION_SNOOZE_1_HOUR -> {
                        val success = locationReminderManager.snoozeReminder(reminderId, ONE_HOUR_MS)
                        showToast(context, if (success) "Snoozed for 1 hour" else "Failed to snooze")
                    }
                    
                    LocationReminderManager.SnoozeActions.ACTION_SNOOZE_NEXT_VISIT -> {
                        val success = locationReminderManager.snoozeReminder(reminderId, NEXT_VISIT_SNOOZE_MS)
                        showToast(context, if (success) "Will remind on next visit" else "Failed to snooze")
                    }
                    
                    LocationReminderManager.SnoozeActions.ACTION_SNOOZE_WHEN_LEAVE -> {
                        val success = locationReminderManager.snoozeUntilLeave(reminderId)
                        showToast(context, if (success) "Will remind when you leave" else "Failed to set reminder")
                    }
                    
                    else -> {
                        Log.w(TAG, "Unknown snooze action: ${intent.action}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling snooze action", e)
                showToast(context, "Error: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle generic snooze for time-based reminders and alarms
     */
    private fun handleGenericSnooze(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE) ?: TYPE_REMINDER
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
        
        if (itemId == -1L) {
            Log.e(TAG, "Invalid item ID in snooze action")
            return
        }
        
        Log.d(TAG, "Generic snooze: type=$itemType, id=$itemId, minutes=$snoozeMinutes")
        
        // Dismiss the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(itemId.toInt())
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snoozeManager = SnoozeManager.getInstance(context)
                
                val success = when (itemType) {
                    TYPE_REMINDER -> {
                        snoozeManager.snoozeReminder(itemId, snoozeMinutes)
                    }
                    TYPE_ALARM -> {
                        snoozeManager.snoozeAlarm(itemId, snoozeMinutes)
                    }
                    else -> {
                        Log.w(TAG, "Unknown item type: $itemType")
                        false
                    }
                }
                
                val message = if (success) {
                    when {
                        snoozeMinutes >= 60 -> "Snoozed for ${snoozeMinutes / 60} hour${if (snoozeMinutes >= 120) "s" else ""}"
                        else -> "Snoozed for $snoozeMinutes minutes"
                    }
                } else {
                    "Failed to snooze"
                }
                
                showToast(context, message)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling generic snooze", e)
                showToast(context, "Error: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
