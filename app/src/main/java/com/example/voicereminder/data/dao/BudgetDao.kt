package com.example.voicereminder.data.dao

import androidx.room.*
import com.example.voicereminder.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Budget operations
 */
@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY category ASC")
    fun getAllActiveBudgets(): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets WHERE month = :month AND isActive = 1 ORDER BY category ASC")
    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets WHERE month = :month AND isActive = 1 ORDER BY category ASC")
    suspend fun getBudgetsForMonthOnce(month: String): List<BudgetEntity>
    
    @Query("SELECT * FROM budgets WHERE category = :category AND month = :month AND isActive = 1 LIMIT 1")
    suspend fun getBudgetForCategoryAndMonth(category: String, month: String): BudgetEntity?
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): BudgetEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long
    
    @Update
    suspend fun updateBudget(budget: BudgetEntity)
    
    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
    
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)
    
    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBudget(id: Long)
}
