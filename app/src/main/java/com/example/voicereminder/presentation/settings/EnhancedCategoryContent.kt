package com.example.voicereminder.presentation.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.voicereminder.data.settings.*
import com.example.voicereminder.presentation.settings.components.*

/**
 * Enhanced content for each settings category
 */
@Composable
fun EnhancedCategoryContent(
    category: SettingsCategory,
    modifier: Modifier = Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings,
    aiSettings: AISettings,
    themeMode: ThemeMode
) {
    when (category) {
        SettingsCategory.GENERAL -> GeneralContent(modifier, viewModel, appSettings, themeMode)
        SettingsCategory.AI -> AIContent(modifier, viewModel, appSettings, aiSettings)
        SettingsCategory.CALENDAR -> CalendarContent(modifier, viewModel, appSettings)
        SettingsCategory.FINANCE -> FinanceContent(modifier, viewModel, appSettings)
        SettingsCategory.LOCATION -> LocationContent(modifier, viewModel, appSettings)
        SettingsCategory.NOTIFICATIONS -> NotificationContent(modifier, viewModel, appSettings)
        SettingsCategory.WIDGET -> WidgetContent(modifier, viewModel, appSettings)
        SettingsCategory.PRIVACY -> PrivacyContent(modifier, viewModel, appSettings)
        SettingsCategory.ABOUT -> AboutContent(modifier)
    }
}


@Composable
fun GeneralContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings,
    themeMode: ThemeMode
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                SettingsSegmentedButton(
                    title = "Theme",
                    options = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM),
                    selectedOption = themeMode,
                    onOptionSelected = viewModel::updateThemeMode,
                    optionLabel = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    icon = Icons.Default.DarkMode
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingsSegmentedButton(
                    title = "Font Size",
                    options = FontScale.values().toList(),
                    selectedOption = appSettings.fontScale,
                    onOptionSelected = viewModel::updateFontScale,
                    optionLabel = { it.displayName },
                    icon = Icons.Default.TextFields
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Accessibility", icon = Icons.Default.Accessibility) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.Contrast,
                    title = "High Contrast",
                    subtitle = "Increase contrast for better visibility",
                    checked = appSettings.highContrast,
                    onCheckedChange = viewModel::updateHighContrast
                )
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.Animation,
                    title = "Reduce Motion",
                    subtitle = "Minimize animations",
                    checked = appSettings.reduceMotion,
                    onCheckedChange = viewModel::updateReduceMotion
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Display", icon = Icons.Default.Smartphone) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.ViewCompact,
                    title = "Compact Mode",
                    subtitle = "Show more content on screen",
                    checked = appSettings.compactMode,
                    onCheckedChange = viewModel::updateCompactMode
                )
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.Animation,
                    title = "Animations",
                    subtitle = "Enable UI animations",
                    checked = appSettings.animations,
                    onCheckedChange = viewModel::updateAnimations
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Voice", icon = Icons.Default.Mic) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "\"Hey Kiro\" Wake Word",
                    subtitle = "Activate assistant with voice",
                    checked = appSettings.wakeWordEnabled,
                    onCheckedChange = viewModel::updateWakeWordEnabled
                )
                
                if (appSettings.wakeWordEnabled) {
                    HorizontalDivider()
                    
                    SettingsSegmentedButton(
                        title = "Wake Word Sensitivity",
                        options = WakeWordSensitivity.values().toList(),
                        selectedOption = appSettings.wakeWordSensitivity,
                        onOptionSelected = viewModel::updateWakeWordSensitivity,
                        optionLabel = { it.displayName }
                    )
                }
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.VolumeUp,
                    title = "Voice Feedback",
                    subtitle = "Audio confirmation for actions",
                    checked = appSettings.voiceFeedback,
                    onCheckedChange = viewModel::updateVoiceFeedback
                )
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.Speaker,
                    title = "Read Aloud Responses",
                    subtitle = "Speak AI responses",
                    checked = appSettings.readAloudResponses,
                    onCheckedChange = viewModel::updateReadAloud
                )
            }
        }
    }
}


