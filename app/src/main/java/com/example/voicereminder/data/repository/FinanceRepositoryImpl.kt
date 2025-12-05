package com.example.voicereminder.data.repository

import com.example.voicereminder.data.FinanceDatabase
import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.SmsSource
import com.example.voicereminder.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date

class FinanceRepositoryImpl(
    private val database: FinanceDatabase
) : FinanceRepository {
    override suspend fun getAllTransactions(): List<FinanceTransaction> {
        return database.financeTransactionDao().getAllTransactions()
    }
    
    /**
     * Get all transactions as a Flow for reactive updates
     */
    fun getAllTransactionsFlow(): Flow<List<FinanceTransaction>> {
        return database.financeTransactionDao().getAllTransactionsFlow()
    }

    override suspend fun getTransactionsByDateRange(startDate: Date, endDate: Date): List<FinanceTransaction> {
        return database.financeTransactionDao().getTransactionsByDateRange(startDate, endDate)
    }

    override suspend fun getTransactionsByType(type: com.example.voicereminder.data.entity.TransactionType): List<FinanceTransaction> {
        return database.financeTransactionDao().getTransactionsByType(type)
    }

    override suspend fun getTransactionsByBank(bankName: String): List<FinanceTransaction> {
        return database.financeTransactionDao().getTransactionsByBank(bankName)
    }

    override suspend fun getTotalSpending(startDate: Date, endDate: Date): Double {
        return database.financeTransactionDao().getTotalSpending(startDate, endDate) ?: 0.0
    }

    override suspend fun getTotalIncome(startDate: Date, endDate: Date): Double {
        return database.financeTransactionDao().getTotalIncome(startDate, endDate) ?: 0.0
    }

    override suspend fun insertTransaction(transaction: FinanceTransaction) {
        database.financeTransactionDao().insertTransaction(transaction)
    }

    override suspend fun insertTransactions(transactions: List<FinanceTransaction>) {
        database.financeTransactionDao().insertTransactions(transactions)
    }

    override suspend fun deleteTransaction(transaction: FinanceTransaction) {
        database.financeTransactionDao().deleteTransaction(transaction)
    }

    override suspend fun deleteAllTransactions() {
        database.financeTransactionDao().deleteAllTransactions()
    }

    // SMS Source operations
    override suspend fun getAllActiveSources(): List<SmsSource> {
        return database.smsSourceDao().getAllActiveSources()
    }

    override suspend fun getSourceByPhoneNumber(phoneNumber: String): SmsSource? {
        return database.smsSourceDao().getSourceByPhoneNumber(phoneNumber)
    }

    override suspend fun insertSource(source: SmsSource) {
        database.smsSourceDao().insertSource(source)
    }

    override suspend fun updateSource(source: SmsSource) {
        database.smsSourceDao().updateSource(source)
    }

    override suspend fun deleteSource(source: SmsSource) {
        database.smsSourceDao().deleteSource(source)
    }
}