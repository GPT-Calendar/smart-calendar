package com.example.voicereminder.domain.repository

import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.SmsSource
import java.util.Date

interface FinanceRepository {
    suspend fun getAllTransactions(): List<FinanceTransaction>
    suspend fun getTransactionsByDateRange(startDate: Date, endDate: Date): List<FinanceTransaction>
    suspend fun getTransactionsByType(type: com.example.voicereminder.data.entity.TransactionType): List<FinanceTransaction>
    suspend fun getTransactionsByBank(bankName: String): List<FinanceTransaction>
    suspend fun getTotalSpending(startDate: Date, endDate: Date): Double
    suspend fun getTotalIncome(startDate: Date, endDate: Date): Double
    suspend fun insertTransaction(transaction: FinanceTransaction)
    suspend fun insertTransactions(transactions: List<FinanceTransaction>)
    suspend fun deleteTransaction(transaction: FinanceTransaction)
    suspend fun deleteAllTransactions()

    // SMS Source operations (for auto-tracking finance from SMS)
    suspend fun getAllActiveSources(): List<SmsSource>
    suspend fun getSourceByPhoneNumber(phoneNumber: String): SmsSource?
    suspend fun insertSource(source: SmsSource)
    suspend fun updateSource(source: SmsSource)
    suspend fun deleteSource(source: SmsSource)
}