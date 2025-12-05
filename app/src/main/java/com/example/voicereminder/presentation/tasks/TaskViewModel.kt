package com.example.voicereminder.presentation.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.Priority
import com.example.voicereminder.data.RecurrenceRule
import com.example.voicereminder.data.TaskCategory
import com.example.voicereminder.domain.TaskManager
import com.example.voicereminder.domain.TaskResult
import com.example.voicereminder.domain.models.Task
import com.example.voicereminder.domain.models.TaskProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Filter options for task list
 */
enum class TaskFilter {
    ALL,
    INCOMPLETE,
    COMPLETED,
    OVERDUE,
    TODAY
}

/**
 * Sort options for task list
 */
enum class TaskSort {
    DUE_DATE,
    PRIORITY,
    CREATED_DATE,
    ALPHABETICAL
}

/**
 * UI state for task list screen
 */
data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val progress: TaskProgress = TaskProgress(0, 0, 0),
    val filter: TaskFilter = TaskFilter.INCOMPLETE,
    val sort: TaskSort = TaskSort.DUE_DATE,
    val selectedCategory: TaskCategory? = null,
    val selectedPriority: Priority? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI state for task detail/edit screen
 */
data class TaskDetailUiState(
    val task: Task? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * ViewModel for task management screens
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    
    private val taskManager = TaskManager.getInstance(application)
    
    // Task list state
    private val _listUiState = MutableStateFlow(TaskListUiState())
    val listUiState: StateFlow<TaskListUiState> = _listUiState.asStateFlow()
    
    // Task detail state
    private val _detailUiState = MutableStateFlow(TaskDetailUiState())
    val detailUiState: StateFlow<TaskDetailUiState> = _detailUiState.asStateFlow()
    
    // All tasks flow
    private val allTasksFlow = taskManager.getAllTasksFlow()
    
    init {
        // Observe tasks and update UI state
        viewModelScope.launch {
            allTasksFlow.collect { tasks ->
                updateTaskList(tasks)
            }
        }
        
        // Load initial progress
        loadProgress()
    }
    
    /**
     * Update task list based on current filters
     */
    private fun updateTaskList(tasks: List<Task>) {
        val state = _listUiState.value
        
        // Apply filters
        var filteredTasks = when (state.filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.INCOMPLETE -> tasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
            TaskFilter.OVERDUE -> tasks.filter { it.isOverdue() }
            TaskFilter.TODAY -> tasks.filter { it.isDueToday() }
        }
        
        // Apply category filter
        state.selectedCategory?.let { category ->
            filteredTasks = filteredTasks.filter { it.category == category }
        }
        
        // Apply priority filter
        state.selectedPriority?.let { priority ->
            filteredTasks = filteredTasks.filter { it.priority == priority }
        }
        
        // Apply search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filteredTasks = filteredTasks.filter {
                it.title.lowercase().contains(query) ||
                it.description?.lowercase()?.contains(query) == true
            }
        }
        
        // Apply sorting
        filteredTasks = when (state.sort) {
            TaskSort.DUE_DATE -> filteredTasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy(nullsLast()) { it.dueDate }
            )
            TaskSort.PRIORITY -> filteredTasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.priority.ordinal }
            )
            TaskSort.CREATED_DATE -> filteredTasks.sortedByDescending { it.createdAt }
            TaskSort.ALPHABETICAL -> filteredTasks.sortedBy { it.title.lowercase() }
        }
        
        _listUiState.update { it.copy(tasks = filteredTasks, isLoading = false) }
    }
    
    /**
     * Load task progress statistics
     */
    private fun loadProgress() {
        viewModelScope.launch {
            val progress = taskManager.getTaskProgress()
            _listUiState.update { it.copy(progress = progress) }
        }
    }
    
    /**
     * Set the filter for task list
     */
    fun setFilter(filter: TaskFilter) {
        _listUiState.update { it.copy(filter = filter) }
        viewModelScope.launch {
            allTasksFlow.first().let { updateTaskList(it) }
        }
    }
    
    /**
     * Set the sort order for task list
     */
    fun setSort(sort: TaskSort) {
        _listUiState.update { it.copy(sort = sort) }
        viewModelScope.launch {
            allTasksFlow.first().let { updateTaskList(it) }
        }
    }
    
    /**
     * Set category filter
     */
    fun setCategoryFilter(category: TaskCategory?) {
        _listUiState.update { it.copy(selectedCategory = category) }
        viewModelScope.launch {
            allTasksFlow.first().let { updateTaskList(it) }
        }
    }
    
    /**
     * Set priority filter
     */
    fun setPriorityFilter(priority: Priority?) {
        _listUiState.update { it.copy(selectedPriority = priority) }
        viewModelScope.launch {
            allTasksFlow.first().let { updateTaskList(it) }
        }
    }
    
    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        _listUiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            allTasksFlow.first().let { updateTaskList(it) }
        }
    }
    
    /**
     * Create a new task
     */
    fun createTask(
        title: String,
        description: String? = null,
        dueDate: LocalDateTime? = null,
        priority: Priority = Priority.MEDIUM,
        category: TaskCategory = TaskCategory.PERSONAL,
        recurrenceRule: RecurrenceRule? = null
    ) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isSaving = true, error = null) }
            
            when (val result = taskManager.createTask(
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority,
                category = category,
                recurrenceRule = recurrenceRule
            )) {
                is TaskResult.Success -> {
                    _detailUiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    loadProgress()
                }
                is TaskResult.Error -> {
                    _detailUiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }
    
    /**
     * Toggle task completion status
     */
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (task.isCompleted) {
                taskManager.uncompleteTask(task.id)
            } else {
                taskManager.completeTask(task.id)
            }
            loadProgress()
        }
    }
    
    /**
     * Complete a task
     */
    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            taskManager.completeTask(taskId)
            loadProgress()
        }
    }
    
    /**
     * Uncomplete a task
     */
    fun uncompleteTask(taskId: Long) {
        viewModelScope.launch {
            taskManager.uncompleteTask(taskId)
            loadProgress()
        }
    }
    
    /**
     * Delete a task
     */
    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskManager.deleteTask(taskId)
            loadProgress()
        }
    }
    
    /**
     * Update an existing task
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isSaving = true, error = null) }
            
            when (val result = taskManager.updateTask(task)) {
                is TaskResult.Success -> {
                    _detailUiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    loadProgress()
                }
                is TaskResult.Error -> {
                    _detailUiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }
    
    /**
     * Load a task for editing
     */
    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isEditing = true) }
            val task = taskManager.getTaskById(taskId)
            _detailUiState.update { it.copy(task = task) }
        }
    }
    
    /**
     * Clear task detail state
     */
    fun clearDetailState() {
        _detailUiState.value = TaskDetailUiState()
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _listUiState.update { it.copy(error = null) }
        _detailUiState.update { it.copy(error = null) }
    }
    
    /**
     * Find task by title (for voice commands)
     */
    suspend fun findTaskByTitle(title: String): Task? {
        val tasks = allTasksFlow.first()
        return tasks.find { 
            it.title.lowercase().contains(title.lowercase()) && !it.isCompleted
        }
    }
    
    /**
     * Complete task by title (for voice commands)
     */
    fun completeTaskByTitle(title: String) {
        viewModelScope.launch {
            val task = findTaskByTitle(title)
            if (task != null) {
                taskManager.completeTask(task.id)
                loadProgress()
            }
        }
    }
}
