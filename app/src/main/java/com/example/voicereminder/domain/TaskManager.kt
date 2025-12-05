package com.example.voicereminder.domain

import android.content.Context
import android.util.Log
import com.example.voicereminder.data.Priority
import com.example.voicereminder.data.RecurrenceRule
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.TaskCategory
import com.example.voicereminder.data.entity.TaskEntity
import com.example.voicereminder.domain.models.Task
import com.example.voicereminder.domain.models.TaskProgress
import com.example.voicereminder.domain.models.toDomain
import com.example.voicereminder.domain.models.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Result class for task operations
 */
sealed class TaskResult {
    data class Success(val taskId: Long) : TaskResult()
    data class Error(val message: String) : TaskResult()
}

/**
 * Core business logic for managing to-do tasks
 */
class TaskManager(
    private val database: ReminderDatabase,
    private val context: Context
) {
    companion object {
        private const val TAG = "TaskManager"
        
        @Volatile
        private var INSTANCE: TaskManager? = null
        
        fun getInstance(context: Context): TaskManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TaskManager(
                    ReminderDatabase.getDatabase(context),
                    context.applicationContext
                ).also { INSTANCE = it }
            }
        }
    }
    
    private val taskDao = database.taskDao()
    
    /**
     * Create a new task
     */
    suspend fun createTask(
        title: String,
        description: String? = null,
        dueDate: LocalDateTime? = null,
        priority: Priority = Priority.MEDIUM,
        category: TaskCategory = TaskCategory.PERSONAL,
        recurrenceRule: RecurrenceRule? = null,
        notes: String? = null
    ): TaskResult {
        return withContext(Dispatchers.IO) {
            try {
                if (title.isBlank()) {
                    return@withContext TaskResult.Error("Task title cannot be empty")
                }
                
                val now = System.currentTimeMillis()
                val taskEntity = TaskEntity(
                    title = title.trim(),
                    description = description?.trim(),
                    dueDate = dueDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                    priority = priority,
                    category = category,
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now,
                    recurrenceRule = recurrenceRule?.toJson(),
                    notes = notes?.trim()
                )
                
                val taskId = taskDao.insert(taskEntity)
                Log.d(TAG, "Task created with ID: $taskId")
                
                // Schedule notification if due date is set
                if (dueDate != null) {
                    scheduleTaskNotification(taskId, title, dueDate)
                }
                
                return@withContext TaskResult.Success(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating task", e)
                TaskResult.Error("Failed to create task: ${e.message}")
            }
        }
    }
    
    /**
     * Mark a task as completed
     */
    suspend fun completeTask(taskId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                taskDao.markAsCompleted(taskId, now, now)
                Log.d(TAG, "Task $taskId marked as completed")
                
                // Check if it's a recurring task and schedule next occurrence
                val task = taskDao.getTaskById(taskId)
                if (task?.recurrenceRule != null) {
                    scheduleNextRecurrence(task)
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task", e)
                Unit
            }
        }
    }
    
    /**
     * Mark a task as incomplete (uncomplete)
     */
    suspend fun uncompleteTask(taskId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                taskDao.markAsIncomplete(taskId, now)
                Log.d(TAG, "Task $taskId marked as incomplete")
            } catch (e: Exception) {
                Log.e(TAG, "Error uncompleting task", e)
            }
        }
    }
    
    /**
     * Delete a task
     */
    suspend fun deleteTask(taskId: Long) {
        withContext(Dispatchers.IO) {
            try {
                taskDao.deleteById(taskId)
                Log.d(TAG, "Task $taskId deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task", e)
            }
        }
    }
    
    /**
     * Update an existing task
     */
    suspend fun updateTask(task: Task): TaskResult {
        return withContext(Dispatchers.IO) {
            try {
                if (task.title.isBlank()) {
                    return@withContext TaskResult.Error("Task title cannot be empty")
                }
                
                val updatedEntity = task.toEntity().copy(
                    updatedAt = System.currentTimeMillis()
                )
                taskDao.update(updatedEntity)
                Log.d(TAG, "Task ${task.id} updated")
                TaskResult.Success(task.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating task", e)
                TaskResult.Error("Failed to update task: ${e.message}")
            }
        }
    }
    
    /**
     * Get a task by ID
     */
    suspend fun getTaskById(taskId: Long): Task? {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getTaskById(taskId)?.toDomain()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting task", e)
                null
            }
        }
    }
    
    /**
     * Get all tasks as a Flow
     */
    fun getAllTasksFlow(): Flow<List<Task>> {
        return taskDao.getAllTasksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get incomplete tasks as a Flow
     */
    fun getIncompleteTasksFlow(): Flow<List<Task>> {
        return taskDao.getIncompleteTasksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get completed tasks as a Flow
     */
    fun getCompletedTasksFlow(): Flow<List<Task>> {
        return taskDao.getCompletedTasksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get tasks by category as a Flow
     */
    fun getTasksByCategoryFlow(category: TaskCategory): Flow<List<Task>> {
        return taskDao.getTasksByCategoryFlow(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get tasks by priority as a Flow
     */
    fun getTasksByPriorityFlow(priority: Priority): Flow<List<Task>> {
        return taskDao.getTasksByPriorityFlow(priority).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get tasks due on a specific date as a Flow
     */
    fun getTasksDueOnDateFlow(date: LocalDate): Flow<List<Task>> {
        val startOfDay = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return taskDao.getTasksDueOnDateFlow(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get overdue tasks as a Flow
     */
    fun getOverdueTasksFlow(): Flow<List<Task>> {
        val now = System.currentTimeMillis()
        return taskDao.getOverdueTasksFlow(now).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Search tasks by title or description
     */
    fun searchTasksFlow(query: String): Flow<List<Task>> {
        return taskDao.searchTasksFlow(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * Get task progress statistics
     */
    suspend fun getTaskProgress(): TaskProgress {
        return withContext(Dispatchers.IO) {
            try {
                val total = taskDao.getTotalCount()
                val completed = taskDao.getCompletedCount()
                val overdue = taskDao.getOverdueCount(System.currentTimeMillis())
                TaskProgress(total, completed, overdue)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting task progress", e)
                TaskProgress(0, 0, 0)
            }
        }
    }
    
    /**
     * Schedule notification for a task with due date
     */
    private fun scheduleTaskNotification(taskId: Long, title: String, dueDate: LocalDateTime) {
        // TODO: Implement task notification scheduling
        Log.d(TAG, "Task notification scheduled for $taskId at $dueDate")
    }
    
    /**
     * Schedule next occurrence for a recurring task
     */
    private suspend fun scheduleNextRecurrence(task: TaskEntity) {
        val rule = RecurrenceRule.fromJson(task.recurrenceRule) ?: return
        if (!rule.isRecurring()) return
        
        // TODO: Implement recurrence scheduling using RecurrenceScheduler
        Log.d(TAG, "Scheduling next recurrence for task ${task.id}")
    }
}
