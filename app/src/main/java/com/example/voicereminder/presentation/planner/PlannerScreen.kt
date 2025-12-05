package com.example.voicereminder.presentation.planner

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.TaskManager
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.domain.models.Task
import com.example.voicereminder.domain.models.Alarm
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unified Planner Screen - Combines Calendar, Tasks, and Alarms
 * Provides a single view for all scheduling and planning features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToTaskDetail: (Long?) -> Unit = {},
    onNavigateToAlarmDetail: (Long?) -> Unit = {},
    onNavigateToReminderDetail: (Long?) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Managers
    val reminderManager = remember { ReminderManager.getInstance(context) }
    val taskManager = remember { TaskManager.getInstance(context) }
    val alarmManager = remember { EnhancedAlarmManager.getInstance(context) }
    
    // State
    var selectedTab by remember { mutableStateOf(PlannerTab.TODAY) }
    val reminders by reminderManager.getAllRemindersFlow().collectAsState(initial = emptyList())
    val tasks by taskManager.getAllTasksFlow().collectAsState(initial = emptyList())
    val alarms by alarmManager.getAllAlarmsFlow().collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row for switching views
        PlannerTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
        
        // Content based on selected tab
        when (selectedTab) {
            PlannerTab.TODAY -> TodayView(
                reminders = reminders,
                tasks = tasks,
                alarms = alarms,
                onTaskComplete = { task ->
                    coroutineScope.launch { taskManager.completeTask(task.id) }
                },
                onAlarmToggle = { alarm ->
                    coroutineScope.launch { alarmManager.toggleAlarm(alarm.id, !alarm.isEnabled) }
                },
                onReminderClick = { onNavigateToReminderDetail(it.id) },
                onTaskClick = { onNavigateToTaskDetail(it.id) },
                onAlarmClick = { onNavigateToAlarmDetail(it.id) }
            )
            PlannerTab.TASKS -> TasksView(
                tasks = tasks,
                onTaskComplete = { task ->
                    coroutineScope.launch { taskManager.completeTask(task.id) }
                },
                onTaskClick = { onNavigateToTaskDetail(it.id) },
                onAddTask = { onNavigateToTaskDetail(null) },
                onTaskDelete = { task ->
                    coroutineScope.launch { taskManager.deleteTask(task.id) }
                }
            )
            PlannerTab.REMINDERS -> RemindersView(
                reminders = reminders,
                onReminderClick = { onNavigateToReminderDetail(it.id) },
                onReminderDelete = { reminder ->
                    coroutineScope.launch { reminderManager.deleteReminder(reminder.id) }
                }
            )
            PlannerTab.ALARMS -> AlarmsView(
                alarms = alarms,
                onAlarmToggle = { alarm ->
                    coroutineScope.launch { alarmManager.toggleAlarm(alarm.id, !alarm.isEnabled) }
                },
                onAlarmClick = { onNavigateToAlarmDetail(it.id) },
                onAddAlarm = { onNavigateToAlarmDetail(null) },
                onAlarmDelete = { alarm ->
                    coroutineScope.launch { alarmManager.deleteAlarm(alarm.id) }
                }
            )
        }
    }
}

enum class PlannerTab {
    TODAY, TASKS, REMINDERS, ALARMS
}

@Composable
fun PlannerTabRow(
    selectedTab: PlannerTab,
    onTabSelected: (PlannerTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp
    ) {
        PlannerTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                PlannerTab.TODAY -> Icons.Default.Today
                                PlannerTab.TASKS -> Icons.Default.CheckCircle
                                PlannerTab.REMINDERS -> Icons.Default.Notifications
                                PlannerTab.ALARMS -> Icons.Default.Alarm
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (tab) {
                                PlannerTab.TODAY -> "Today"
                                PlannerTab.TASKS -> "Tasks"
                                PlannerTab.REMINDERS -> "Reminders"
                                PlannerTab.ALARMS -> "Alarms"
                            },
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }
    }
}


/**
 * Today View - Shows all items due today across reminders, tasks, and alarms
 */
@Composable
fun TodayView(
    reminders: List<Reminder>,
    tasks: List<Task>,
    alarms: List<Alarm>,
    onTaskComplete: (Task) -> Unit,
    onAlarmToggle: (Alarm) -> Unit,
    onReminderClick: (Reminder) -> Unit,
    onTaskClick: (Task) -> Unit,
    onAlarmClick: (Alarm) -> Unit
) {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    
    // Filter items for today
    val todayReminders = reminders.filter { reminder ->
        reminder.scheduledTime?.toLocalDate() == today
    }
    val todayTasks = tasks.filter { task ->
        task.dueDate?.toLocalDate() == today && !task.isCompleted
    }
    val enabledAlarms = alarms.filter { it.isEnabled }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Date Header
            Text(
                text = today.format(dateFormatter),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        // Quick Stats
        item {
            QuickStatsCard(
                reminderCount = todayReminders.size,
                taskCount = todayTasks.size,
                alarmCount = enabledAlarms.size
            )
        }
        
        // Alarms Section
        if (enabledAlarms.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Alarms",
                    icon = Icons.Default.Alarm,
                    count = enabledAlarms.size
                )
            }
            items(enabledAlarms.take(3)) { alarm ->
                AlarmQuickCard(
                    alarm = alarm,
                    onToggle = { onAlarmToggle(alarm) },
                    onClick = { onAlarmClick(alarm) }
                )
            }
        }
        
        // Reminders Section
        if (todayReminders.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Reminders",
                    icon = Icons.Default.Notifications,
                    count = todayReminders.size
                )
            }
            items(todayReminders.take(5)) { reminder ->
                ReminderQuickCard(
                    reminder = reminder,
                    onClick = { onReminderClick(reminder) }
                )
            }
        }
        
        // Tasks Section
        if (todayTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Tasks Due Today",
                    icon = Icons.Default.CheckCircle,
                    count = todayTasks.size
                )
            }
            items(todayTasks.take(5)) { task ->
                TaskQuickCard(
                    task = task,
                    onComplete = { onTaskComplete(task) },
                    onClick = { onTaskClick(task) }
                )
            }
        }
        
        // Empty State
        if (todayReminders.isEmpty() && todayTasks.isEmpty() && enabledAlarms.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "Nothing scheduled for today!\nUse the chat to add reminders, tasks, or alarms."
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun QuickStatsCard(
    reminderCount: Int,
    taskCount: Int,
    alarmCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Notifications,
                count = reminderCount,
                label = "Reminders"
            )
            StatItem(
                icon = Icons.Default.CheckCircle,
                count = taskCount,
                label = "Tasks"
            )
            StatItem(
                icon = Icons.Default.Alarm,
                count = alarmCount,
                label = "Alarms"
            )
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}


