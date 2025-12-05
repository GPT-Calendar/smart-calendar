package com.example.voicereminder.data.dao

import androidx.room.*
import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.SmsSource
import com.example.voicereminder.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface FinanceTransactionDao {
    @Query("SELECT * FROM finance_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<FinanceTransaction>
    
    @Query("SELECT * FROM finance_transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<FinanceTransaction>>

    @Query("SELECT * FROM finance_transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getTransactionsByDateRange(startDate: Date, endDate: Date): List<FinanceTransaction>
    
    @Query("SELECT * FROM finance_transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getTransactionsByDateRangeFlow(startDate: Date, endDate: Date): Flow<List<FinanceTransaction>>

    @Query("SELECT * FROM finance_transactions WHERE transactionType = :type ORDER BY timestamp DESC")
    suspend fun getTransactionsByType(type: com.example.voicereminder.data.entity.TransactionType): List<FinanceTransaction>
    
    @Query("SELECT * FROM finance_transactions WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getTransactionsByCategory(category: String): List<FinanceTransaction>
    
    @Query("SELECT * FROM finance_transactions WHERE category = :category AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getTransactionsByCategoryAndDateRange(category: String, startDate: Date, endDate: Date): List<FinanceTransaction>

    @Query("SELECT * FROM finance_transactions WHERE bankName = :bankName ORDER BY timestamp DESC")
    suspend fun getTransactionsByBank(bankName: String): List<FinanceTransaction>
    
    @Query("SELECT * FROM finance_transactions WHERE isManual = 1 ORDER BY timestamp DESC")
    suspend fun getManualTransactions(): List<FinanceTransaction>
    
    @Query("SELECT * FROM finance_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): FinanceTransaction?
    
    @Query("SELECT * FROM finance_transactions WHERE description LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchTransactions(query: String): List<FinanceTransaction>

    @Query("SELECT SUM(amount) FROM finance_transactions WHERE transactionType = 'DEBIT' AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalSpending(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(amount) FROM finance_transactions WHERE transactionType = 'CREDIT' AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncome(startDate: Date, endDate: Date): Double?
    
    @Query("SELECT SUM(amount) FROM finance_transactions WHERE category = :category AND transactionType = 'DEBIT' AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalSpendingByCategory(category: String, startDate: Date, endDate: Date): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinanceTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<FinanceTransaction>)
    
    @Update
    suspend fun updateTransaction(transaction: FinanceTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: FinanceTransaction)
    
    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM finance_transactions")
    suspend fun deleteAllTransactions()
}

/**
 * DAO for managing SMS sources used for automatic finance tracking
 */
@Dao
interface SmsSourceDao {
    @Query("SELECT * FROM sms_sources WHERE isActive = 1")
    suspend fun getAllActiveSources(): List<SmsSource>
    
    @Query("SELECT * FROM sms_sources")
    suspend fun getAllSources(): List<SmsSource>

    @Query("SELECT * FROM sms_sources WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getSourceByPhoneNumber(phoneNumber: String): SmsSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SmsSource)

    @Update
    suspend fun updateSource(source: SmsSource)

    @Delete
    suspend fun deleteSource(source: SmsSource)
}

// Keep old interface name as typealias for backward compatibility
@Deprecated("Use SmsSourceDao instead")
typealias BankSmsPatternDao = SmsSourceDao