@Composable
fun AIContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings,
    aiSettings: AISettings
) {
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "AI Provider", icon = Icons.Default.SmartToy) {
                AIProvider.values().forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = aiSettings.provider == provider,
                            onClick = { viewModel.updateAIProvider(provider) }
                        )
                        Text(
                            text = provider.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        
        item {
            EnhancedSettingsSection(
                title = "Provider Configuration",
                icon = Icons.Default.Settings,
                collapsible = false
            ) {
                when (aiSettings.provider) {
                    AIProvider.OLLAMA -> OllamaConfig(aiSettings, viewModel)
                    AIProvider.OPENAI -> OpenAIConfig(aiSettings, viewModel)
                    AIProvider.ANTHROPIC -> AnthropicConfig(aiSettings, viewModel)
                    AIProvider.CUSTOM -> CustomConfig(aiSettings, viewModel)
                }
            }
        }
        
        item {
            Button(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isTesting) "Testing..." else "Test Connection")
            }
            
            testResult?.let { (success, message) ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (success) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message)
                    }
                }
            }
        }
        
        item {
            EnhancedSettingsSection(
                title = "Advanced AI Settings",
                icon = Icons.Default.Tune,
                collapsible = true,
                initiallyExpanded = false
            ) {
                SettingsSlider(
                    title = "Temperature",
                    value = appSettings.aiTemperature,
                    onValueChange = viewModel::updateAITemperature,
                    valueRange = 0f..2f,
                    valueLabel = String.format("%.1f", appSettings.aiTemperature),
                    icon = Icons.Default.Thermostat
                )
                
                SettingsInfoCard(
                    title = "Temperature",
                    content = "Lower = more focused, Higher = more creative"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsNumberField(
                    title = "Max Tokens",
                    value = appSettings.aiMaxTokens,
                    onValueChange = viewModel::updateAIMaxTokens,
                    suffix = "tokens",
                    icon = Icons.Default.DataUsage,
                    range = 256..8192
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsNumberField(
                    title = "Context Length",
                    value = appSettings.aiContextLength,
                    onValueChange = viewModel::updateAIContextLength,
                    suffix = "tokens",
                    icon = Icons.Default.Memory,
                    range = 1024..32768
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.Stream,
                    title = "Stream Responses",
                    subtitle = "Show responses as they generate",
                    checked = appSettings.streamResponses,
                    onCheckedChange = viewModel::updateStreamResponses
                )
            }
        }
    }
}

@Composable
fun OllamaConfig(aiSettings: AISettings, viewModel: EnhancedSettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSegmentedButton(
            title = "Connection Mode",
            options = OllamaConnectionMode.values().toList(),
            selectedOption = aiSettings.ollamaConnectionMode,
            onOptionSelected = viewModel::updateOllamaConnectionMode,
            optionLabel = { if (it == OllamaConnectionMode.WIFI) "WiFi" else "ADB USB" }
        )
        
        if (aiSettings.ollamaConnectionMode == OllamaConnectionMode.WIFI) {
            OutlinedTextField(
                value = aiSettings.ollamaWifiIp,
                onValueChange = viewModel::updateOllamaWifiIp,
                label = { Text("PC IP Address") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("192.168.1.100") }
            )
        }
        
        OutlinedTextField(
            value = aiSettings.ollamaModel,
            onValueChange = viewModel::updateOllamaModel,
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("llama3.1:latest") }
        )
        
        SettingsInfoCard(
            title = if (aiSettings.ollamaConnectionMode == OllamaConnectionMode.WIFI) "WiFi Mode" else "ADB Mode",
            content = if (aiSettings.ollamaConnectionMode == OllamaConnectionMode.WIFI)
                "Connect PC and device to same WiFi. URL: http://${aiSettings.ollamaWifiIp}:8080/"
            else
                "Run: adb reverse tcp:8080 tcp:8080. URL: http://localhost:8080/"
        )
    }
}

@Composable
fun OpenAIConfig(aiSettings: AISettings, viewModel: EnhancedSettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsPasswordField(
            title = "API Key",
            value = aiSettings.openaiApiKey,
            onValueChange = viewModel::updateOpenAIApiKey,
            placeholder = "sk-...",
            icon = Icons.Default.Key
        )
        
        OutlinedTextField(
            value = aiSettings.openaiModel,
            onValueChange = viewModel::updateOpenAIModel,
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("gpt-4") }
        )
    }
}

