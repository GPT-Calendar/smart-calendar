package com.example.voicereminder.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.voicereminder.R
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver for handling alarm triggers
 */
class AlarmTriggerReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmTriggerReceiver"
        const val CHANNEL_ID = "alarm_channel"
        const val CHANNEL_NAME = "Alarms"
        
        const val ACTION_DISMISS = "com.example.voicereminder.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.example.voicereminder.ACTION_SNOOZE_ALARM"
        
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_SNOOZE_DURATION = "snooze_duration"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        
        if (alarmId == -1L) {
            Log.e(TAG, "Invalid alarm ID received")
            return
        }
        
        Log.d(TAG, "Alarm triggered: ID=$alarmId, isSnooze=$isSnooze")
        
        // Handle the alarm in a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            val alarmManager = EnhancedAlarmManager.getInstance(context)
            val alarm = alarmManager.getAlarmById(alarmId)
            
            if (alarm == null) {
                Log.e(TAG, "Alarm not found: $alarmId")
                return@launch
            }
            
            // Show notification
            showAlarmNotification(context, alarmId, alarm.label, alarm.vibrate)
        }
    }
    
    private fun showAlarmNotification(
        context: Context,
        alarmId: Long,
        label: String,
        vibrate: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                enableVibration(vibrate)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create full-screen intent to open alarm screen
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_alarm", true)
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create dismiss action
        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10 + 1).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create snooze action (5 minutes default)
        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_SNOOZE_DURATION, 5)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .addAction(0, "Snooze", snoozePendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()
        
        notificationManager.notify(alarmId.toInt(), notification)
        
        // Vibrate if enabled
        if (vibrate) {
            vibrateDevice(context)
        }
    }
    
    private fun vibrateDevice(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Vibration pattern: wait 0ms, vibrate 500ms, wait 200ms, vibrate 500ms (repeat)
            val pattern = longArrayOf(0, 500, 200, 500)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500), 0)
        }
    }
}

/**
 * Broadcast receiver for handling alarm actions (dismiss, snooze)
 */
class AlarmActionReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmActionReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmTriggerReceiver.EXTRA_ALARM_ID, -1)
        
        if (alarmId == -1L) {
            Log.e(TAG, "Invalid alarm ID received")
            return
        }
        
        // Cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.toInt())
        
        // Stop vibration
        stopVibration(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val alarmManager = EnhancedAlarmManager.getInstance(context)
            
            when (intent.action) {
                AlarmTriggerReceiver.ACTION_DISMISS -> {
                    Log.d(TAG, "Dismissing alarm: $alarmId")
                    alarmManager.dismissAlarm(alarmId)
                }
                AlarmTriggerReceiver.ACTION_SNOOZE -> {
                    val duration = intent.getIntExtra(AlarmTriggerReceiver.EXTRA_SNOOZE_DURATION, 5)
                    Log.d(TAG, "Snoozing alarm: $alarmId for $duration minutes")
                    alarmManager.snoozeAlarm(alarmId, duration)
                }
            }
        }
    }
    
    private fun stopVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }
}
