package com.example.voicereminder.presentation.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.data.settings.*
import com.example.voicereminder.presentation.components.CompactTopBar
import com.example.voicereminder.presentation.settings.components.*

/**
 * Enhanced Settings Hub with search, auto-save, and comprehensive options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsHubScreen(
    onNavigateBack: () -> Unit,
    viewModel: EnhancedSettingsViewModel = viewModel()
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val aiSettings by viewModel.aiSettings.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopBar(
                title = if (selectedCategory != null) selectedCategory!!.title else "Settings",
                onBackClick = {
                    if (selectedCategory != null) {
                        selectedCategory = null
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    // Auto-save indicator
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (selectedCategory == null) {
            EnhancedSettingsCategoryList(
                modifier = Modifier.padding(padding),
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onCategoryClick = { selectedCategory = it },
                appSettings = appSettings,
                themeMode = themeMode,
                viewModel = viewModel
            )
        } else {
            EnhancedCategoryContent(
                category = selectedCategory!!,
                modifier = Modifier.padding(padding),
                viewModel = viewModel,
                appSettings = appSettings,
                aiSettings = aiSettings,
                themeMode = themeMode
            )
        }
    }
}

@Composable
fun EnhancedSettingsCategoryList(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryClick: (SettingsCategory) -> Unit,
    appSettings: AppSettings,
    themeMode: ThemeMode,
    viewModel: EnhancedSettingsViewModel
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Search bar
        item {
            SettingsSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = { onSearchQueryChange("") }
            )
        }
        
        // Search results
        if (searchQuery.isNotEmpty()) {
            val filteredSettings = viewModel.getFilteredSettings()
            if (filteredSettings.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(filteredSettings) { item ->
                    SearchResultItem(
                        item = item,
                        onClick = { onCategoryClick(item.category) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No settings found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // User profile section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                UserProfileCard(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Quick settings toggles
            item {
                EnhancedQuickSettingsCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    appSettings = appSettings,
                    themeMode = themeMode,
                    viewModel = viewModel
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Settings categories
            item {
                Text(
                    text = "Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            items(SettingsCategory.values().toList()) { category ->
                SettingsCategoryItem(
                    category = category,
                    onClick = { onCategoryClick(category) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // App version footer
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Kiro v1.0.0",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    item: SettingItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "in ${item.category.title}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
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
fun UserProfileCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
fun EnhancedQuickSettingsCard(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    themeMode: ThemeMode,
    viewModel: EnhancedSettingsViewModel
) {
    // Calculate actual dark mode state based on theme mode and system setting
    val isSystemDark = isSystemInDarkTheme()
    val isActuallyDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
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
            
            SettingsToggleWithIcon(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                checked = isActuallyDark,
                onCheckedChange = { enabled ->
                    viewModel.updateThemeMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
                }
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            SettingsToggleWithIcon(
                icon = Icons.Default.Mic,
                title = "\"Hey Kiro\" Wake Word",
                checked = appSettings.wakeWordEnabled,
                onCheckedChange = viewModel::updateWakeWordEnabled
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            SettingsToggleWithIcon(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                checked = appSettings.notificationsEnabled,
                onCheckedChange = viewModel::updateNotificationsEnabled
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            SettingsToggleWithIcon(
                icon = Icons.Default.LocationOn,
                title = "Location Services",
                checked = appSettings.locationEnabled,
                onCheckedChange = viewModel::updateLocationEnabled
            )
        }
    }
}

@Composable
fun SettingsCategoryItem(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