@Composable
fun AnthropicConfig(aiSettings: AISettings, viewModel: EnhancedSettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsPasswordField(
            title = "API Key",
            value = aiSettings.anthropicApiKey,
            onValueChange = viewModel::updateAnthropicApiKey,
            placeholder = "sk-ant-...",
            icon = Icons.Default.Key
        )
        
        OutlinedTextField(
            value = aiSettings.anthropicModel,
            onValueChange = viewModel::updateAnthropicModel,
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("claude-3-5-sonnet-20241022") }
        )
    }
}

@Composable
fun CustomConfig(aiSettings: AISettings, viewModel: EnhancedSettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = aiSettings.customBaseUrl,
            onValueChange = viewModel::updateCustomBaseUrl,
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://api.example.com") }
        )
        
        SettingsPasswordField(
            title = "API Key (Optional)",
            value = aiSettings.customApiKey,
            onValueChange = viewModel::updateCustomApiKey,
            icon = Icons.Default.Key
        )
        
        OutlinedTextField(
            value = aiSettings.customModel,
            onValueChange = viewModel::updateCustomModel,
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth()
        )
        
        SettingsInfoCard(
            title = "Custom API",
            content = "Must be OpenAI-compatible (LM Studio, LocalAI, etc.)"
        )
    }
}


@Composable
fun CalendarContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "Sync", icon = Icons.Default.Sync) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.CalendarToday,
                    title = "Calendar Sync",
                    subtitle = "Sync with device calendar",
                    checked = appSettings.calendarSyncEnabled,
                    onCheckedChange = viewModel::updateCalendarSync
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Reminders", icon = Icons.Default.Alarm) {
                val reminderOptions = listOf(5, 10, 15, 30, 60)
                SettingsSegmentedButton(
                    title = "Default Reminder",
                    options = reminderOptions,
                    selectedOption = appSettings.defaultReminderMinutes,
                    onOptionSelected = viewModel::updateDefaultReminderMinutes,
                    optionLabel = { if (it < 60) "${it}m" else "1h" }
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Display", icon = Icons.Default.ViewWeek) {
                SettingsSegmentedButton(
                    title = "Week Starts On",
                    options = WeekStart.values().toList(),
                    selectedOption = appSettings.weekStartsOn,
                    onOptionSelected = viewModel::updateWeekStart,
                    optionLabel = { it.displayName.take(3) }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.Numbers,
                    title = "Show Week Numbers",
                    subtitle = "Display week numbers in calendar",
                    checked = appSettings.showWeekNumbers,
                    onCheckedChange = viewModel::updateShowWeekNumbers
                )
            }
        }
    }
}

@Composable
fun FinanceContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "Currency", icon = Icons.Default.AttachMoney) {
                val currencies = listOf("USD", "EUR", "GBP", "JPY", "INR", "SAR", "ETB")
                var expanded by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Default Currency", modifier = Modifier.weight(1f))
                    
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(appSettings.defaultCurrency)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        viewModel.updateDefaultCurrency(currency)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.MoneyOff,
                    title = "Show Cents",
                    subtitle = "Display decimal places in amounts",
                    checked = appSettings.showCentsInAmounts,
                    onCheckedChange = viewModel::updateShowCents
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Tracking", icon = Icons.Default.TrackChanges) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.Sms,
                    title = "SMS Transaction Tracking",
                    subtitle = "Auto-detect transactions from bank SMS",
                    checked = appSettings.smsTrackingEnabled,
                    onCheckedChange = viewModel::updateSmsTracking
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Budget Alerts", icon = Icons.Default.Warning) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.NotificationsActive,
                    title = "Budget Alerts",
                    subtitle = "Get notified when approaching budget limit",
                    checked = appSettings.budgetAlerts,
                    onCheckedChange = viewModel::updateBudgetAlerts
                )
                
                if (appSettings.budgetAlerts) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsSlider(
                        title = "Alert Threshold",
                        value = appSettings.budgetAlertThreshold.toFloat(),
                        onValueChange = { viewModel.updateBudgetAlertThreshold(it.toInt()) },
                        valueRange = 50f..95f,
                        steps = 8,
                        valueLabel = "${appSettings.budgetAlertThreshold}%",
                        icon = Icons.Default.Percent
                    )
                }
            }
        }
    }
}

