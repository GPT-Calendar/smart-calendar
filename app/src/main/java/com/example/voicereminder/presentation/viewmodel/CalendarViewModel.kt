package com.example.voicereminder.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.TaskManager
import com.example.voicereminder.domain.models.Alarm
import com.example.voicereminder.domain.models.Task
import com.example.voicereminder.presentation.calendar.ReminderFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Represents an item that can appear on the calendar
 */
sealed class CalendarItem {
    abstract val id: Long
    abstract val title: String
    abstract val time: java.time.LocalDateTime?
    
    data class ReminderItem(val reminder: com.example.voicereminder.domain.models.Reminder) : CalendarItem() {
        override val id: Long = reminder.id
        override val title: String = reminder.message
        override val time: java.time.LocalDateTime? = reminder.scheduledTime
    }
    
    data class TaskItem(val task: Task) : CalendarItem() {
        override val id: Long = task.id
        override val title: String = task.title
        override val time: java.time.LocalDateTime? = task.dueDate
    }
    
    data class AlarmItem(val alarm: Alarm) : CalendarItem() {
        override val id: Long = alarm.id
        override val title: String = alarm.label
        override val time: java.time.LocalDateTime? = alarm.nextTrigger
    }
}

class CalendarViewModel(
    private val reminderManager: ReminderManager,
    private val taskManager: TaskManager? = null,
    private val alarmManager: EnhancedAlarmManager? = null
) : ViewModel() {
    
    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now())
    val selectedDate: StateFlow<java.time.LocalDate> = _selectedDate.asStateFlow()
    
    private val _allReminders = MutableStateFlow<List<com.example.voicereminder.domain.models.Reminder>>(emptyList())
    val allReminders: StateFlow<List<com.example.voicereminder.domain.models.Reminder>> = _allReminders.asStateFlow()
    
    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks.asStateFlow()
    
    private val _allAlarms = MutableStateFlow<List<Alarm>>(emptyList())
    val allAlarms: StateFlow<List<Alarm>> = _allAlarms.asStateFlow()
    
    private val _selectedDateReminders = MutableStateFlow<List<com.example.voicereminder.domain.models.Reminder>>(emptyList())
    val selectedDateReminders: StateFlow<List<com.example.voicereminder.domain.models.Reminder>> = _selectedDateReminders.asStateFlow()
    
    private val _selectedDateItems = MutableStateFlow<List<CalendarItem>>(emptyList())
    val selectedDateItems: StateFlow<List<CalendarItem>> = _selectedDateItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(com.example.voicereminder.presentation.calendar.ReminderFilter.ALL)
    val currentFilter: StateFlow<com.example.voicereminder.presentation.calendar.ReminderFilter> = _currentFilter.asStateFlow()
    
    // Track if data has been loaded to avoid repeated loading
    private var dataLoaded = false
    
    init {
        // REMOVED: loadReminders() call on init
        // Data is now loaded lazily when screen becomes visible
        // This prevents unnecessary database operations on app startup
    }
    
    /**
     * Called when the screen becomes visible
     * Loads data lazily to improve app startup performance
     */
    fun onScreenVisible() {
        if (!dataLoaded) {
            loadReminders()
        }
    }
    
    fun selectDate(date: java.time.LocalDate) {
        _selectedDate.value = date
        updateSelectedDateReminders()
        updateSelectedDateItems()
    }
    
    private fun updateSelectedDateReminders() {
        val date = _selectedDate.value
        val filteredReminders = _allReminders.value.filter { reminder ->
            reminder.scheduledTime?.toLocalDate() == date
        }
        _selectedDateReminders.value = filteredReminders
    }
    
    fun loadReminders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reminders = reminderManager.getAllReminders()
                _allReminders.value = reminders
                
                // Load tasks if TaskManager is available
                taskManager?.let { tm ->
                    val tasks = tm.getAllTasksFlow().first()
                    _allTasks.value = tasks
                }
                
                // Load alarms if AlarmManager is available
                alarmManager?.let { am ->
                    val alarms = am.getAllAlarmsFlow().first()
                    _allAlarms.value = alarms
                }
                
                updateSelectedDateReminders()
                updateSelectedDateItems()
                dataLoaded = true
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun updateSelectedDateItems() {
        val date = _selectedDate.value
        val items = mutableListOf<CalendarItem>()
        
        // Add reminders for selected date
        _allReminders.value
            .filter { it.scheduledTime?.toLocalDate() == date }
            .forEach { items.add(CalendarItem.ReminderItem(it)) }
        
        // Add tasks with due date on selected date
        _allTasks.value
            .filter { it.dueDate?.toLocalDate() == date }
            .forEach { items.add(CalendarItem.TaskItem(it)) }
        
        // Add alarms that trigger on selected date
        _allAlarms.value
            .filter { alarm ->
                alarm.nextTrigger?.toLocalDate() == date ||
                (alarm.isRepeating() && alarm.repeatDays.contains(date.dayOfWeek.value))
            }
            .forEach { items.add(CalendarItem.AlarmItem(it)) }
        
        // Sort by time
        _selectedDateItems.value = items.sortedBy { it.time }
    }

    fun deleteReminder(id: Long): Boolean {
        // Launch the async operation in the background
        viewModelScope.launch {
            try {
                // Call the suspend function from the reminder manager
                reminderManager.deleteReminder(id)
                // Refresh the list after successful deletion
                loadReminders()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
        // Return true immediately - the actual result will be reflected in the UI state
        return true
    }

    fun applyFilter(filter: com.example.voicereminder.presentation.calendar.ReminderFilter) {
        _currentFilter.value = filter
    }
    
    fun getRemindersForMonth(month: java.time.YearMonth): Map<java.time.LocalDate, Int> {
        return _allReminders.value
            .filter { reminder ->
                reminder.scheduledTime?.let {
                    it.year == month.year && it.monthValue == month.monthValue
                } ?: false
            }
            .groupBy { it.scheduledTime!!.toLocalDate() }
            .mapValues { it.value.size }
    }
    
    /**
     * Get all calendar items (reminders, tasks, alarms) for a month
     * Returns a map of date to count of items
     */
    fun getItemsForMonth(month: java.time.YearMonth): Map<java.time.LocalDate, ItemCount> {
        val counts = mutableMapOf<java.time.LocalDate, ItemCount>()
        
        // Count reminders
        _allReminders.value
            .filter { it.scheduledTime?.let { t -> t.year == month.year && t.monthValue == month.monthValue } ?: false }
            .forEach { reminder ->
                val date = reminder.scheduledTime!!.toLocalDate()
                val current = counts.getOrDefault(date, ItemCount())
                counts[date] = current.copy(reminders = current.reminders + 1)
            }
        
        // Count tasks
        _allTasks.value
            .filter { it.dueDate?.let { t -> t.year == month.year && t.monthValue == month.monthValue } ?: false }
            .forEach { task ->
                val date = task.dueDate!!.toLocalDate()
                val current = counts.getOrDefault(date, ItemCount())
                counts[date] = current.copy(tasks = current.tasks + 1)
            }
        
        // Count alarms (for repeating alarms, count each day they occur)
        _allAlarms.value.forEach { alarm ->
            if (alarm.isRepeating()) {
                // For repeating alarms, mark each day in the month
                var date = month.atDay(1)
                while (date.monthValue == month.monthValue) {
                    if (alarm.repeatDays.contains(date.dayOfWeek.value)) {
                        val current = counts.getOrDefault(date, ItemCount())
                        counts[date] = current.copy(alarms = current.alarms + 1)
                    }
                    date = date.plusDays(1)
                }
            } else {
                // One-time alarm
                alarm.nextTrigger?.let { trigger ->
                    if (trigger.year == month.year && trigger.monthValue == month.monthValue) {
                        val date = trigger.toLocalDate()
                        val current = counts.getOrDefault(date, ItemCount())
                        counts[date] = current.copy(alarms = current.alarms + 1)
                    }
                }
            }
        }
        
        return counts
    }
    
    /**
     * Complete a task from the calendar view
     */
    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            taskManager?.completeTask(taskId)
            loadReminders() // Refresh data
        }
    }
    
    /**
     * Toggle alarm from the calendar view
     */
    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            alarmManager?.toggleAlarm(alarmId, enabled)
            loadReminders() // Refresh data
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Data class to hold counts of different item types for a date
 */
data class ItemCount(
    val reminders: Int = 0,
    val tasks: Int = 0,
    val alarms: Int = 0
) {
    val total: Int get() = reminders + tasks + alarms
}

class CalendarViewModelFactory(
    private val reminderManager: ReminderManager,
    private val taskManager: TaskManager? = null,
    private val alarmManager: EnhancedAlarmManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(reminderManager, taskManager, alarmManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}