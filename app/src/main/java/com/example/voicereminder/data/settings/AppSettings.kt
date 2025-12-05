package com.example.voicereminder.data.settings

/**
 * Comprehensive app settings data class with all configurable options
 */
data class AppSettings(
    // Display & Accessibility
    val fontScale: FontScale = FontScale.NORMAL,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val compactMode: Boolean = false,
    val animations: Boolean = true,
    
    // Voice & Wake Word
    val wakeWordEnabled: Boolean = false,
    val wakeWordSensitivity: WakeWordSensitivity = WakeWordSensitivity.MEDIUM,
    val voiceFeedback: Boolean = true,
    val readAloudResponses: Boolean = false,
    
    // Calendar
    val calendarSyncEnabled: Boolean = true,
    val defaultReminderMinutes: Int = 15,
    val weekStartsOn: WeekStart = WeekStart.SUNDAY,
    val showWeekNumbers: Boolean = false,
    
    // Finance
    val defaultCurrency: String = "USD",
    val smsTrackingEnabled: Boolean = true,
    val showCentsInAmounts: Boolean = true,
    val budgetAlerts: Boolean = true,
    val budgetAlertThreshold: Int = 80, // percentage
    
    // Location
    val locationEnabled: Boolean = true,
    val geofencingEnabled: Boolean = true,
    val locationAccuracy: LocationAccuracy = LocationAccuracy.BALANCED,
    val geofenceRadius: Int = 100, // meters
    
    // Notifications
    val notificationsEnabled: Boolean = true,
    val reminderNotifications: Boolean = true,
    val financeNotifications: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    
    // AI Advanced
    val aiTemperature: Float = 0.7f,
    val aiMaxTokens: Int = 1024,
    val aiContextLength: Int = 4096,
    val streamResponses: Boolean = true,
    
    // Widget
    val widgetTheme: WidgetTheme = WidgetTheme.SYSTEM,
    val widgetTransparency: Int = 90, // percentage
    val showWidgetQuickActions: Boolean = true,
    
    // Data & Privacy
    val analyticsEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = false,
    val backupFrequency: BackupFrequency = BackupFrequency.WEEKLY
)

enum class FontScale(val scale: Float, val displayName: String) {
    SMALL(0.85f, "Small"),
    NORMAL(1.0f, "Normal"),
    LARGE(1.15f, "Large"),
    EXTRA_LARGE(1.3f, "Extra Large")
}

enum class WakeWordSensitivity(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class WeekStart(val displayName: String) {
    SUNDAY("Sunday"),
    MONDAY("Monday"),
    SATURDAY("Saturday")
}

enum class LocationAccuracy(val displayName: String) {
    HIGH("High (GPS)"),
    BALANCED("Balanced"),
    LOW("Low (Battery Saver)")
}

enum class WidgetTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

enum class BackupFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}
