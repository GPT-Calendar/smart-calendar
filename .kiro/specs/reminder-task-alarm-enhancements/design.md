# Design Document

## Overview

This design document outlines the implementation of enhanced reminder, to-do task, and alarm clock features for the Voice Reminder Assistant application. The enhancements build upon the existing reminder infrastructure to provide a comprehensive task management system.

## Architecture

### Data Layer Changes

```
data/
├── entity/
│   ├── TaskEntity.kt (NEW)
│   ├── AlarmEntity.kt (NEW)
│   └── RecurrenceRule.kt (NEW)
├── dao/
│   ├── TaskDao.kt (NEW)
│   └── AlarmDao.kt (NEW)
├── ReminderEntity.kt (ENHANCED)
├── ReminderDao.kt (ENHANCED)
├── TaskDatabase.kt (NEW)
└── Priority.kt (NEW)
```

### Domain Layer Changes

```
domain/
├── models/
│   ├── Task.kt (NEW)
│   ├── Alarm.kt (NEW)
│   └── RecurrencePattern.kt (NEW)
├── TaskManager.kt (NEW)
├── AlarmManager.kt (NEW - enhanced)
├── RecurrenceScheduler.kt (NEW)
├── SnoozeManager.kt (NEW)
└── CommandParser.kt (ENHANCED)
```

### Presentation Layer Changes

```
presentation/
├── tasks/
│   ├── TaskListScreen.kt (NEW)
│   ├── TaskDetailScreen.kt (NEW)
│   └── TaskViewModel.kt (NEW)
├── alarms/
│   ├── AlarmListScreen.kt (NEW)
│   ├── AlarmDetailScreen.kt (NEW)
│   ├── AlarmTriggerScreen.kt (NEW)
│   └── AlarmViewModel.kt (NEW)
├── calendar/
│   └── CalendarScreen.kt (ENHANCED)
└── components/
    ├── PrioritySelector.kt (NEW)
    ├── CategorySelector.kt (NEW)
    ├── RecurrenceSelector.kt (NEW)
    └── SnoozeDialog.kt (NEW)
```

## Data Models

### TaskEntity

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null, // Unix timestamp
    val priority: Priority = Priority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val recurrenceRule: String? = null, // JSON for RecurrenceRule
    val parentTaskId: Long? = null // For subtasks
)
```

### AlarmEntity

```kotlin
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String = "Alarm",
    val hour: Int, // 0-23
    val minute: Int, // 0-59
    val isEnabled: Boolean = true,
    val repeatDays: String = "", // Comma-separated: "1,2,3,4,5" for weekdays
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeCount: Int = 0,
    val lastTriggered: Long? = null,
    val createdAt: Long
)
```

### Enhanced ReminderEntity

```kotlin
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,
    val scheduledTime: Long?,
    val status: ReminderStatus,
    val createdAt: Long,
    val reminderType: ReminderType = ReminderType.TIME_BASED,
    val locationData: String? = null,
    val geofenceId: String? = null,
    // NEW FIELDS
    val priority: Priority = Priority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val recurrenceRule: String? = null,
    val snoozeCount: Int = 0,
    val originalScheduledTime: Long? = null, // For tracking snoozes
    val notes: String? = null
)
```

### Priority Enum

```kotlin
enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}
```

### TaskCategory Enum

```kotlin
enum class TaskCategory {
    WORK,
    PERSONAL,
    SHOPPING,
    HEALTH,
    FINANCE,
    HOME,
    CUSTOM
}
```

### RecurrenceRule

```kotlin
data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1, // Every X days/weeks/months
    val daysOfWeek: List<Int>? = null, // 1=Mon, 7=Sun
    val dayOfMonth: Int? = null,
    val endDate: Long? = null,
    val maxOccurrences: Int? = null
)

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}
```

## Components and Interfaces

### TaskManager

```kotlin
class TaskManager(
    private val database: TaskDatabase,
    private val context: Context
) {
    suspend fun createTask(
        title: String,
        description: String? = null,
        dueDate: LocalDateTime? = null,
        priority: Priority = Priority.MEDIUM,
        category: TaskCategory = TaskCategory.PERSONAL,
        recurrenceRule: RecurrenceRule? = null
    ): Long
    
    suspend fun completeTask(taskId: Long)
    suspend fun uncompleteTask(taskId: Long)
    suspend fun deleteTask(taskId: Long)
    suspend fun updateTask(task: Task)
    
    fun getTasksFlow(): Flow<List<Task>>
    fun getTasksByCategory(category: TaskCategory): Flow<List<Task>>
    fun getTasksByPriority(priority: Priority): Flow<List<Task>>
    fun getTasksDueOn(date: LocalDate): Flow<List<Task>>
    fun getOverdueTasks(): Flow<List<Task>>
    
    suspend fun getTaskProgress(): TaskProgress
}

