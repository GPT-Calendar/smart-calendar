package com.example.voicereminder.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.presentation.components.CompactTopBar
import com.example.voicereminder.data.settings.ThemeManager
import com.example.voicereminder.data.settings.ThemeMode

/**
 * Settings Hub - Central settings screen with categorized tabs
 * Production-ready settings organization
 */

enum class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    GENERAL("General", Icons.Default.Settings, "App preferences & display"),
    AI("AI Assistant", Icons.Default.SmartToy, "AI provider & model settings"),
    CALENDAR("Calendar", Icons.Default.CalendarToday, "Calendar sync & reminders"),
    FINANCE("Finance", Icons.Default.AccountBalance, "Budget & tracking settings"),
    LOCATION("Location", Icons.Default.LocationOn, "Location services & geofencing"),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications, "Alert preferences"),
    WIDGET("Widget", Icons.Default.Widgets, "Home screen widget settings"),
    PRIVACY("Privacy & Security", Icons.Default.Security, "Data & permissions"),
    ABOUT("About", Icons.Default.Info, "App info & support")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (SettingsCategory) -> Unit = {},
    themeManager: ThemeManager? = null
) {
    val context = LocalContext.current
    val actualThemeManager = themeManager ?: remember { ThemeManager.getInstance(context) }
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    
    Scaffold(
        topBar = {
            CompactTopBar(
                title = if (selectedCategory != null) selectedCategory!!.title else "Settings",
                onBackClick = {
                    if (selectedCategory != null) {
                        selectedCategory = null
                    } else {
                        onNavigateBack()
                    }
                }
            )
        }
    ) { padding ->
        if (selectedCategory == null) {
            // Show settings categories list
            SettingsCategoryList(
                modifier = Modifier.padding(padding),
                onCategoryClick = { category ->
                    selectedCategory = category
                },
                themeManager = actualThemeManager
            )
        } else {
            // Show selected category content
            SettingsCategoryContent(
                category = selectedCategory!!,
                modifier = Modifier.padding(padding),
                themeManager = actualThemeManager
            )
        }
    }
}

@Composable
fun SettingsCategoryList(
    modifier: Modifier = Modifier,
    onCategoryClick: (SettingsCategory) -> Unit,
    themeManager: ThemeManager
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // User profile section
        item {
            UserProfileCard()
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Quick settings toggles
        item {
            QuickSettingsCard(themeManager = themeManager)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Settings categories
        item {
            Text(
                text = "Settings",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        
        items(SettingsCategory.values().toList()) { category ->
            SettingsCategoryItem(
                category = category,
                onClick = { onCategoryClick(category) }
            )
        }
        
        // App version footer
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Kiro v1.0.0",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun UserProfileCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage your profile",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun QuickSettingsCard(themeManager: ThemeManager) {
    val themeMode by themeManager.themeMode.collectAsState()
    // Calculate actual dark mode state based on theme mode and system setting
    val isSystemDark = isSystemInDarkTheme()
    val isDarkMode = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }
    var wakeWord by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Quick Settings",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            QuickToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                checked = isDarkMode,
                onCheckedChange = { enabled ->
                    themeManager.setThemeMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
                }
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            QuickToggleItem(
                icon = Icons.Default.Mic,
                title = "\"Hey Kiro\" Wake Word",
                checked = wakeWord,
                onCheckedChange = { wakeWord = it }
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            QuickToggleItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                checked = notifications,
                onCheckedChange = { notifications = it }
            )
        }
    }
}

@Composable
fun QuickToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsCategoryItem(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}


/**
 * Content for each settings category
 */
@Composable
fun SettingsCategoryContent(
    category: SettingsCategory,
    modifier: Modifier = Modifier,
    themeManager: ThemeManager? = null
) {
    val context = LocalContext.current
    val actualThemeManager = themeManager ?: remember { ThemeManager.getInstance(context) }
    
    when (category) {
        SettingsCategory.GENERAL -> GeneralSettingsContent(modifier, actualThemeManager)
        SettingsCategory.AI -> AISettingsContent(modifier)
        SettingsCategory.CALENDAR -> CalendarSettingsContent(modifier)
        SettingsCategory.FINANCE -> FinanceSettingsContent(modifier)
        SettingsCategory.LOCATION -> LocationSettingsContent(modifier)
        SettingsCategory.NOTIFICATIONS -> NotificationSettingsContent(modifier)
        SettingsCategory.WIDGET -> WidgetSettingsContent(modifier)
        SettingsCategory.PRIVACY -> PrivacySettingsContent(modifier)
        SettingsCategory.ABOUT -> AboutSettingsContent(modifier)
    }
}

@Composable
fun GeneralSettingsContent(modifier: Modifier = Modifier, themeManager: ThemeManager? = null) {
    val context = LocalContext.current
    val actualThemeManager = themeManager ?: remember { ThemeManager.getInstance(context) }
    val themeMode by actualThemeManager.themeMode.collectAsState()
    
    var language by remember { mutableStateOf("English") }
    val themeValue = when (themeMode) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System"
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Appearance") {
                SettingsDropdownItem(
                    title = "Theme",
                    value = themeValue,
                    options = listOf("Light", "Dark", "System"),
                    onValueChange = { selected ->
                        val newMode = when (selected) {
                            "Light" -> ThemeMode.LIGHT
                            "Dark" -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        }
                        actualThemeManager.setThemeMode(newMode)
                    }
                )
                SettingsDropdownItem(
                    title = "Language",
                    value = language,
                    options = listOf("English", "Spanish", "French", "German", "Arabic"),
                    onValueChange = { language = it }
                )
            }
        }
        
        item {
            SettingsSection(title = "Display") {
                var compactMode by remember { mutableStateOf(false) }
                var animations by remember { mutableStateOf(true) }
                
                SettingsToggleItem(
                    title = "Compact Mode",
                    subtitle = "Show more content on screen",
                    checked = compactMode,
                    onCheckedChange = { compactMode = it }
                )
                SettingsToggleItem(
                    title = "Animations",
                    subtitle = "Enable UI animations",
                    checked = animations,
                    onCheckedChange = { animations = it }
                )
            }
        }
    }
}

