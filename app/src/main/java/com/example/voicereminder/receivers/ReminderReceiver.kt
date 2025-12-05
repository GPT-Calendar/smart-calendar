package com.example.voicereminder.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.voicereminder.R
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.domain.NotificationScheduler
import com.example.voicereminder.presentation.MainActivity
import com.example.voicereminder.presentation.TTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BroadcastReceiver that handles scheduled reminder notifications
 * Displays notification and triggers TTS to speak the reminder message
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        private const val CHANNEL_ID = "reminder_notifications"
        private const val CHANNEL_NAME = "Reminder Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for scheduled reminders"
        
        // Action constants for notification buttons
        const val ACTION_SNOOZE = "com.example.voicereminder.ACTION_SNOOZE"
        const val ACTION_MARK_DONE = "com.example.voicereminder.ACTION_MARK_DONE"
        const val SNOOZE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    private var ttsManager: TTSManager? = null

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val reminderId = intent.getLongExtra(NotificationScheduler.EXTRA_REMINDER_ID, -1L)
        val reminderMessage = intent.getStringExtra(NotificationScheduler.EXTRA_REMINDER_MESSAGE)

        // Handle action button clicks
        when (action) {
            ACTION_SNOOZE -> {
                handleSnooze(context, reminderId, reminderMessage)
                return
            }
            ACTION_MARK_DONE -> {
                handleMarkDone(context, reminderId)
                return
            }
        }

        // Regular reminder trigger
        if (reminderId == -1L || reminderMessage == null) {
            android.util.Log.e(TAG, "Invalid reminder data in intent")
            return
        }

        android.util.Log.d(TAG, "Received reminder notification for ID: $reminderId")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ReminderDatabase.getDatabase(context)
                val reminderEntity = try {
                    database.reminderDao().getReminderById(reminderId)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Database error fetching reminder $reminderId", e)
                    null
                }

                if (reminderEntity != null) {
                    withContext(Dispatchers.Main) {
                        try {
                            showNotification(context, reminderId, reminderMessage)
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Error showing notification for reminder $reminderId", e)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        try {
                            speakReminder(context, reminderMessage)
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Error speaking reminder $reminderId", e)
                        }
                    }

                    // Don't mark as completed yet - let user snooze or mark done
                } else {
                    android.util.Log.w(TAG, "Reminder $reminderId not found in database")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Unexpected error processing reminder $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Handle snooze action - reschedule reminder for 5 minutes later
     */
    private fun handleSnooze(context: Context, reminderId: Long, message: String?) {
        if (reminderId == -1L) return
        
        android.util.Log.d(TAG, "Snoozing reminder $reminderId for 5 minutes")
        
        // Dismiss current notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())
        
        // Schedule new alarm for 5 minutes later
        val snoozeTime = System.currentTimeMillis() + SNOOZE_DURATION_MS
        
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NotificationScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationScheduler.EXTRA_REMINDER_MESSAGE, message ?: "Reminder")
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            snoozeTime,
            pendingIntent
        )
        
        android.util.Log.d(TAG, "Reminder $reminderId snoozed until ${java.util.Date(snoozeTime)}")
    }

    /**
     * Handle mark done action - complete the reminder
     */
    private fun handleMarkDone(context: Context, reminderId: Long) {
        if (reminderId == -1L) return
        
        android.util.Log.d(TAG, "Marking reminder $reminderId as done")
        
        // Dismiss notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())
        
        // Update database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ReminderDatabase.getDatabase(context)
                val reminderEntity = database.reminderDao().getReminderById(reminderId)
                if (reminderEntity != null) {
                    val updatedReminder = reminderEntity.copy(status = ReminderStatus.COMPLETED)
                    database.reminderDao().update(updatedReminder)
                    android.util.Log.d(TAG, "Reminder $reminderId marked as completed")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error marking reminder $reminderId as done", e)
            }
        }
    }

    /**
     * Display a notification with the reminder message and action buttons
     */
    private fun showNotification(context: Context, reminderId: Long, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            
            if (notificationManager == null) {
                android.util.Log.e(TAG, "NotificationManager not available")
                return
            }

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = CHANNEL_DESCRIPTION
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error creating notification channel", e)
                }
            }

            // Content intent - opens app when notification is tapped
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                reminderId.toInt(),
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Snooze action
            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(NotificationScheduler.EXTRA_REMINDER_ID, reminderId)
                putExtra(NotificationScheduler.EXTRA_REMINDER_MESSAGE, message)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                (reminderId * 10 + 1).toInt(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Mark done action
            val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_MARK_DONE
                putExtra(NotificationScheduler.EXTRA_REMINDER_ID, reminderId)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context,
                (reminderId * 10 + 2).toInt(),
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification with actions
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⏰ Reminder")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .addAction(0, "Snooze 5m", snoozePendingIntent)
                .addAction(0, "Done", donePendingIntent)
                .build()

            notificationManager.notify(reminderId.toInt(), notification)
            android.util.Log.d(TAG, "Notification displayed for reminder $reminderId")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error displaying notification", e)
        }
    }

    /**
     * Use TTS to speak the reminder message
     * Checks if device is in silent mode before speaking
     */
    private fun speakReminder(context: Context, message: String) {
        try {
            // Check if device is in silent mode
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            
            if (audioManager == null) {
                android.util.Log.e(TAG, "AudioManager not available")
                return
            }
            
            val ringerMode = audioManager.ringerMode

            if (ringerMode == AudioManager.RINGER_MODE_SILENT || 
                ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                // Don't speak if device is in silent or vibrate mode
                android.util.Log.d(TAG, "Device in silent/vibrate mode, skipping TTS")
                return
            }

            // Initialize TTSManager and speak the reminder
            ttsManager = TTSManager(context)
            
            // Set callback to handle TTS lifecycle
            ttsManager?.setCallback(object : TTSManager.TTSCallback {
                override fun onSpeakingStarted() {
                    android.util.Log.d(TAG, "TTS started speaking reminder")
                }
                
                override fun onSpeakingCompleted() {
                    android.util.Log.d(TAG, "TTS completed speaking reminder")
                    // Shutdown TTS after speaking completes
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        ttsManager?.shutdown()
                        ttsManager = null
                    }, 500)
                }
                
                override fun onError(error: String) {
                    android.util.Log.e(TAG, "TTS error: $error")
                    // TTS error - gracefully handle by not speaking
                    ttsManager?.shutdown()
                    ttsManager = null
                }
            })
            
            // Wait a moment for TTS to initialize, then speak
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    if (ttsManager?.isReady() == true) {
                        ttsManager?.speak(message, "reminder_notification")
                    } else {
                        android.util.Log.w(TAG, "TTS not ready, skipping speech")
                        // TTS not ready - gracefully handle by not speaking
                        ttsManager?.shutdown()
                        ttsManager = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error speaking reminder", e)
                    ttsManager?.shutdown()
                    ttsManager = null
                }
            }, 1000)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in speakReminder", e)
        }
    }
}
