package com.example.voicereminder.data.dao

import androidx.room.*
import com.example.voicereminder.data.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Savings Goal operations
 */
@Dao
interface SavingsGoalDao {
    
    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<SavingsGoalEntity>>
    
    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getActiveGoalsOnce(): List<SavingsGoalEntity>
    
    @Query("SELECT * FROM savings_goals WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedGoals(): Flow<List<SavingsGoalEntity>>
    
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>
    
    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): SavingsGoalEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long
    
    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)
    
    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)
    
    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
    
    @Query("UPDATE savings_goals SET currentAmount = :amount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateGoalAmount(id: Long, amount: Double, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE savings_goals SET isCompleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markGoalCompleted(id: Long, updatedAt: Long = System.currentTimeMillis())
}