@Composable
fun AISettingsContent(modifier: Modifier = Modifier) {
    // Reuse existing SettingsScreen content
    val viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val settings by viewModel.settings.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(title = "AI Provider") {
                com.example.voicereminder.data.settings.AIProvider.values().forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateProvider(provider) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.provider == provider,
                            onClick = { viewModel.updateProvider(provider) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF305CDE)
                            )
                        )
                        Text(
                            text = provider.name,
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        }
        
        item {
            SettingsSection(title = "Provider Configuration") {
                when (settings.provider) {
                    com.example.voicereminder.data.settings.AIProvider.OLLAMA -> {
                        OllamaSettings(settings, viewModel)
                    }
                    com.example.voicereminder.data.settings.AIProvider.OPENAI -> {
                        OpenAISettings(settings, viewModel)
                    }
                    com.example.voicereminder.data.settings.AIProvider.ANTHROPIC -> {
                        AnthropicSettings(settings, viewModel)
                    }
                    com.example.voicereminder.data.settings.AIProvider.CUSTOM -> {
                        CustomSettings(settings, viewModel)
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF305CDE)
                )
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isTesting) "Testing..." else "Test Connection")
            }
        }
        
        testResult?.let { (success, message) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (success) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message, color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarSettingsContent(modifier: Modifier = Modifier) {
    var syncEnabled by remember { mutableStateOf(true) }
    var defaultReminder by remember { mutableStateOf("15 minutes") }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Sync") {
                SettingsToggleItem(
                    title = "Calendar Sync",
                    subtitle = "Sync with device calendar",
                    checked = syncEnabled,
                    onCheckedChange = { syncEnabled = it }
                )
            }
        }
        
        item {
            SettingsSection(title = "Reminders") {
                SettingsDropdownItem(
                    title = "Default Reminder",
                    value = defaultReminder,
                    options = listOf("5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour"),
                    onValueChange = { defaultReminder = it }
                )
            }
        }
    }
}

@Composable
fun FinanceSettingsContent(modifier: Modifier = Modifier) {
    var currency by remember { mutableStateOf("USD") }
    var smsTracking by remember { mutableStateOf(true) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Currency") {
                SettingsDropdownItem(
                    title = "Default Currency",
                    value = currency,
                    options = listOf("USD", "EUR", "GBP", "JPY", "INR", "SAR"),
                    onValueChange = { currency = it }
                )
            }
        }
        
        item {
            SettingsSection(title = "Tracking") {
                SettingsToggleItem(
                    title = "SMS Transaction Tracking",
                    subtitle = "Auto-detect transactions from SMS",
                    checked = smsTracking,
                    onCheckedChange = { smsTracking = it }
                )
            }
        }
    }
}