data class TaskProgress(
    val total: Int,
    val completed: Int,
    val overdue: Int
)
```

### Enhanced AlarmManager

```kotlin
class EnhancedAlarmManager(
    private val database: AlarmDatabase,
    private val context: Context
) {
    suspend fun createAlarm(
        hour: Int,
        minute: Int,
        label: String = "Alarm",
        repeatDays: List<Int> = emptyList(),
        vibrate: Boolean = true
    ): Long
    
    suspend fun toggleAlarm(alarmId: Long, enabled: Boolean)
    suspend fun deleteAlarm(alarmId: Long)
    suspend fun snoozeAlarm(alarmId: Long, minutes: Int)
    
    fun getAlarmsFlow(): Flow<List<Alarm>>
    fun getNextAlarm(): Flow<Alarm?>
    
    suspend fun rescheduleAllAlarms()
}
```

### SnoozeManager

```kotlin
class SnoozeManager(
    private val context: Context,
    private val reminderManager: ReminderManager,
    private val alarmManager: EnhancedAlarmManager
) {
    suspend fun snoozeReminder(reminderId: Long, minutes: Int): Boolean
    suspend fun snoozeAlarm(alarmId: Long, minutes: Int): Boolean
    suspend fun postponeReminder(reminderId: Long, newTime: LocalDateTime): Boolean
    
    fun getSnoozeOptions(): List<SnoozeOption>
}

