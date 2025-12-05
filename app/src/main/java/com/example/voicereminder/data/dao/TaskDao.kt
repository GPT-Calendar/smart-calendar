package com.example.voicereminder.data.dao

import androidx.room.*
import com.example.voicereminder.data.Priority
import com.example.voicereminder.data.TaskCategory
import com.example.voicereminder.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for task database operations
 */
@Dao
interface TaskDao {
    
    /**
     * Insert a new task into the database
     * @return The ID of the inserted task
     */
    @Insert
    suspend fun insert(task: TaskEntity): Long
    
    /**
     * Update an existing task
     */
    @Update
    suspend fun update(task: TaskEntity)
    
    /**
     * Delete a task from the database
     */
    @Delete
    suspend fun delete(task: TaskEntity)
    
    /**
     * Delete a task by ID
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
    
    /**
     * Get a specific task by its ID
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?
    
    /**
     * Get all tasks ordered by due date (nulls last), then by priority
     */
    @Query("""
        SELECT * FROM tasks 
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            CASE priority 
                WHEN 'HIGH' THEN 0 
                WHEN 'MEDIUM' THEN 1 
                WHEN 'LOW' THEN 2 
            END
    """)
    suspend fun getAllTasks(): List<TaskEntity>
    
    /**
     * Get all tasks as a Flow for reactive updates
     */
    @Query("""
        SELECT * FROM tasks 
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            CASE priority 
                WHEN 'HIGH' THEN 0 
                WHEN 'MEDIUM' THEN 1 
                WHEN 'LOW' THEN 2 
            END
    """)
    fun getAllTasksFlow(): Flow<List<TaskEntity>>
    
    /**
     * Get incomplete tasks only
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            CASE priority 
                WHEN 'HIGH' THEN 0 
                WHEN 'MEDIUM' THEN 1 
                WHEN 'LOW' THEN 2 
            END
    """)
    fun getIncompleteTasksFlow(): Flow<List<TaskEntity>>
    
    /**
     * Get completed tasks only
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasksFlow(): Flow<List<TaskEntity>>
    
    /**
     * Get tasks by category
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE category = :category
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun getTasksByCategoryFlow(category: TaskCategory): Flow<List<TaskEntity>>
    
    /**
     * Get tasks by priority
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE priority = :priority
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun getTasksByPriorityFlow(priority: Priority): Flow<List<TaskEntity>>
    
    /**
     * Get tasks due on a specific date (start and end of day timestamps)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE dueDate >= :startOfDay AND dueDate < :endOfDay
        ORDER BY 
            isCompleted ASC,
            dueDate ASC,
            CASE priority 
                WHEN 'HIGH' THEN 0 
                WHEN 'MEDIUM' THEN 1 
                WHEN 'LOW' THEN 2 
            END
    """)
    fun getTasksDueOnDateFlow(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>
    
    /**
     * Get overdue tasks (due date in the past and not completed)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE dueDate < :currentTime AND isCompleted = 0
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasksFlow(currentTime: Long): Flow<List<TaskEntity>>
    
    /**
     * Get tasks due today or overdue
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE (dueDate < :endOfToday OR dueDate IS NULL) AND isCompleted = 0
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun getTodayAndOverdueTasksFlow(endOfToday: Long): Flow<List<TaskEntity>>
    
    /**
     * Get count of all tasks
     */
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalCount(): Int
    
    /**
     * Get count of completed tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int
    
    /**
     * Get count of overdue tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE dueDate < :currentTime AND isCompleted = 0")
    suspend fun getOverdueCount(currentTime: Long): Int
    
    /**
     * Mark a task as completed
     */
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun markAsCompleted(taskId: Long, completedAt: Long, updatedAt: Long)
    
    /**
     * Mark a task as incomplete
     */
    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun markAsIncomplete(taskId: Long, updatedAt: Long)
    
    /**
     * Search tasks by title or description
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun searchTasksFlow(query: String): Flow<List<TaskEntity>>
    
    /**
     * Get tasks with recurrence rules (for rescheduling)
     */
    @Query("SELECT * FROM tasks WHERE recurrenceRule IS NOT NULL AND isCompleted = 1")
    suspend fun getCompletedRecurringTasks(): List<TaskEntity>
}
