package com.example.voicereminder

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Configuration
import com.example.voicereminder.domain.FinanceTrackingService
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VoiceReminderApplication : Application(), Configuration.Provider {
    
    companion object {
        private const val TAG = "VoiceReminderApp"
        private const val PREFS_NAME = "app_init_prefs"
        private const val KEY_PATTERNS_INITIALIZED = "patterns_initialized"
        private const val KEY_APP_VERSION = "app_version"
    }
    
    // Application-scoped coroutine scope with SupervisorJob for proper cancellation
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }
    
    override fun onCreate() {
        super.onCreate()

        // Initialize ThreeTenABP for time handling - this is lightweight and required
        AndroidThreeTen.init(this)
        
        // Only initialize default SMS sources on first install or app update
        // This prevents unnecessary database operations on every app launch
        initializeDefaultSmsSourcesIfNeeded()
        
        // Auto-start wake word service if enabled in settings
        startWakeWordServiceIfEnabled()
    }
    
    /**
     * Start the wake word service if it was previously enabled in settings
     */
    private fun startWakeWordServiceIfEnabled() {
        applicationScope.launch {
            try {
                val settingsRepo = com.example.voicereminder.data.settings.AppSettingsRepository.getInstance(this@VoiceReminderApplication)
                settingsRepo.appSettings.collect { settings ->
                    if (settings.wakeWordEnabled && !com.example.voicereminder.widget.WakeWordService.isServiceRunning()) {
                        Log.d(TAG, "Auto-starting wake word service (was enabled in settings)")
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            com.example.voicereminder.widget.WakeWordService.start(this@VoiceReminderApplication)
                        }
                    }
                    // Only need to check once at startup
                    return@collect
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking wake word settings", e)
            }
        }
    }
    
    /**
     * Initialize default SMS sources only once (on first install or app update)
     * This prevents repeated database writes on every app launch which was causing:
     * - Slow startup
     * - Unnecessary battery drain
     * - Database contention
     */
    private fun initializeDefaultSmsSourcesIfNeeded() {
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: Exception) {
            1
        }
        
        val savedVersion = prefs.getInt(KEY_APP_VERSION, 0)
        val sourcesInitialized = prefs.getBoolean(KEY_PATTERNS_INITIALIZED, false)
        
        // Only initialize if:
        // 1. Sources have never been initialized, OR
        // 2. App was updated to a new version
        if (!sourcesInitialized || savedVersion < currentVersion) {
            applicationScope.launch {
                try {
                    Log.d(TAG, "Initializing default SMS sources (first time or app update)")
                    val financeService = FinanceTrackingService(this@VoiceReminderApplication)
                    financeService.initializeDefaultSources()
                    
                    // Mark as initialized
                    prefs.edit()
                        .putBoolean(KEY_PATTERNS_INITIALIZED, true)
                        .putInt(KEY_APP_VERSION, currentVersion)
                        .apply()
                    
                    Log.d(TAG, "Default SMS sources initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing SMS sources", e)
                    // Don't mark as initialized so it retries next time
                }
            }
        } else {
            Log.d(TAG, "SMS sources already initialized, skipping")
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Cancel all coroutines when app terminates
        applicationScope.cancel()
    }
}