data class SnoozeOption(
    val label: String,
    val minutes: Int
)
```

### RecurrenceScheduler

```kotlin
class RecurrenceScheduler(
    private val context: Context
) {
    fun calculateNextOccurrence(
        currentTime: LocalDateTime,
        rule: RecurrenceRule
    ): LocalDateTime?
    
    fun getOccurrencesInRange(
        rule: RecurrenceRule,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDateTime>
    
    suspend fun scheduleNextRecurrence(
        reminderId: Long,
        rule: RecurrenceRule
    )
}
```

### Enhanced CommandParser

New voice command patterns:

```kotlin
// Task commands
private val TASK_PATTERN = Regex(
    """(?:add|create|new)\s+task\s+(.+)""",
    RegexOption.IGNORE_CASE
)

private val COMPLETE_TASK_PATTERN = Regex(
    """(?:mark|complete|finish|done)\s+(?:task\s+)?(.+?)(?:\s+as\s+done)?""",
    RegexOption.IGNORE_CASE
)

// Recurring alarm pattern
private val RECURRING_ALARM_PATTERN = Regex(
    """(?:set\s+)?alarm(?:\s+at|\s+for)?\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\s+(?:every|on)\s+(weekdays?|weekends?|monday|tuesday|wednesday|thursday|friday|saturday|sunday)""",
    RegexOption.IGNORE_CASE
)

// Recurring reminder pattern
private val RECURRING_REMINDER_PATTERN = Regex(
    """remind me\s+(.+?)\s+every\s+(day|week|month|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\s+at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
    RegexOption.IGNORE_CASE
)

// Snooze command
private val SNOOZE_PATTERN = Regex(
    """snooze(?:\s+for)?\s+(\d+)\s*(minutes?|hours?)""",
    RegexOption.IGNORE_CASE
)
```

## UI Components

### TaskListScreen

```kotlin
@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onAddTask: () -> Unit,
    selectedCategory: TaskCategory?,
    onCategoryChange: (TaskCategory?) -> Unit,
    modifier: Modifier = Modifier
)
```

Features:
- Grouped by category or due date
- Swipe to complete
- Priority indicators (colored left border)
- Progress bar at top
- Filter chips for categories
- FAB for adding new task

### AlarmListScreen

```kotlin
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onAlarmClick: (Alarm) -> Unit,
    onAlarmToggle: (Alarm, Boolean) -> Unit,
    onAddAlarm: () -> Unit,
    modifier: Modifier = Modifier
)
```

Features:
- Toggle switch for each alarm
- Repeat days displayed as chips
- Next alarm time highlighted
- Swipe to delete
- FAB for adding new alarm

### AlarmTriggerScreen

```kotlin
@Composable
fun AlarmTriggerScreen(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

Features:
- Full-screen overlay
- Large time display
- Alarm label
- Dismiss button (slide to dismiss)
- Snooze button with duration options
- Animated background

### PrioritySelector

```kotlin
@Composable
fun PrioritySelector(
    selectedPriority: Priority,
    onPrioritySelected: (Priority) -> Unit,
    modifier: Modifier = Modifier
)
```

Visual design:
- HIGH: Red indicator (#D32F2F)
- MEDIUM: Orange indicator (#FF9800)
- LOW: Green indicator (#4CAF50)

### CategorySelector

```kotlin
@Composable
fun CategorySelector(
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier
)
```

### RecurrenceSelector

```kotlin
@Composable
fun RecurrenceSelector(
    selectedRule: RecurrenceRule?,
    onRuleSelected: (RecurrenceRule?) -> Unit,
    modifier: Modifier = Modifier
)
```

Options:
- None (one-time)
- Daily
- Weekly (select days)
- Monthly (select day of month)
- Custom

### SnoozeDialog

```kotlin
@Composable
fun SnoozeDialog(
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

Options:
- 5 minutes
- 10 minutes
- 15 minutes
- 30 minutes
- 1 hour
- Custom time

## Navigation Updates

Add new navigation destinations:

```kotlin
sealed class Screen {
    // Existing
    object Assistant : Screen()
    object Calendar : Screen()
    object Finance : Screen()
    object AllEvents : Screen()
    object Map : Screen()
    
    // New
    object Tasks : Screen()
    object TaskDetail : Screen()
    object Alarms : Screen()
    object AlarmDetail : Screen()
}
```

Update bottom navigation to include Tasks tab (replace or add alongside existing tabs).

## Database Migration

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to reminders table
        database.execSQL("""
            ALTER TABLE reminders ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'
        """)
        database.execSQL("""
            ALTER TABLE reminders ADD COLUMN category TEXT NOT NULL DEFAULT 'PERSONAL'
        """)
        database.execSQL("""
            ALTER TABLE reminders ADD COLUMN recurrenceRule TEXT
        """)
        database.execSQL("""
            ALTER TABLE reminders ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0
        """)
        database.execSQL("""
            ALTER TABLE reminders ADD COLUMN originalScheduledTime INTEGER
        """)
        database.execSQL("""
            ALTER TABLE reminders ADD COLUMN notes TEXT
        """)
        
        // Create tasks table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                dueDate INTEGER,
                priority TEXT NOT NULL DEFAULT 'MEDIUM',
                category TEXT NOT NULL DEFAULT 'PERSONAL',
                isCompleted INTEGER NOT NULL DEFAULT 0,
                completedAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                recurrenceRule TEXT,
                parentTaskId INTEGER
            )
        """)
        
        // Create alarms table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS alarms (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                label TEXT NOT NULL DEFAULT 'Alarm',
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                isEnabled INTEGER NOT NULL DEFAULT 1,
                repeatDays TEXT NOT NULL DEFAULT '',
                soundUri TEXT,
                vibrate INTEGER NOT NULL DEFAULT 1,
                snoozeCount INTEGER NOT NULL DEFAULT 0,
                lastTriggered INTEGER,
                createdAt INTEGER NOT NULL
            )
        """)
    }
}
```

## Notification Channels

```kotlin
object NotificationChannels {
    const val REMINDERS = "reminders"
    const val TASKS = "tasks"
    const val ALARMS = "alarms"
    const val ALARMS_HIGH = "alarms_high" // Full-screen intent
}
```

## Broadcast Receivers

### AlarmReceiver (NEW)

```kotlin
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        // Show full-screen alarm notification
        // Play alarm sound
        // Vibrate if enabled
    }
}
```

### SnoozeReceiver (ENHANCED)

```kotlin
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra("item_id", -1)
        val itemType = intent.getStringExtra("item_type") // "reminder" or "alarm"
        val snoozeDuration = intent.getIntExtra("snooze_duration", 5)
        // Reschedule the item
    }
}
```

## Testing Strategy

### Unit Tests
- TaskManager CRUD operations
- AlarmManager scheduling logic
- RecurrenceScheduler calculations
- CommandParser new patterns
- SnoozeManager rescheduling

### Integration Tests
- Database migrations
- Notification delivery
- Boot receiver rescheduling
- Calendar integration

### UI Tests
- Task list interactions
- Alarm toggle functionality
- Snooze dialog
- Priority/category selection

## Implementation Phases

### Phase 1: Data Layer
1. Create Priority and TaskCategory enums
2. Create TaskEntity and TaskDao
3. Create AlarmEntity and AlarmDao
4. Enhance ReminderEntity with new fields
5. Create database migration

### Phase 2: Domain Layer
1. Create Task and Alarm domain models
2. Implement TaskManager
3. Implement EnhancedAlarmManager
4. Implement RecurrenceScheduler
5. Implement SnoozeManager
6. Enhance CommandParser

### Phase 3: Presentation Layer - Tasks
1. Create TaskListScreen
2. Create TaskDetailScreen
3. Create TaskViewModel
4. Add task-related components

### Phase 4: Presentation Layer - Alarms
1. Create AlarmListScreen
2. Create AlarmDetailScreen
3. Create AlarmTriggerScreen
4. Create AlarmViewModel

### Phase 5: Integration
1. Update calendar to show tasks and alarms
2. Update navigation
3. Implement notification actions
4. Add voice command support

### Phase 6: Polish
1. Add animations
2. Implement backup/restore
3. Add widgets
4. Performance optimization