@Composable
fun LocationSettingsContent(modifier: Modifier = Modifier) {
    var locationEnabled by remember { mutableStateOf(true) }
    var geofencing by remember { mutableStateOf(true) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Location Services") {
                SettingsToggleItem(
                    title = "Location Access",
                    subtitle = "Enable location-based features",
                    checked = locationEnabled,
                    onCheckedChange = { locationEnabled = it }
                )
                SettingsToggleItem(
                    title = "Geofencing",
                    subtitle = "Trigger reminders at locations",
                    checked = geofencing,
                    onCheckedChange = { geofencing = it }
                )
            }
        }
        
        item {
            SettingsSection(title = "Saved Places") {
                SettingsActionItem(
                    title = "Manage Saved Locations",
                    subtitle = "Home, Work, and custom places",
                    onClick = { /* Navigate to location settings */ }
                )
            }
        }
    }
}

@Composable
fun NotificationSettingsContent(modifier: Modifier = Modifier) {
    var reminderNotifs by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Notifications") {
                SettingsToggleItem(
                    title = "Reminder Notifications",
                    subtitle = "Get notified for reminders",
                    checked = reminderNotifs,
                    onCheckedChange = { reminderNotifs = it }
                )
                SettingsToggleItem(
                    title = "Sound",
                    subtitle = "Play notification sound",
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )
                SettingsToggleItem(
                    title = "Vibration",
                    subtitle = "Vibrate on notifications",
                    checked = vibration,
                    onCheckedChange = { vibration = it }
                )
            }
        }
    }
}

@Composable
fun WidgetSettingsContent(modifier: Modifier = Modifier) {
    var widgetTheme by remember { mutableStateOf("System") }
    var showQuickActions by remember { mutableStateOf(true) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Appearance") {
                SettingsDropdownItem(
                    title = "Widget Theme",
                    value = widgetTheme,
                    options = listOf("Light", "Dark", "System"),
                    onValueChange = { widgetTheme = it }
                )
            }
        }
        
        item {
            SettingsSection(title = "Features") {
                SettingsToggleItem(
                    title = "Quick Actions",
                    subtitle = "Show quick action buttons on widget",
                    checked = showQuickActions,
                    onCheckedChange = { showQuickActions = it }
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Add Widget",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Long press on your home screen and select Widgets to add the Kiro chat widget.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Permissions") {
                SettingsActionItem(
                    title = "Manage Permissions",
                    subtitle = "Location, Microphone, Notifications",
                    onClick = { /* Open app settings */ }
                )
            }
        }
        
        item {
            SettingsSection(title = "Data") {
                SettingsActionItem(
                    title = "Export Data",
                    subtitle = "Download your data",
                    onClick = { }
                )
                SettingsActionItem(
                    title = "Clear Data",
                    subtitle = "Delete all app data",
                    onClick = { },
                    isDestructive = true
                )
            }
        }
    }
}

@Composable
fun AboutSettingsContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "App Info") {
                SettingsInfoItem(title = "Version", value = "1.0.0")
                SettingsInfoItem(title = "Build", value = "2024.11.26")
            }
        }
        
        item {
            SettingsSection(title = "Support") {
                SettingsActionItem(
                    title = "Help Center",
                    subtitle = "FAQs and guides",
                    onClick = { }
                )
                SettingsActionItem(
                    title = "Send Feedback",
                    subtitle = "Report issues or suggestions",
                    onClick = { }
                )
                SettingsActionItem(
                    title = "Rate App",
                    subtitle = "Rate us on Play Store",
                    onClick = { }
                )
            }
        }
        
        item {
            SettingsSection(title = "Legal") {
                SettingsActionItem(
                    title = "Privacy Policy",
                    subtitle = null,
                    onClick = { }
                )
                SettingsActionItem(
                    title = "Terms of Service",
                    subtitle = null,
                    onClick = { }
                )
                SettingsActionItem(
                    title = "Open Source Licenses",
                    subtitle = null,
                    onClick = { }
                )
            }
        }
    }
}

// Reusable Settings Components

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
