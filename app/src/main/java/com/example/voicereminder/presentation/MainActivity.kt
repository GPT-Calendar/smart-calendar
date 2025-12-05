package com.example.voicereminder.presentation

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import com.example.voicereminder.R
import com.example.voicereminder.presentation.ui.theme.SmartCalendarTheme
import com.example.voicereminder.presentation.home.HomeScreen
import com.example.voicereminder.presentation.chat.ChatScreen
import com.example.voicereminder.presentation.chat.GlobalBottomNavigation
import com.example.voicereminder.presentation.calendar.CalendarScreen
import com.example.voicereminder.presentation.calendar.AllEventsScreen
import com.example.voicereminder.presentation.calendar.ReminderFilter
import com.example.voicereminder.presentation.viewmodel.CalendarViewModel
import com.example.voicereminder.presentation.viewmodel.CalendarViewModelFactory
import com.example.voicereminder.presentation.map.MapScreen
import com.example.voicereminder.presentation.finance.FinanceScreen
import com.example.voicereminder.presentation.settings.SettingsScreen
import com.example.voicereminder.presentation.settings.SettingsHubScreen
import com.example.voicereminder.presentation.settings.EnhancedSettingsHubScreen
import com.example.voicereminder.presentation.components.GlobalTopBar
import com.example.voicereminder.data.settings.ThemeManager
import com.example.voicereminder.data.settings.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme

