package com.example.voicereminder.presentation.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.settings.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Enhanced ViewModel managing all app settings with auto-save
 */
class EnhancedSettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appSettingsRepo = AppSettingsRepository.getInstance(application)
    private val aiSettingsRepo = SettingsRepository(application)
    private val themeManager = ThemeManager.getInstance(application)
    private val backupManager = BackupManager.getInstance(application)
    
    // App Settings
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()
    
    // AI Settings
    private val _aiSettings = MutableStateFlow(AISettings())
    val aiSettings: StateFlow<AISettings> = _aiSettings.asStateFlow()
    
    // Theme
    val themeMode = themeManager.themeMode
    
    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // UI State
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    // Backup/Restore
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()
    
    // AI Connection Test
    private val _testResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val testResult: StateFlow<Pair<Boolean, String>?> = _testResult.asStateFlow()
    
    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()
    
    init {
        // Load settings
        viewModelScope.launch {
            appSettingsRepo.appSettings.collect { settings ->
                _appSettings.value = settings
            }
        }
        
        viewModelScope.launch {
            aiSettingsRepo.aiSettings.collect { settings ->
                _aiSettings.value = settings
            }
        }
    }
    
    // Search functionality
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun getFilteredSettings(): List<SettingItem> {
        val query = _searchQuery.value.lowercase()
        if (query.isBlank()) return emptyList()
        
        return allSettingItems.filter { item ->
            item.title.lowercase().contains(query) ||
            item.description.lowercase().contains(query) ||
            item.category.title.lowercase().contains(query)
        }
    }
    
    // Auto-save helper
    private fun autoSaveAppSettings(newSettings: AppSettings) {
        _appSettings.value = newSettings
        viewModelScope.launch {
            _isSaving.value = true
            appSettingsRepo.updateSettings(newSettings)
            _isSaving.value = false
        }
    }
    
    private fun autoSaveAISettings(newSettings: AISettings) {
        _aiSettings.value = newSettings
        viewModelScope.launch {
            _isSaving.value = true
            aiSettingsRepo.updateSettings(newSettings)
            _isSaving.value = false
        }
    }
    
    // Display & Accessibility
    fun updateFontScale(scale: FontScale) {
        autoSaveAppSettings(_appSettings.value.copy(fontScale = scale))
    }
    
    fun updateHighContrast(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(highContrast = enabled))
    }
    
    fun updateReduceMotion(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(reduceMotion = enabled))
    }
    
    fun updateCompactMode(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(compactMode = enabled))
    }
    
    fun updateAnimations(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(animations = enabled))
    }
    
    // Theme
    fun updateThemeMode(mode: ThemeMode) {
        themeManager.setThemeMode(mode)
    }
    
    // Voice & Wake Word
    fun updateWakeWordEnabled(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(wakeWordEnabled = enabled))
        
        // Actually start or stop the WakeWordService
        val context = getApplication<Application>()
        if (enabled) {
            com.example.voicereminder.widget.WakeWordService.start(context)
        } else {
            com.example.voicereminder.widget.WakeWordService.stop(context)
        }
    }
    
    fun updateWakeWordSensitivity(sensitivity: WakeWordSensitivity) {
        autoSaveAppSettings(_appSettings.value.copy(wakeWordSensitivity = sensitivity))
    }
    
    fun updateVoiceFeedback(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(voiceFeedback = enabled))
    }
    
    fun updateReadAloud(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(readAloudResponses = enabled))
    }
    
    // Calendar
    fun updateCalendarSync(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(calendarSyncEnabled = enabled))
    }
    
    fun updateDefaultReminderMinutes(minutes: Int) {
        autoSaveAppSettings(_appSettings.value.copy(defaultReminderMinutes = minutes))
    }
    
    fun updateWeekStart(weekStart: WeekStart) {
        autoSaveAppSettings(_appSettings.value.copy(weekStartsOn = weekStart))
    }
    
    fun updateShowWeekNumbers(show: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(showWeekNumbers = show))
    }
    
    // Finance
    fun updateDefaultCurrency(currency: String) {
        autoSaveAppSettings(_appSettings.value.copy(defaultCurrency = currency))
    }
    
    fun updateSmsTracking(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(smsTrackingEnabled = enabled))
    }
    
    fun updateShowCents(show: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(showCentsInAmounts = show))
    }
    
    fun updateBudgetAlerts(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(budgetAlerts = enabled))
    }
    
    fun updateBudgetAlertThreshold(threshold: Int) {
        autoSaveAppSettings(_appSettings.value.copy(budgetAlertThreshold = threshold))
    }
    
    // Location
    fun updateLocationEnabled(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(locationEnabled = enabled))
    }
    
    fun updateGeofencing(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(geofencingEnabled = enabled))
    }
    
    fun updateLocationAccuracy(accuracy: LocationAccuracy) {
        autoSaveAppSettings(_appSettings.value.copy(locationAccuracy = accuracy))
    }
    
    fun updateGeofenceRadius(radius: Int) {
        autoSaveAppSettings(_appSettings.value.copy(geofenceRadius = radius))
    }
    
    // Notifications
    fun updateNotificationsEnabled(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(notificationsEnabled = enabled))
    }
    
    fun updateReminderNotifications(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(reminderNotifications = enabled))
    }
    
    fun updateFinanceNotifications(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(financeNotifications = enabled))
    }
    
    fun updateSoundEnabled(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(soundEnabled = enabled))
    }
    
    fun updateVibrationEnabled(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(vibrationEnabled = enabled))
    }
    
    fun updateQuietHoursEnabled(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(quietHoursEnabled = enabled))
    }
    
    fun updateQuietHoursStart(time: String) {
        autoSaveAppSettings(_appSettings.value.copy(quietHoursStart = time))
    }
    
    fun updateQuietHoursEnd(time: String) {
        autoSaveAppSettings(_appSettings.value.copy(quietHoursEnd = time))
    }
    
    // AI Advanced
    fun updateAITemperature(temp: Float) {
        autoSaveAppSettings(_appSettings.value.copy(aiTemperature = temp))
    }
    
    fun updateAIMaxTokens(tokens: Int) {
        autoSaveAppSettings(_appSettings.value.copy(aiMaxTokens = tokens))
    }
    
    fun updateAIContextLength(length: Int) {
        autoSaveAppSettings(_appSettings.value.copy(aiContextLength = length))
    }
    
    fun updateStreamResponses(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(streamResponses = enabled))
    }
    
    // AI Provider Settings
    fun updateAIProvider(provider: AIProvider) {
        autoSaveAISettings(_aiSettings.value.copy(provider = provider))
    }
    
    fun updateOllamaConnectionMode(mode: OllamaConnectionMode) {
        autoSaveAISettings(_aiSettings.value.copy(ollamaConnectionMode = mode))
    }
    
    fun updateOllamaWifiIp(ip: String) {
        autoSaveAISettings(_aiSettings.value.copy(ollamaWifiIp = ip))
    }
    
    fun updateOllamaModel(model: String) {
        autoSaveAISettings(_aiSettings.value.copy(ollamaModel = model))
    }
    
    fun updateOpenAIApiKey(key: String) {
        autoSaveAISettings(_aiSettings.value.copy(openaiApiKey = key))
    }
    
    fun updateOpenAIModel(model: String) {
        autoSaveAISettings(_aiSettings.value.copy(openaiModel = model))
    }
    
    fun updateAnthropicApiKey(key: String) {
        autoSaveAISettings(_aiSettings.value.copy(anthropicApiKey = key))
    }
    
    fun updateAnthropicModel(model: String) {
        autoSaveAISettings(_aiSettings.value.copy(anthropicModel = model))
    }
    
    fun updateCustomBaseUrl(url: String) {
        autoSaveAISettings(_aiSettings.value.copy(customBaseUrl = url))
    }
    
    fun updateCustomApiKey(key: String) {
        autoSaveAISettings(_aiSettings.value.copy(customApiKey = key))
    }
    
    fun updateCustomModel(model: String) {
        autoSaveAISettings(_aiSettings.value.copy(customModel = model))
    }
    
    fun updateSystemPrompt(prompt: String) {
        autoSaveAISettings(_aiSettings.value.copy(systemPrompt = prompt))
    }
    
    // Widget
    fun updateWidgetTheme(theme: WidgetTheme) {
        autoSaveAppSettings(_appSettings.value.copy(widgetTheme = theme))
    }
    
    fun updateWidgetTransparency(transparency: Int) {
        autoSaveAppSettings(_appSettings.value.copy(widgetTransparency = transparency))
    }
    
    fun updateWidgetQuickActions(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(showWidgetQuickActions = enabled))
    }
    
    // Privacy
    fun updateAnalytics(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(analyticsEnabled = enabled))
    }
    
    fun updateCrashReporting(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(crashReportingEnabled = enabled))
    }
    
    fun updateAutoBackup(enabled: Boolean) {
        autoSaveAppSettings(_appSettings.value.copy(autoBackupEnabled = enabled))
    }
    
    fun updateBackupFrequency(frequency: BackupFrequency) {
        autoSaveAppSettings(_appSettings.value.copy(backupFrequency = frequency))
    }
    
    // Backup & Restore
    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            val result = backupManager.exportToUri(uri)
            _isExporting.value = false
            
            result.fold(
                onSuccess = { showSnackbar("Data exported successfully") },
                onFailure = { showSnackbar("Export failed: ${it.message}") }
            )
        }
    }
    
    fun importData(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = backupManager.importFromUri(uri)
            
            result.fold(
                onSuccess = { backup ->
                    backupManager.applyBackup(backup).fold(
                        onSuccess = { showSnackbar("Data restored successfully") },
                        onFailure = { showSnackbar("Restore failed: ${it.message}") }
                    )
                },
                onFailure = { showSnackbar("Import failed: ${it.message}") }
            )
            _isImporting.value = false
        }
    }
    
    // Test AI Connection
    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            
            try {
                val aiService = com.example.voicereminder.data.ai.AIServiceFactory.create(_aiSettings.value.provider)
                val result = aiService.testConnection(_aiSettings.value)
                _testResult.value = result
            } catch (e: Exception) {
                _testResult.value = Pair(false, "Error: ${e.message}")
            } finally {
                _isTesting.value = false
            }
        }
    }
    
    // Reset
    fun resetAllSettings() {
        viewModelScope.launch {
            appSettingsRepo.resetToDefaults()
            showSnackbar("Settings reset to defaults")
        }
    }
    
    // Clear Chat History
    private val _isClearingChat = MutableStateFlow(false)
    val isClearingChat: StateFlow<Boolean> = _isClearingChat.asStateFlow()
    
    fun clearChatHistory() {
        viewModelScope.launch {
            _isClearingChat.value = true
            try {
                val chatHistoryRepo = com.example.voicereminder.data.ChatHistoryRepository.getInstance(getApplication())
                chatHistoryRepo.clearHistory()
                showSnackbar("Chat history cleared")
            } catch (e: Exception) {
                showSnackbar("Failed to clear chat history: ${e.message}")
            } finally {
                _isClearingChat.value = false
            }
        }
    }
    
    fun clearTestResult() {
        _testResult.value = null
    }
    
    private fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }
    
    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
    
    // Setting items for search
    private val allSettingItems = listOf(
        SettingItem("Theme", "Light, Dark, or System theme", SettingsCategory.GENERAL),
        SettingItem("Font Size", "Adjust text size for readability", SettingsCategory.GENERAL),
        SettingItem("High Contrast", "Increase contrast for visibility", SettingsCategory.GENERAL),
        SettingItem("Animations", "Enable or disable UI animations", SettingsCategory.GENERAL),
        SettingItem("Compact Mode", "Show more content on screen", SettingsCategory.GENERAL),
        
        SettingItem("AI Provider", "Ollama, OpenAI, Anthropic, or Custom", SettingsCategory.AI),
        SettingItem("Temperature", "AI response creativity level", SettingsCategory.AI),
        SettingItem("Max Tokens", "Maximum response length", SettingsCategory.AI),
        SettingItem("System Prompt", "Customize AI behavior", SettingsCategory.AI),
        
        SettingItem("Calendar Sync", "Sync with device calendar", SettingsCategory.CALENDAR),
        SettingItem("Default Reminder", "Default reminder time before events", SettingsCategory.CALENDAR),
        SettingItem("Week Start", "First day of the week", SettingsCategory.CALENDAR),
        
        SettingItem("Currency", "Default currency for transactions", SettingsCategory.FINANCE),
        SettingItem("SMS Tracking", "Auto-detect transactions from SMS", SettingsCategory.FINANCE),
        SettingItem("Budget Alerts", "Get notified when approaching budget", SettingsCategory.FINANCE),
        
        SettingItem("Location Access", "Enable location-based features", SettingsCategory.LOCATION),
        SettingItem("Geofencing", "Trigger reminders at locations", SettingsCategory.LOCATION),
        SettingItem("Geofence Radius", "Detection radius for locations", SettingsCategory.LOCATION),
        
        SettingItem("Notifications", "Enable or disable all notifications", SettingsCategory.NOTIFICATIONS),
        SettingItem("Sound", "Play notification sounds", SettingsCategory.NOTIFICATIONS),
        SettingItem("Vibration", "Vibrate on notifications", SettingsCategory.NOTIFICATIONS),
        SettingItem("Quiet Hours", "Silence notifications during set hours", SettingsCategory.NOTIFICATIONS),
        
        SettingItem("Export Data", "Download your data as backup", SettingsCategory.PRIVACY),
        SettingItem("Import Data", "Restore from backup file", SettingsCategory.PRIVACY),
        SettingItem("Clear Data", "Delete all app data", SettingsCategory.PRIVACY),
        SettingItem("Analytics", "Share anonymous usage data", SettingsCategory.PRIVACY)
    )
}

data class SettingItem(
    val title: String,
    val description: String,
    val category: SettingsCategory
)