@Composable
fun LocationContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "Location Services", icon = Icons.Default.LocationOn) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.MyLocation,
                    title = "Location Access",
                    subtitle = "Enable location-based features",
                    checked = appSettings.locationEnabled,
                    onCheckedChange = viewModel::updateLocationEnabled
                )
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.ShareLocation,
                    title = "Geofencing",
                    subtitle = "Trigger reminders at locations",
                    checked = appSettings.geofencingEnabled,
                    onCheckedChange = viewModel::updateGeofencing,
                    enabled = appSettings.locationEnabled
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Accuracy", icon = Icons.Default.GpsFixed) {
                SettingsSegmentedButton(
                    title = "Location Accuracy",
                    options = LocationAccuracy.values().toList(),
                    selectedOption = appSettings.locationAccuracy,
                    onOptionSelected = viewModel::updateLocationAccuracy,
                    optionLabel = { 
                        when (it) {
                            LocationAccuracy.HIGH -> "High"
                            LocationAccuracy.BALANCED -> "Balanced"
                            LocationAccuracy.LOW -> "Low"
                        }
                    }
                )
                
                SettingsInfoCard(
                    title = "Battery Impact",
                    content = "High accuracy uses more battery. Balanced is recommended."
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Geofence Settings", icon = Icons.Default.Radar) {
                SettingsSlider(
                    title = "Geofence Radius",
                    value = appSettings.geofenceRadius.toFloat(),
                    onValueChange = { viewModel.updateGeofenceRadius(it.toInt()) },
                    valueRange = 50f..500f,
                    steps = 8,
                    valueLabel = "${appSettings.geofenceRadius}m",
                    icon = Icons.Default.RadioButtonChecked
                )
            }
        }
    }
}


@Composable
fun NotificationContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "General", icon = Icons.Default.Notifications) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.NotificationsActive,
                    title = "All Notifications",
                    subtitle = "Master toggle for all notifications",
                    checked = appSettings.notificationsEnabled,
                    onCheckedChange = viewModel::updateNotificationsEnabled
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Notification Types", icon = Icons.Default.Category) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.Alarm,
                    title = "Reminder Notifications",
                    subtitle = "Alerts for reminders and alarms",
                    checked = appSettings.reminderNotifications,
                    onCheckedChange = viewModel::updateReminderNotifications,
                    enabled = appSettings.notificationsEnabled
                )
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.AccountBalance,
                    title = "Finance Notifications",
                    subtitle = "Budget alerts and transaction updates",
                    checked = appSettings.financeNotifications,
                    onCheckedChange = viewModel::updateFinanceNotifications,
                    enabled = appSettings.notificationsEnabled
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Alert Style", icon = Icons.Default.VolumeUp) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.MusicNote,
                    title = "Sound",
                    subtitle = "Play notification sound",
                    checked = appSettings.soundEnabled,
                    onCheckedChange = viewModel::updateSoundEnabled,
                    enabled = appSettings.notificationsEnabled
                )
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.Vibration,
                    title = "Vibration",
                    subtitle = "Vibrate on notifications",
                    checked = appSettings.vibrationEnabled,
                    onCheckedChange = viewModel::updateVibrationEnabled,
                    enabled = appSettings.notificationsEnabled
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Quiet Hours", icon = Icons.Default.DoNotDisturb) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.Bedtime,
                    title = "Quiet Hours",
                    subtitle = "Silence notifications during set hours",
                    checked = appSettings.quietHoursEnabled,
                    onCheckedChange = viewModel::updateQuietHoursEnabled,
                    enabled = appSettings.notificationsEnabled
                )
                
                if (appSettings.quietHoursEnabled && appSettings.notificationsEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsTimePicker(
                        title = "Start Time",
                        time = appSettings.quietHoursStart,
                        onTimeChange = viewModel::updateQuietHoursStart,
                        icon = Icons.Default.NightsStay
                    )
                    
                    SettingsTimePicker(
                        title = "End Time",
                        time = appSettings.quietHoursEnd,
                        onTimeChange = viewModel::updateQuietHoursEnd,
                        icon = Icons.Default.WbSunny
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings
) {
    val isExporting by viewModel.isExporting.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "Permissions", icon = Icons.Default.Security) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Permissions")
                        Text(
                            "Location, Microphone, Notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Data Collection", icon = Icons.Default.Analytics) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.BarChart,
                    title = "Analytics",
                    subtitle = "Share anonymous usage data to improve app",
                    checked = appSettings.analyticsEnabled,
                    onCheckedChange = viewModel::updateAnalytics
                )
                
                HorizontalDivider()
                
                SettingsToggleWithIcon(
                    icon = Icons.Default.BugReport,
                    title = "Crash Reporting",
                    subtitle = "Send crash reports to help fix bugs",
                    checked = appSettings.crashReportingEnabled,
                    onCheckedChange = viewModel::updateCrashReporting
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Backup & Restore", icon = Icons.Default.Backup) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.CloudSync,
                    title = "Auto Backup",
                    subtitle = "Automatically backup your data",
                    checked = appSettings.autoBackupEnabled,
                    onCheckedChange = viewModel::updateAutoBackup
                )
                
                if (appSettings.autoBackupEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsSegmentedButton(
                        title = "Backup Frequency",
                        options = BackupFrequency.values().toList(),
                        selectedOption = appSettings.backupFrequency,
                        onOptionSelected = viewModel::updateBackupFrequency,
                        optionLabel = { it.displayName }
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("kiro_backup.json") },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Upload, null)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                    
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, null)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }
                }
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Data Management", icon = Icons.Default.Storage) {
                val isClearingChat by viewModel.isClearingChat.collectAsState()
                var showClearChatDialog by remember { mutableStateOf(false) }
                
                // Clear Chat History Dialog
                if (showClearChatDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearChatDialog = false },
                        title = { Text("Clear Chat History") },
                        text = { Text("Are you sure you want to clear all chat messages? This cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.clearChatHistory()
                                    showClearChatDialog = false
                                }
                            ) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearChatDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Clear Chat History Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Chat History", color = MaterialTheme.colorScheme.error)
                        Text(
                            "Delete all chat messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isClearingChat) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { showClearChatDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear chat",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingsDestructiveButton(
                    text = "Clear All Data",
                    onClick = { viewModel.resetAllSettings() },
                    icon = Icons.Default.DeleteForever
                )
            }
        }
    }
}

