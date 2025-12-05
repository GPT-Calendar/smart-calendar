package com.example.voicereminder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.models.Reminder
import java.time.format.DateTimeFormatter

/**
 * Global Top Bar with Facebook/Instagram style notification dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTopBar(
    title: String = "Smart Calendar",
    subtitle: String? = null,
    onMenuClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onReminderClick: (Reminder) -> Unit = {},
    onReminderDelete: (Reminder) -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    var showQuickMenu by remember { mutableStateOf(false) }
    var showNotificationPanel by remember { mutableStateOf(false) }
    
    // Get pending reminders
    val reminderManager = remember { ReminderManager.getInstance(context) }
    val activeReminders by reminderManager.getActiveRemindersFlow().collectAsState(initial = emptyList())
    val pendingCount = activeReminders.size
    
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    val topBarColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFF305CDE)
    }
    
    val iconColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    
    val titleColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    
    val subtitleColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.White.copy(alpha = 0.8f)
    }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = topBarColor,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showBackButton) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = iconColor)
                        }
                    } else {
                        IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.AccountCircle, "Profile", tint = iconColor, modifier = Modifier.size(28.dp))
                        }
                    }
                    
                    Column {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                        subtitle?.let { Text(it, fontSize = 12.sp, color = subtitleColor) }
                    }
                }
                
                // Right section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    actions()
                    
                    // Notification bell with dropdown
                    Box {
                        IconButton(
                            onClick = { showNotificationPanel = !showNotificationPanel },
                            modifier = Modifier.size(40.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (pendingCount > 0) {
                                        Badge(containerColor = Color(0xFFFF5252)) {
                                            Text(if (pendingCount > 9) "9+" else pendingCount.toString(), fontSize = 10.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, "Notifications", tint = iconColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    // Menu
                    Box {
                        IconButton(onClick = { showQuickMenu = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.MoreVert, "Menu", tint = iconColor, modifier = Modifier.size(24.dp))
                        }
                        
                        DropdownMenu(expanded = showQuickMenu, onDismissRequest = { showQuickMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { showQuickMenu = false; onMenuClick() },
                                leadingIcon = { Icon(Icons.Default.Settings, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                onClick = { showQuickMenu = false; onProfileClick() },
                                leadingIcon = { Icon(Icons.Default.Person, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Help & Feedback") },
                                onClick = { showQuickMenu = false },
                                leadingIcon = { Icon(Icons.Default.Help, null) }
                            )
                        }
                    }
                }
            }
        }
        
        // Notification dropdown panel (Facebook/Instagram style)
        if (showNotificationPanel) {
            NotificationDropdownPanel(
                reminders = activeReminders,
                onDismiss = { showNotificationPanel = false },
                onReminderClick = { reminder ->
                    showNotificationPanel = false
                    onReminderClick(reminder)
                },
                onReminderDelete = onReminderDelete,
                onViewAll = {
                    showNotificationPanel = false
                    onNotificationsClick()
                }
            )
        }
    }
}

@Composable
private fun NotificationDropdownPanel(
    reminders: List<Reminder>,
    onDismiss: () -> Unit,
    onReminderClick: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit,
    onViewAll: () -> Unit
) {
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .padding(top = 56.dp, end = 8.dp)
                .width(320.dp)
                .heightIn(max = 400.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (reminders.isNotEmpty()) {
                        Text(
                            "${reminders.size} pending",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                HorizontalDivider()
                
                if (reminders.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No pending reminders",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Reminder list
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(reminders.take(5)) { reminder ->
                            NotificationItem(
                                reminder = reminder,
                                onClick = { onReminderClick(reminder) },
                                onDelete = { onReminderDelete(reminder) }
                            )
                        }
                    }
                    
                    // View all button
                    if (reminders.size > 5 || reminders.isNotEmpty()) {
                        HorizontalDivider()
                        TextButton(
                            onClick = onViewAll,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text("View all reminders")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    reminder: Reminder,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (reminder.locationData != null) Icons.Default.LocationOn else Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reminder.scheduledTime?.format(timeFormatter) 
                    ?: if (reminder.locationData != null) "Location-based" else "Pending",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Delete button
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact Top Bar variant for secondary screens
 */
@Composable
fun CompactTopBar(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    val topBarColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFF305CDE)
    val contentColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color.White

    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = topBarColor,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, "Back", tint = contentColor)
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = contentColor, modifier = Modifier.weight(1f))
            actions()
        }
    }
}