// Quick Card Components for Today View
@Composable
fun AlarmQuickCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.getFormattedTime(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = alarm.label,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun ReminderQuickCard(
    reminder: Reminder,
    onClick: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val isLocationBased = reminder.reminderType.name == "LOCATION_BASED"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLocationBased) Icons.Default.LocationOn else Icons.Default.Notifications,
                contentDescription = null,
                tint = if (isLocationBased) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Text(
                    text = if (isLocationBased) "Location-based" else reminder.scheduledTime?.format(timeFormatter) ?: "",
                    fontSize = 12.sp,
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
fun TaskQuickCard(
    task: Task,
    onComplete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onComplete() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Priority indicator
                    val priorityColor = com.example.voicereminder.data.Priority.getColor(task.priority)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = task.priority.name,
                            fontSize = 10.sp,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // Category
                    Text(
                        text = com.example.voicereminder.data.TaskCategory.getDisplayName(task.category),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tasks View - Full task list with filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksView(
    tasks: List<Task>,
    onTaskComplete: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onAddTask: () -> Unit,
    onTaskDelete: (Task) -> Unit
) {
    var showCompleted by remember { mutableStateOf(false) }
    
    val filteredTasks = if (showCompleted) tasks else tasks.filter { !it.isCompleted }
    val incompleteTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${incompleteTasks.size} tasks remaining",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showCompleted = !showCompleted }) {
                        Text(if (showCompleted) "Hide Completed" else "Show Completed (${completedTasks.size})")
                    }
                }
            }
            
            items(filteredTasks) { task ->
                TaskQuickCard(
                    task = task,
                    onComplete = { onTaskComplete(task) },
                    onClick = { onTaskClick(task) }
                )
            }
            
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = if (showCompleted) "No tasks yet" else "All tasks completed! 🎉"
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        
        // FAB for adding tasks
        FloatingActionButton(
            onClick = onAddTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

/**
 * Reminders View - Full reminder list
 */
@Composable
fun RemindersView(
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit
) {
    val pendingReminders = reminders.filter { it.status.name == "PENDING" }
    val completedReminders = reminders.filter { it.status.name != "PENDING" }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "${pendingReminders.size} pending reminders",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        if (pendingReminders.isNotEmpty()) {
            item { SectionHeader(title = "Upcoming", icon = Icons.Default.Schedule, count = pendingReminders.size) }
            items(pendingReminders) { reminder ->
                ReminderQuickCard(
                    reminder = reminder,
                    onClick = { onReminderClick(reminder) }
                )
            }
        }
        
        if (completedReminders.isNotEmpty()) {
            item { SectionHeader(title = "Past", icon = Icons.Default.History, count = completedReminders.size) }
            items(completedReminders.take(10)) { reminder ->
                ReminderQuickCard(
                    reminder = reminder,
                    onClick = { onReminderClick(reminder) }
                )
            }
        }
        
        if (reminders.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "No reminders yet.\nSay \"remind me to...\" in the chat!"
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * Alarms View - Full alarm list
 */
@Composable
fun AlarmsView(
    alarms: List<Alarm>,
    onAlarmToggle: (Alarm) -> Unit,
    onAlarmClick: (Alarm) -> Unit,
    onAddAlarm: () -> Unit,
    onAlarmDelete: (Alarm) -> Unit
) {
    val enabledAlarms = alarms.filter { it.isEnabled }
    val disabledAlarms = alarms.filter { !it.isEnabled }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "${enabledAlarms.size} active alarms",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(enabledAlarms) { alarm ->
                AlarmQuickCard(
                    alarm = alarm,
                    onToggle = { onAlarmToggle(alarm) },
                    onClick = { onAlarmClick(alarm) }
                )
            }
            
            if (disabledAlarms.isNotEmpty()) {
                item {
                    Text(
                        text = "Disabled",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(disabledAlarms) { alarm ->
                    AlarmQuickCard(
                        alarm = alarm,
                        onToggle = { onAlarmToggle(alarm) },
                        onClick = { onAlarmClick(alarm) }
                    )
                }
            }
            
            if (alarms.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No alarms set.\nSay \"set alarm for 7am\" in the chat!"
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        
        // FAB for adding alarms
        FloatingActionButton(
            onClick = onAddAlarm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }
}