@Composable
fun AboutContent(modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "App Info", icon = Icons.Default.Info) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version")
                    Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Build")
                    Text("2024.11.27", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Support", icon = Icons.Default.Help) {
                listOf(
                    Triple(Icons.Default.HelpCenter, "Help Center", "FAQs and guides"),
                    Triple(Icons.Default.Feedback, "Send Feedback", "Report issues or suggestions"),
                    Triple(Icons.Default.Star, "Rate App", "Rate us on Play Store")
                ).forEach { (icon, title, subtitle) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                    }
                    HorizontalDivider()
                }
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Legal", icon = Icons.Default.Gavel) {
                listOf("Privacy Policy", "Terms of Service", "Open Source Licenses").forEach { title ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}


@Composable
fun WidgetContent(
    modifier: Modifier,
    viewModel: EnhancedSettingsViewModel,
    appSettings: AppSettings
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                SettingsSegmentedButton(
                    title = "Widget Theme",
                    options = WidgetTheme.values().toList(),
                    selectedOption = appSettings.widgetTheme,
                    onOptionSelected = viewModel::updateWidgetTheme,
                    optionLabel = { it.displayName },
                    icon = Icons.Default.ColorLens
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingsSlider(
                    title = "Widget Transparency",
                    value = appSettings.widgetTransparency.toFloat(),
                    onValueChange = { viewModel.updateWidgetTransparency(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 9,
                    valueLabel = "${appSettings.widgetTransparency}%",
                    icon = Icons.Default.Opacity
                )
            }
        }
        
        item {
            EnhancedSettingsSection(title = "Features", icon = Icons.Default.TouchApp) {
                SettingsToggleWithIcon(
                    icon = Icons.Default.FlashOn,
                    title = "Quick Actions",
                    subtitle = "Show quick action buttons on widget",
                    checked = appSettings.showWidgetQuickActions,
                    onCheckedChange = viewModel::updateWidgetQuickActions
                )
            }
        }
        
        item {
            SettingsInfoCard(
                title = "Add Widget",
                content = "Long press on your home screen and select Widgets to add the Kiro chat widget.",
                icon = Icons.Default.Widgets
            )
        }
    }
}