// Data class for bottom navigation items
data class BottomNavigationItem(
    val route: String,
    val icon: @Composable () -> Unit,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {
    
    private var permissionsChecked = false

    // Permission launcher for POST_NOTIFICATIONS permission (Android 13+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Permission denied - show user-friendly message
            showPermissionDeniedDialog(
                "Notification Permission Required",
                "Smart Calendar needs notification permission to alert you when reminders are due. " +
                        "Without this permission, you won't receive reminder notifications.",
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        // Continue with other permission checks
        checkExactAlarmPermission()
    }

    // Permission launcher for location permissions
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Fine location granted, check if we need background location (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                checkBackgroundLocationPermission()
            }
        } else {
            // Permission denied - show user-friendly message
            showPermissionDeniedDialog(
                "Location Permission Required",
                "Smart Calendar needs location permission to create location-based reminders. " +
                        "Without this permission, you won't be able to set reminders for specific places.",
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // Permission launcher for background location permission (Android 10+)
    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Background location denied - show explanation
            showPermissionDeniedDialog(
                "Background Location Permission",
                "Background location access allows the app to detect when you arrive at reminder locations " +
                        "even when the app is not in use. Without this permission, location reminders may not " +
                        "trigger reliably.",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                } else {
                    null
                }
            )
        }
    }

    // Activity result launcher for exact alarm settings (Android 12+)
    private val exactAlarmSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if permission was granted after returning from settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showPermissionDeniedDialog(
                    "Exact Alarm Permission Required",
                    "Smart Calendar needs exact alarm permission to trigger reminders at precise times. " +
                            "Without this permission, reminders may not trigger at the exact scheduled time.",
                    null
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Theme management
            val themeManager = remember { ThemeManager.getInstance(applicationContext) }
            val themeMode by themeManager.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }
            
            SmartCalendarTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // State to track if splash screen is complete
                    var showSplash by remember { mutableStateOf(true) }
                    
                    if (showSplash) {
                        // Show splash screen first
                        com.example.voicereminder.presentation.splash.SplashScreen(
                            onSplashComplete = { showSplash = false }
                        )
                    } else {
                        // Show main content after splash
                        MainScreen(themeManager = themeManager)
                    }
                }
            }
        }
    }
    
    // Track if permission check has been done to avoid repeated checks
    private var permissionCheckDone = false
    
    override fun onResume() {
        super.onResume()
        
        // Check for location permission changes only once per app session
        // or when explicitly needed (not on every resume)
        // This prevents repeated heavy operations that drain battery
        if (!permissionCheckDone) {
            checkLocationPermissionChanges()
            permissionCheckDone = true
        }
    }
    
    /**
     * Check if location permissions have changed and handle accordingly
     * Now runs with lower priority and only when needed
     */
    private fun checkLocationPermissionChanges() {
        // Use Dispatchers.IO with lower priority to avoid blocking UI
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Delay slightly to let UI render first
                kotlinx.coroutines.delay(500)
                
                val database = com.example.voicereminder.data.ReminderDatabase.getDatabase(applicationContext)
                val locationServiceManager = com.example.voicereminder.domain.LocationServiceManager(applicationContext)
                val geofenceManager = com.example.voicereminder.domain.GeofenceManager(applicationContext)

                val permissionMonitor = com.example.voicereminder.domain.LocationPermissionMonitor(
                    applicationContext,
                    database,
                    locationServiceManager,
                    geofenceManager
                )

                // Initialize and check for permission changes
                permissionMonitor.initialize()

            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error checking location permission changes", e)
            }
        }
    }
    
    /**
     * Force recheck permissions - call this when user returns from settings
     */
    fun forceRecheckPermissions() {
        permissionCheckDone = false
        checkLocationPermissionChanges()
        permissionCheckDone = true
    }

    /**
     * Navigate to the voice input screen
     */
    fun navigateToVoiceInput(navController: NavController) {
        navController.navigate("assistant") {
            // Navigate to assistant screen
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    @Composable
    fun MainScreen(themeManager: ThemeManager? = null) {
        val navController = rememberNavController()
        val context = LocalContext.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "assistant"
        
        // Check permissions on first resume
        LaunchedEffect(Unit) {
            if (!permissionsChecked) {
                permissionsChecked = true
                checkAndRequestPermissions()
            }
        }
        
        // Determine if we should show the global top bar (hide on settings screens)
        val showGlobalTopBar = currentRoute !in listOf("settings", "settings_hub", "ai_settings")
        val showBottomNav = currentRoute !in listOf("settings", "settings_hub", "ai_settings")

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (showGlobalTopBar) {
                    val reminderManager = remember { com.example.voicereminder.domain.ReminderManager.getInstance(context) }
                    val coroutineScope = rememberCoroutineScope()
                    
                    GlobalTopBar(
                        title = "Smart Calendar",
                        subtitle = getScreenSubtitle(currentRoute),
                        onMenuClick = { navController.navigate("settings_hub") },
                        onProfileClick = { /* Navigate to profile */ },
                        onNotificationsClick = { 
                            // Navigate to calendar to see all reminders
                            navController.navigate("calendar") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onReminderClick = { reminder ->
                            // Navigate to calendar when reminder is clicked
                            navController.navigate("calendar") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onReminderDelete = { reminder ->
                            coroutineScope.launch {
                                reminderManager.deleteReminder(reminder.id)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBottomNav) {
                    BottomNavigationBar(
                        navController = navController,
                        navigateToVoiceInput = { navigateToVoiceInput(navController) }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "assistant",
                modifier = Modifier.padding(padding)
            ) {
                // Unified Planner Screen - combines Calendar, Tasks, Reminders, Alarms
                composable("planner") {
                    com.example.voicereminder.presentation.planner.PlannerScreen(
                        onNavigateToTaskDetail = { taskId ->
                            if (taskId != null) {
                                navController.navigate("task_detail/$taskId")
                            } else {
                                navController.navigate("task_detail")
                            }
                        },
                        onNavigateToAlarmDetail = { alarmId ->
                            if (alarmId != null) {
                                navController.navigate("alarm_detail/$alarmId")
                            } else {
                                navController.navigate("alarm_detail")
                            }
                        },
                        onNavigateToReminderDetail = { reminderId ->
                            // Navigate to reminder detail if needed
                        }
                    )
                }
                // Calendar route - unified calendar with events and transactions
                composable("calendar") {
                    CalendarScreen(
                        onReminderClick = { },
                        onReminderDelete = { }
                    )
                }
                composable("task_detail") {
                    val taskViewModel: com.example.voicereminder.presentation.tasks.TaskViewModel = 
                        androidx.lifecycle.viewmodel.compose.viewModel()
                    
                    com.example.voicereminder.presentation.tasks.TaskDetailScreen(
                        task = null,
                        onSave = { title, description, dueDate, priority, category, recurrence ->
                            taskViewModel.createTask(title, description, dueDate, priority, category, recurrence)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("alarm_detail") {
                    val alarmViewModel: com.example.voicereminder.presentation.alarms.AlarmViewModel = 
                        androidx.lifecycle.viewmodel.compose.viewModel()
                    
                    com.example.voicereminder.presentation.alarms.AlarmDetailScreen(
                        alarm = null,
                        onSave = { hour, minute, label, repeatDays, vibrate ->
                            alarmViewModel.createAlarm(hour, minute, label, repeatDays, vibrate)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("finance") { 
                    FinanceScreen() 
                }
                composable("all_events") {
                    val context = LocalContext.current
                    val calendarViewModel: CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = CalendarViewModelFactory(
                            reminderManager = com.example.voicereminder.domain.ReminderManager.getInstance(context)
                        )
                    )
                    LaunchedEffect(Unit) {
                        calendarViewModel.onScreenVisible()
                    }
                    val reminders by calendarViewModel.allReminders.collectAsState()
                    AllEventsScreen(
                        reminders = reminders,
                        onReminderClick = { },
                        onReminderDelete = { reminder -> calendarViewModel.deleteReminder(reminder.id) }
                    )
                }
                composable("map") { 
                    MapScreen(onLocationPermissionNeeded = { checkAndRequestLocationPermission() }) 
                }
                composable("assistant") { 
                    ChatScreen(
                        onLocationPermissionNeeded = { checkAndRequestLocationPermission() },
                        onNavigateToSettings = { navController.navigate("settings_hub") }
                    ) 
                }
                // Enhanced Settings Hub - main settings entry point
                composable("settings_hub") {
                    EnhancedSettingsHubScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                // Legacy AI settings (for backward compatibility)
                composable("settings") {
                    SettingsScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
    
    /**
     * Get subtitle text for current screen
     */
    private fun getScreenSubtitle(route: String): String? {
        return when (route) {
            "assistant" -> "AI Assistant"
            "planner" -> "Planner"
            "calendar" -> "Planner"
            "finance" -> "Finance Tracker"
            "map" -> "Locations"
            "task_detail" -> "New Task"
            "alarm_detail" -> "New Alarm"
            else -> null
        }
    }

    @Composable
    fun BottomNavigationBar(
        navController: NavController,
        navigateToVoiceInput: () -> Unit
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "assistant"

        GlobalBottomNavigation(
            currentScreen = currentRoute,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    /**
     * Check and request all necessary permissions on app start
     */
    private fun checkAndRequestPermissions() {
        // Check notification permission first (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        } else {
            // For older versions, skip to exact alarm check
            checkExactAlarmPermission()
        }
    }

    /**
     * Check and request notification permission (Android 13+)
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, continue to next check
                    checkExactAlarmPermission()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale dialog before requesting
                    showPermissionRationaleDialog(
                        "Notification Permission",
                        getString(R.string.permission_rationale_notification)
                    ) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                else -> {
                    // Request permission directly
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    /**
     * Check and request exact alarm permission (Android 12+)
     */
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Show rationale and guide user to settings
                showPermissionRationaleDialog(
                    "Exact Alarm Permission",
                    getString(R.string.permission_rationale_alarm)
                ) {
                    // Open exact alarm settings
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    exactAlarmSettingsLauncher.launch(intent)
                }
            }
        }
    }

    /**
     * Check and request location permission for location-based reminders
     * This should be called when user attempts to create a location-based reminder
     */
    fun checkAndRequestLocationPermission(onGranted: () -> Unit = {}) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                onGranted()
                // Check background location if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    checkBackgroundLocationPermission()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show rationale dialog before requesting
                showPermissionRationaleDialog(
                    "Location Permission",
                    "Location access is needed to create reminders that trigger when you arrive at specific places. " +
                            "This allows you to set reminders like \"remind me to buy milk when I reach the store\"."
                ) {
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else -> {
                // Request permission directly
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check and request background location permission (Android 10+)
     * Should be called after foreground location permission is granted
     */
    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> {
                    // Show rationale dialog before requesting
                    showPermissionRationaleDialog(
                        "Background Location Permission",
                        "Background location access allows the app to detect when you arrive at reminder locations " +
                                "even when the app is closed or not in use. This ensures your location reminders " +
                                "trigger reliably.\n\nOn the next screen, please select \"Allow all the time\"."
                    ) {
                        requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }
                else -> {
                    // Request permission directly
                    requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }
    }

    /**
     * Show permission rationale dialog before requesting permission
     */
    private fun showPermissionRationaleDialog(title: String, message: String, onPositive: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") { dialog, _ ->
                dialog.dismiss()
                onPositive()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Show permission denied dialog with option to open settings
     */
    private fun showPermissionDeniedDialog(title: String, message: String, permission: String?) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
}