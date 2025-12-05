package com.example.voicereminder.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Repository for persisting all app settings using DataStore
 */
class AppSettingsRepository(private val context: Context) {
    
    companion object {
        // Display & Accessibility
        private val FONT_SCALE = stringPreferencesKey("font_scale")
        private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        private val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        private val COMPACT_MODE = booleanPreferencesKey("compact_mode")
        private val ANIMATIONS = booleanPreferencesKey("animations")
        
        // Voice & Wake Word
        private val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        private val WAKE_WORD_SENSITIVITY = stringPreferencesKey("wake_word_sensitivity")
        private val VOICE_FEEDBACK = booleanPreferencesKey("voice_feedback")
        private val READ_ALOUD = booleanPreferencesKey("read_aloud")
        
        // Calendar
        private val CALENDAR_SYNC = booleanPreferencesKey("calendar_sync")
        private val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        private val WEEK_STARTS_ON = stringPreferencesKey("week_starts_on")
        private val SHOW_WEEK_NUMBERS = booleanPreferencesKey("show_week_numbers")
        
        // Finance
        private val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        private val SMS_TRACKING = booleanPreferencesKey("sms_tracking")
        private val SHOW_CENTS = booleanPreferencesKey("show_cents")
        private val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        private val BUDGET_ALERT_THRESHOLD = intPreferencesKey("budget_alert_threshold")
        
        // Location
        private val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        private val GEOFENCING_ENABLED = booleanPreferencesKey("geofencing_enabled")
        private val LOCATION_ACCURACY = stringPreferencesKey("location_accuracy")
        private val GEOFENCE_RADIUS = intPreferencesKey("geofence_radius")
        
        // Notifications
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val REMINDER_NOTIFICATIONS = booleanPreferencesKey("reminder_notifications")
        private val FINANCE_NOTIFICATIONS = booleanPreferencesKey("finance_notifications")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
        private val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
        
        // AI Advanced
        private val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        private val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        private val AI_CONTEXT_LENGTH = intPreferencesKey("ai_context_length")
        private val STREAM_RESPONSES = booleanPreferencesKey("stream_responses")
        
        // Widget
        private val WIDGET_THEME = stringPreferencesKey("widget_theme")
        private val WIDGET_TRANSPARENCY = intPreferencesKey("widget_transparency")
        private val WIDGET_QUICK_ACTIONS = booleanPreferencesKey("widget_quick_actions")
        
        // Data & Privacy
        private val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val CRASH_REPORTING = booleanPreferencesKey("crash_reporting")
        private val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        private val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        
        @Volatile
        private var instance: AppSettingsRepository? = null
        
        fun getInstance(context: Context): AppSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: AppSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    
    val appSettings: Flow<AppSettings> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings(
            fontScale = FontScale.valueOf(prefs[FONT_SCALE] ?: FontScale.NORMAL.name),
            highContrast = prefs[HIGH_CONTRAST] ?: false,
            reduceMotion = prefs[REDUCE_MOTION] ?: false,
            compactMode = prefs[COMPACT_MODE] ?: false,
            animations = prefs[ANIMATIONS] ?: true,
            
            wakeWordEnabled = prefs[WAKE_WORD_ENABLED] ?: false,
            wakeWordSensitivity = WakeWordSensitivity.valueOf(prefs[WAKE_WORD_SENSITIVITY] ?: WakeWordSensitivity.MEDIUM.name),
            voiceFeedback = prefs[VOICE_FEEDBACK] ?: true,
            readAloudResponses = prefs[READ_ALOUD] ?: false,
            
            calendarSyncEnabled = prefs[CALENDAR_SYNC] ?: true,
            defaultReminderMinutes = prefs[DEFAULT_REMINDER_MINUTES] ?: 15,
            weekStartsOn = WeekStart.valueOf(prefs[WEEK_STARTS_ON] ?: WeekStart.SUNDAY.name),
            showWeekNumbers = prefs[SHOW_WEEK_NUMBERS] ?: false,
            
            defaultCurrency = prefs[DEFAULT_CURRENCY] ?: "USD",
            smsTrackingEnabled = prefs[SMS_TRACKING] ?: true,
            showCentsInAmounts = prefs[SHOW_CENTS] ?: true,
            budgetAlerts = prefs[BUDGET_ALERTS] ?: true,
            budgetAlertThreshold = prefs[BUDGET_ALERT_THRESHOLD] ?: 80,
            
            locationEnabled = prefs[LOCATION_ENABLED] ?: true,
            geofencingEnabled = prefs[GEOFENCING_ENABLED] ?: true,
            locationAccuracy = LocationAccuracy.valueOf(prefs[LOCATION_ACCURACY] ?: LocationAccuracy.BALANCED.name),
            geofenceRadius = prefs[GEOFENCE_RADIUS] ?: 100,
            
            notificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
            reminderNotifications = prefs[REMINDER_NOTIFICATIONS] ?: true,
            financeNotifications = prefs[FINANCE_NOTIFICATIONS] ?: true,
            soundEnabled = prefs[SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[VIBRATION_ENABLED] ?: true,
            quietHoursEnabled = prefs[QUIET_HOURS_ENABLED] ?: false,
            quietHoursStart = prefs[QUIET_HOURS_START] ?: "22:00",
            quietHoursEnd = prefs[QUIET_HOURS_END] ?: "07:00",
            
            aiTemperature = prefs[AI_TEMPERATURE] ?: 0.7f,
            aiMaxTokens = prefs[AI_MAX_TOKENS] ?: 1024,
            aiContextLength = prefs[AI_CONTEXT_LENGTH] ?: 4096,
            streamResponses = prefs[STREAM_RESPONSES] ?: true,
            
            widgetTheme = WidgetTheme.valueOf(prefs[WIDGET_THEME] ?: WidgetTheme.SYSTEM.name),
            widgetTransparency = prefs[WIDGET_TRANSPARENCY] ?: 90,
            showWidgetQuickActions = prefs[WIDGET_QUICK_ACTIONS] ?: true,
            
            analyticsEnabled = prefs[ANALYTICS_ENABLED] ?: false,
            crashReportingEnabled = prefs[CRASH_REPORTING] ?: true,
            autoBackupEnabled = prefs[AUTO_BACKUP] ?: false,
            backupFrequency = BackupFrequency.valueOf(prefs[BACKUP_FREQUENCY] ?: BackupFrequency.WEEKLY.name)
        )
    }
    
    suspend fun updateSettings(settings: AppSettings) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[FONT_SCALE] = settings.fontScale.name
            prefs[HIGH_CONTRAST] = settings.highContrast
            prefs[REDUCE_MOTION] = settings.reduceMotion
            prefs[COMPACT_MODE] = settings.compactMode
            prefs[ANIMATIONS] = settings.animations
            
            prefs[WAKE_WORD_ENABLED] = settings.wakeWordEnabled
            prefs[WAKE_WORD_SENSITIVITY] = settings.wakeWordSensitivity.name
            prefs[VOICE_FEEDBACK] = settings.voiceFeedback
            prefs[READ_ALOUD] = settings.readAloudResponses
            
            prefs[CALENDAR_SYNC] = settings.calendarSyncEnabled
            prefs[DEFAULT_REMINDER_MINUTES] = settings.defaultReminderMinutes
            prefs[WEEK_STARTS_ON] = settings.weekStartsOn.name
            prefs[SHOW_WEEK_NUMBERS] = settings.showWeekNumbers
            
            prefs[DEFAULT_CURRENCY] = settings.defaultCurrency
            prefs[SMS_TRACKING] = settings.smsTrackingEnabled
            prefs[SHOW_CENTS] = settings.showCentsInAmounts
            prefs[BUDGET_ALERTS] = settings.budgetAlerts
            prefs[BUDGET_ALERT_THRESHOLD] = settings.budgetAlertThreshold
            
            prefs[LOCATION_ENABLED] = settings.locationEnabled
            prefs[GEOFENCING_ENABLED] = settings.geofencingEnabled
            prefs[LOCATION_ACCURACY] = settings.locationAccuracy.name
            prefs[GEOFENCE_RADIUS] = settings.geofenceRadius
            
            prefs[NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            prefs[REMINDER_NOTIFICATIONS] = settings.reminderNotifications
            prefs[FINANCE_NOTIFICATIONS] = settings.financeNotifications
            prefs[SOUND_ENABLED] = settings.soundEnabled
            prefs[VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[QUIET_HOURS_ENABLED] = settings.quietHoursEnabled
            prefs[QUIET_HOURS_START] = settings.quietHoursStart
            prefs[QUIET_HOURS_END] = settings.quietHoursEnd
            
            prefs[AI_TEMPERATURE] = settings.aiTemperature
            prefs[AI_MAX_TOKENS] = settings.aiMaxTokens
            prefs[AI_CONTEXT_LENGTH] = settings.aiContextLength
            prefs[STREAM_RESPONSES] = settings.streamResponses
            
            prefs[WIDGET_THEME] = settings.widgetTheme.name
            prefs[WIDGET_TRANSPARENCY] = settings.widgetTransparency
            prefs[WIDGET_QUICK_ACTIONS] = settings.showWidgetQuickActions
            
            prefs[ANALYTICS_ENABLED] = settings.analyticsEnabled
            prefs[CRASH_REPORTING] = settings.crashReportingEnabled
            prefs[AUTO_BACKUP] = settings.autoBackupEnabled
            prefs[BACKUP_FREQUENCY] = settings.backupFrequency.name
        }
    }
    
    suspend fun resetToDefaults() {
        context.appSettingsDataStore.edit { it.clear() }
    }
}
