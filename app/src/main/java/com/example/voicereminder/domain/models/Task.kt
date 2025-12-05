package com.example.voicereminder.domain.models

import com.example.voicereminder.data.Priority
import com.example.voicereminder.data.RecurrenceRule
import com.example.voicereminder.data.TaskCategory
import com.example.voicereminder.data.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Domain model representing a to-do task
 */
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val dueDate: LocalDateTime? = null,
    val priority: Priority = Priority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val isCompleted: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val recurrenceRule: RecurrenceRule? = null,
    val parentTaskId: Long? = null,
    val notes: String? = null,
    val tags: List<String>? = null
) {
    /**
     * Check if the task is overdue
     */
    fun isOverdue(): Boolean {
        if (isCompleted) return false
        val due = dueDate ?: return false
        return due.isBefore(LocalDateTime.now())
    }
    
    /**
     * Check if the task is due today
     */
    fun isDueToday(): Boolean {
        val due = dueDate ?: return false
        return due.toLocalDate() == LocalDate.now()
    }
    
    /**
     * Check if the task is due this week
     */
    fun isDueThisWeek(): Boolean {
        val due = dueDate ?: return false
        val today = LocalDate.now()
        val endOfWeek = today.plusDays(7 - today.dayOfWeek.value.toLong())
        return !due.toLocalDate().isBefore(today) && !due.toLocalDate().isAfter(endOfWeek)
    }
    
    /**
     * Check if this is a recurring task
     */
    fun isRecurring(): Boolean = recurrenceRule?.isRecurring() == true
    
    /**
     * Get days until due date (negative if overdue)
     */
    fun getDaysUntilDue(): Int? {
        val due = dueDate?.toLocalDate() ?: return null
        val today = LocalDate.now()
        return java.time.temporal.ChronoUnit.DAYS.between(today, due).toInt()
    }
    
    /**
     * Get a human-readable due date description
     */
    fun getDueDateDescription(): String? {
        val due = dueDate ?: return null
        val daysUntil = getDaysUntilDue() ?: return null
        
        return when {
            daysUntil < -1 -> "${-daysUntil} days overdue"
            daysUntil == -1 -> "Yesterday"
            daysUntil == 0 -> "Today"
            daysUntil == 1 -> "Tomorrow"
            daysUntil <= 7 -> "In $daysUntil days"
            else -> due.toLocalDate().toString()
        }
    }
}

/**
 * Extension function to convert TaskEntity to domain Task
 */
fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        },
        priority = priority,
        category = category,
        isCompleted = isCompleted,
        completedAt = completedAt?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        },
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault()),
        recurrenceRule = RecurrenceRule.fromJson(recurrenceRule),
        parentTaskId = parentTaskId,
        notes = notes,
        tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    )
}

/**
 * Extension function to convert domain Task to TaskEntity
 */
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        priority = priority,
        category = category,
        isCompleted = isCompleted,
        completedAt = completedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedAt = updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        recurrenceRule = recurrenceRule?.toJson(),
        parentTaskId = parentTaskId,
        notes = notes,
        tags = tags?.joinToString(",")
    )
}

/**
 * Data class for task progress statistics
 */
data class TaskProgress(
    val total: Int,
    val completed: Int,
    val overdue: Int
) {
    val pending: Int get() = total - completed
    val completionPercentage: Float get() = if (total > 0) completed.toFloat() / total else 0f
}
