package com.example.voicereminder.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.receivers.ReminderReceiver
import java.time.ZoneId

/**
 * Handles scheduling and canceling of reminder notifications using AlarmManager
 */
class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "NotificationScheduler"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_MESSAGE = "reminder_message"
    }

    /**
     * Schedule a reminder notification at the specified time
     * Uses exact alarms for precise timing
     * 
     * @param reminder The reminder to schedule
     * @return true if scheduling was successful, false otherwise
     */
    fun scheduleReminder(reminder: Reminder): Boolean {
        try {
            // Only schedule time-based reminders (location-based reminders use geofencing)
            val scheduledTime = reminder.scheduledTime
            if (scheduledTime == null) {
                Log.w(TAG, "Cannot schedule reminder ${reminder.id} - no scheduled time (likely location-based)")
                return false
            }
            
            // Check if we have permission to schedule exact alarms on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                    return false
                }
            }

            val triggerTimeMillis = scheduledTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // Don't schedule if the time is in the past
            if (triggerTimeMillis <= System.currentTimeMillis()) {
                Log.e(TAG, "Cannot schedule reminder ${reminder.id} - time is in the past")
                return false
            }

            val pendingIntent = createPendingIntent(reminder)

            try {
                // Use setExactAndAllowWhileIdle for precise timing even in Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Reminder ${reminder.id} scheduled successfully for $scheduledTime")
                return true
            } catch (e: SecurityException) {
                // Handle case where exact alarm permission is not granted
                Log.e(TAG, "SecurityException while scheduling reminder ${reminder.id}", e)
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error while scheduling reminder ${reminder.id}", e)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scheduleReminder for reminder ${reminder.id}", e)
            return false
        }
    }

    /**
     * Cancel a scheduled reminder notification
     * 
     * @param reminderId The ID of the reminder to cancel
     */
    fun cancelReminder(reminderId: Long) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Reminder $reminderId cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling reminder $reminderId", e)
        }
    }

    /**
     * Reschedule all active reminders (typically called after device boot)
     * 
     * @param reminders List of reminders to reschedule
     */
    fun rescheduleAllReminders(reminders: List<Reminder>) {
        Log.d(TAG, "Rescheduling ${reminders.size} reminders")
        var successCount = 0
        var failureCount = 0
        
        reminders.forEach { reminder ->
            if (scheduleReminder(reminder)) {
                successCount++
            } else {
                failureCount++
                Log.w(TAG, "Failed to reschedule reminder ${reminder.id}")
            }
        }
        
        Log.d(TAG, "Rescheduling complete: $successCount succeeded, $failureCount failed")
    }

    /**
     * Check if the app can schedule exact alarms
     * Required for Android 12+ (API 31+)
     * 
     * @return true if exact alarms can be scheduled, false otherwise
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // No permission needed on older versions
        }
    }

    /**
     * Create a PendingIntent for the ReminderReceiver
     */
    private fun createPendingIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_MESSAGE, reminder.message)
        }

        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(), // Use reminder ID as request code for uniqueness
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
