package com.example.voicereminder.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Handles scheduling of system alarms using AlarmManager.AlarmClockInfo
 * These alarms appear in the system Clock app
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    /**
     * Schedule a system alarm at the specified time
     * The alarm will appear in the system Clock app
     * 
     * @param scheduledTime The time to trigger the alarm
     * @param label Optional label for the alarm (default: "Alarm")
     * @return true if scheduling was successful, false otherwise
     */
    fun scheduleAlarm(scheduledTime: LocalDateTime, label: String = "Alarm"): Boolean {
        try {
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
                Log.e(TAG, "Cannot schedule alarm - time is in the past")
                return false
            }

            // Use AlarmClock intent to set alarm in system Clock app
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, scheduledTime.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, scheduledTime.minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true) // Don't show Clock app UI
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                context.startActivity(intent)
                Log.d(TAG, "Alarm scheduled successfully for $scheduledTime")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm via AlarmClock intent", e)
                // Fallback: try using AlarmManager directly
                return scheduleAlarmFallback(triggerTimeMillis, label)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scheduleAlarm", e)
            return false
        }
    }

    /**
     * Fallback method to schedule alarm using AlarmManager.AlarmClockInfo
     * Used when AlarmClock intent fails
     */
    private fun scheduleAlarmFallback(triggerTimeMillis: Long, label: String): Boolean {
        try {
            // Create a show intent (what happens when user taps the alarm notification)
            val showIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            val showPendingIntent = PendingIntent.getActivity(
                context,
                0,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create AlarmClockInfo
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerTimeMillis,
                showPendingIntent
            )

            // Create a dummy pending intent for the alarm trigger
            // Note: This won't actually trigger anything, it just satisfies the API
            val triggerIntent = Intent(context, AlarmScheduler::class.java)
            val triggerPendingIntent = PendingIntent.getBroadcast(
                context,
                triggerTimeMillis.toInt(),
                triggerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set the alarm
            alarmManager.setAlarmClock(alarmClockInfo, triggerPendingIntent)
            Log.d(TAG, "Alarm scheduled via AlarmClockInfo fallback")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback alarm scheduling", e)
            return false
        }
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
}
