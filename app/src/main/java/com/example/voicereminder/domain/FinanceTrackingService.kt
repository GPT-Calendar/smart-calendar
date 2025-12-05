package com.example.voicereminder.domain

import android.content.Context
import com.example.voicereminder.data.FinanceDatabase
import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.SmsSource
import com.example.voicereminder.data.entity.TransactionType
import com.example.voicereminder.data.repository.FinanceRepositoryImpl
import java.util.*

class FinanceTrackingService(context: Context) {
    private val repository = FinanceRepositoryImpl(FinanceDatabase.getDatabase(context))
    
    // SMS Source management
    suspend fun addSmsSource(source: SmsSource) {
        repository.insertSource(source)
    }
    
    suspend fun updateSmsSource(source: SmsSource) {
        repository.updateSource(source)
    }
    
    suspend fun removeSmsSource(source: SmsSource) {
        repository.deleteSource(source)
    }
    
    suspend fun getAllActiveSources(): List<SmsSource> {
        return repository.getAllActiveSources()
    }
    
    /**
     * Initialize default SMS sources for common banks.
     * This is called once on first app install or update.
     * Users can add more sources manually through the UI.
     */
    suspend fun initializeDefaultSources() {
        // Check if any sources already exist
        val existingSources = repository.getAllActiveSources()
        if (existingSources.isNotEmpty()) {
            return // Don't overwrite user's configured sources
        }
        
        // Default bank SMS sources - users can add more via UI
        // These are common bank SMS sender IDs/numbers
        val defaultSources = listOf(
            SmsSource(
                name = "Afghanistan International Bank",
                phoneNumber = "AIB",
                description = "AIB transaction alerts",
                isActive = true
            ),
            SmsSource(
                name = "Azizi Bank",
                phoneNumber = "AZIZI",
                description = "Azizi Bank alerts",
                isActive = true
            ),
            SmsSource(
                name = "Da Afghanistan Bank",
                phoneNumber = "DAB",
                description = "Central bank notifications",
                isActive = true
            )
        )
        
        defaultSources.forEach { source ->
            try {
                repository.insertSource(source)
            } catch (e: Exception) {
                // Ignore duplicates
            }
        }
    }
    
    // Backward compatibility methods (deprecated)
    @Deprecated("Use addSmsSource instead")
    suspend fun addCustomBankPattern(pattern: SmsSource) = addSmsSource(pattern)
    
    @Deprecated("Use updateSmsSource instead")
    suspend fun updateBankPattern(pattern: SmsSource) = updateSmsSource(pattern)
    
    @Deprecated("Use removeSmsSource instead")
    suspend fun removeBankPattern(pattern: SmsSource) = removeSmsSource(pattern)
    
    @Deprecated("Use getAllActiveSources instead")
    suspend fun getAllActivePatterns(): List<SmsSource> = getAllActiveSources()
    
    suspend fun getAllTransactions(): List<FinanceTransaction> {
        return repository.getAllTransactions()
    }
    
    /**
     * Get all transactions as a Flow for reactive UI updates
     */
    fun getAllTransactionsFlow(): kotlinx.coroutines.flow.Flow<List<FinanceTransaction>> {
        return repository.getAllTransactionsFlow()
    }
    
    suspend fun getTransactionsByDateRange(startDate: Date, endDate: Date): List<FinanceTransaction> {
        return repository.getTransactionsByDateRange(startDate, endDate)
    }
    
    suspend fun getTransactionsByType(type: TransactionType): List<FinanceTransaction> {
        return repository.getTransactionsByType(type)
    }
    
    suspend fun getTransactionsByBank(bankName: String): List<FinanceTransaction> {
        return repository.getTransactionsByBank(bankName)
    }
    
    suspend fun getTotalSpending(startDate: Date, endDate: Date): Double {
        return repository.getTotalSpending(startDate, endDate)
    }
    
    suspend fun getTotalIncome(startDate: Date, endDate: Date): Double {
        return repository.getTotalIncome(startDate, endDate)
    }
    
    suspend fun addTransaction(transaction: FinanceTransaction) {
        repository.insertTransaction(transaction)
    }
    
    suspend fun deleteTransaction(transaction: FinanceTransaction) {
        repository.deleteTransaction(transaction)
    }
    
    suspend fun deleteAllTransactions() {
        repository.deleteAllTransactions()
    }
    
    // Calculate financial statistics
    suspend fun getMonthlySummary(month: Int, year: Int): FinanceSummary {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time
        
        val spending = getTotalSpending(startDate, endDate)
        val income = getTotalIncome(startDate, endDate)
        
        return FinanceSummary(
            totalIncome = income,
            totalExpenses = spending,
            netBalance = income - spending,
            startDate = startDate,
            endDate = endDate
        )
    }
    
    // Process SMS content to extract transaction details
    fun parseSmsForTransaction(bankName: String, phoneNumber: String, message: String, timestamp: Date): FinanceTransaction? {
        // This is a simplified version - in a real implementation, you'd match against stored patterns
        val debitPatterns = listOf(
            Regex("""(ETB|RMB|Ksh|GHS|Naira|\$|Rs|Rs\.|\b\d+\s*USD)\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""([0-9,]+\.?\d*)\s*(birr|etb|usd|eur|gbp)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in debitPatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val amountStr = match.groups[2]?.value?.replace(",", "") ?: match.groups[1]?.value?.replace(",", "")
                if (!amountStr.isNullOrEmpty()) {
                    val amount = amountStr.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: continue
                    
                    // Determine transaction type based on keywords
                    val isDebit = listOf("debited", "withdrawn", "deducted", "spent", "used", "paid", "transfer out", "outgoing")
                        .any { message.contains(it, ignoreCase = true) }
                    
                    val isCredit = listOf("credited", "deposited", "received", "added", "income", "salary", "transfer in", "incoming")
                        .any { message.contains(it, ignoreCase = true) }
                    
                    val transactionType = when {
                        isDebit -> TransactionType.DEBIT
                        isCredit -> TransactionType.CREDIT
                        else -> TransactionType.DEBIT // Default to debit if uncertain
                    }
                    
                    return FinanceTransaction(
                        amount = amount,
                        currency = "ETB", // Could be extracted from SMS too
                        description = extractDescription(message),
                        transactionType = transactionType,
                        bankName = bankName,
                        phoneNumber = phoneNumber,
                        smsContent = message,
                        timestamp = timestamp
                    )
                }
            }
        }
        
        return null
    }
    
    private fun extractDescription(message: String): String {
        // Extract meaningful description from the SMS
        val descriptionPatterns = listOf(
            Regex("""(from|to|account|card)\s+([A-Z0-9#\*]+)""", RegexOption.IGNORE_CASE),
            Regex("""(at|on)\s+(.*?)($|on|for|with)""", RegexOption.IGNORE_CASE),
            Regex("""(for|purpose|reason)\s+(.*?)($|\.|,|\s+transaction)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in descriptionPatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val extracted = match.groups[2]?.value?.trim() ?: continue
                if (extracted.isNotEmpty() && extracted.length < 100) { // Reasonable length limit
                    return extracted
                }
            }
        }
        
        // Default to first 50 characters if no specific description found
        return message.take(50) + if (message.length > 50) "..." else ""
    }
}

data class FinanceSummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val netBalance: Double, // Income - Expenses
    val startDate: Date,
    val endDate: Date
)