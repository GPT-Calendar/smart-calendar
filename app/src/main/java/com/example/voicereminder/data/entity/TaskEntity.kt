package com.example.voicereminder.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.voicereminder.data.Priority
import com.example.voicereminder.data.TaskCategory

/**
 * Room entity representing a to-do task in the database
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isCompleted"]),
        Index(value = ["dueDate"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    
    val description: String? = null,
    
    val dueDate: Long? = null, // Unix timestamp in milliseconds
    
    val priority: Priority = Priority.MEDIUM,
    
    val category: TaskCategory = TaskCategory.PERSONAL,
    
    val isCompleted: Boolean = false,
    
    val completedAt: Long? = null, // Unix timestamp when completed
    
    val createdAt: Long, // Unix timestamp in milliseconds
    
    val updatedAt: Long, // Unix timestamp in milliseconds
    
    val recurrenceRule: String? = null, // JSON string for RecurrenceRule
    
    val parentTaskId: Long? = null, // For subtasks (future feature)
    
    val notes: String? = null, // Additional notes
    
    val tags: String? = null // Comma-separated tags (future feature)
)
