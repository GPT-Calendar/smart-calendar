package com.example.voicereminder.data

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.voicereminder.data.entity.TransactionType as EntityTransactionType
import com.example.voicereminder.domain.FinanceTrackingService
import com.example.voicereminder.domain.models.*

class FinanceRepository(private val context: Context) {
    // Lazy initialization - only create service when actually needed
    private val financeTrackingService by lazy { FinanceTrackingService(context) }

    companion object {
        private const val TAG = "FinanceRepository"
    }

    suspend fun getFinanceData(): FinanceData {
        Log.d(TAG, "Getting finance data from database")

        try {
            // REMOVED: initializeDefaultPatterns() call
            // Bank patterns are now initialized only once in VoiceReminderApplication
            // This was causing repeated database writes on every data fetch

            // Get all tracked transactions
            val transactions = financeTrackingService.getAllTransactions()

            // Convert database entities to UI models
            val uiTransactions = transactions.map { transaction ->
                Transaction(
                    id = transaction.id.toString(),
                    title = transaction.description,
                    subtitle = formatTimestamp(transaction.timestamp.time),
                    amount = if (transaction.transactionType == EntityTransactionType.CREDIT) transaction.amount else -transaction.amount,
                    type = if (transaction.transactionType == EntityTransactionType.CREDIT) TransactionType.RECEIVED else TransactionType.SPENT,
                    category = mapTransactionCategory(transaction.description),
                    timestamp = transaction.timestamp.time,
                    isManual = transaction.isManual,
                    isEdited = transaction.isEdited,
                    notes = transaction.notes
                )
            }

            // Calculate finance summary based on transactions
            val summary = calculateFinanceSummary(uiTransactions)
            val quickStats = calculateQuickStats(uiTransactions)
            val insight = generateAIInsight(uiTransactions)

            // Validate data before returning
            validateFinanceSummary(summary)
            validateQuickStats(quickStats)
            validateTransactions(uiTransactions)
            if (insight != null) validateAIInsight(insight)

            Log.d(TAG, "Finance data retrieved successfully: ${uiTransactions.size} transactions")

            return FinanceData(
                summary = summary,
                quickStats = quickStats,
                transactions = uiTransactions,
                insight = insight
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving finance data", e)
            // Return template data as fallback
            return getTemplateFinanceData()
        }
    }

    private fun calculateFinanceSummary(transactions: List<Transaction>): FinanceSummary {
        val totalReceived = transactions.filter { it.type == TransactionType.RECEIVED }.sumOf { it.amount }
        val totalSpent = transactions.filter { it.type == TransactionType.SPENT }.sumOf { kotlin.math.abs(it.amount) }

        // Calculate trend (simplified - comparing with previous period)
        val currentTime = System.currentTimeMillis()
        val oneMonthAgo = currentTime - (30 * 24 * 60 * 60 * 1000L)

        val recentSpent = transactions
            .filter { it.type == TransactionType.SPENT && it.timestamp > oneMonthAgo }
            .sumOf { kotlin.math.abs(it.amount) }

        val previousSpent = transactions
            .filter { it.type == TransactionType.SPENT && it.timestamp <= oneMonthAgo && it.timestamp > (oneMonthAgo - 30 * 24 * 60 * 60 * 1000L) }
            .sumOf { kotlin.math.abs(it.amount) }

        val trendPercentage = if (previousSpent > 0) {
            ((recentSpent - previousSpent) / previousSpent) * 100
        } else if (recentSpent > 0) {
            100.0 // 100% increase if there was no previous spending
        } else {
            0.0
        }

        val trendDirection = when {
            trendPercentage > 0 -> TrendDirection.UP
            trendPercentage < 0 -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }

        return FinanceSummary(
            totalSpent = totalSpent,
            totalReceived = totalReceived,
            trendPercentage = kotlin.math.abs(trendPercentage),
            trendDirection = trendDirection,
            month = android.text.format.DateFormat.format("MMMM yyyy", System.currentTimeMillis()).toString()
        )
    }

    private fun calculateQuickStats(transactions: List<Transaction>): List<QuickStat> {
        val currentTime = System.currentTimeMillis()
        val oneWeekAgo = currentTime - (7 * 24 * 60 * 60 * 1000L)

        val weeklySpending = transactions
            .filter { it.type == TransactionType.SPENT && it.timestamp > oneWeekAgo }
            .sumOf { kotlin.math.abs(it.amount) }

        val biggestExpense = transactions
            .filter { it.type == TransactionType.SPENT }
            .maxByOrNull { it.amount }

        val topCategory = transactions
            .filter { it.type == TransactionType.SPENT }
            .groupBy { it.category }
            .maxByOrNull { it.value.sumOf { t -> kotlin.math.abs(t.amount) } }

        return listOf(
            QuickStat(
                icon = Icons.Default.CalendarToday,
                value = "ETB ${String.format("%.0f", weeklySpending)}",
                label = "This Week's Spend",
                type = StatType.WEEKLY_SPEND
            ),
            QuickStat(
                icon = Icons.Default.ShoppingBag,
                value = if (biggestExpense != null) "ETB ${String.format("%.0f", kotlin.math.abs(biggestExpense.amount))}" else "N/A",
                label = "Biggest Expense",
                type = StatType.BIGGEST_EXPENSE
            ),
            QuickStat(
                icon = Icons.Default.Category,
                value = topCategory?.key?.name ?: "N/A",
                label = "Most Spent Category",
                type = StatType.TOP_CATEGORY
            )
        )
    }

    private fun generateAIInsight(transactions: List<Transaction>): AIInsight? {
        if (transactions.isEmpty()) return null

        val currentTime = System.currentTimeMillis()
        val oneWeekAgo = currentTime - (7 * 24 * 60 * 60 * 1000L)
        val twoWeeksAgo = oneWeekAgo - (7 * 24 * 60 * 60 * 1000L)

        val recentSpending = transactions
            .filter { it.type == TransactionType.SPENT && it.timestamp > oneWeekAgo }
            .sumOf { kotlin.math.abs(it.amount) }

        val previousSpending = transactions
            .filter { it.type == TransactionType.SPENT && it.timestamp > twoWeeksAgo && it.timestamp <= oneWeekAgo }
            .sumOf { kotlin.math.abs(it.amount) }

        if (previousSpending > 0) {
            val percentageChange = ((recentSpending - previousSpending) / previousSpending) * 100

            val message = if (percentageChange > 0) {
                "You spent ${String.format("%.1f", percentageChange)}% more this week compared to last week."
            } else if (percentageChange < 0) {
                "You spent ${String.format("%.1f", kotlin.math.abs(percentageChange))}% less this week compared to last week."
            } else {
                "Your spending stayed the same as last week."
            }

            val insightType = when {
                percentageChange > 20 -> InsightType.SPENDING_INCREASE
                percentageChange < -20 -> InsightType.SPENDING_DECREASE
                else -> InsightType.SAVING_TIP
            }

            return AIInsight(
                message = message,
                type = insightType,
                timestamp = System.currentTimeMillis()
            )
        }

        return null
    }

    private fun mapTransactionCategory(description: String): TransactionCategory {
        val lowerDesc = description.lowercase()

        return when {
            listOf("salary", "wage", "income", "payment", "deposit", "credit").any { lowerDesc.contains(it) } -> TransactionCategory.SALARY
            listOf("food", "restaurant", "cafe", "meal", "dine", "eat").any { lowerDesc.contains(it) } -> TransactionCategory.FOOD
            listOf("grocery", "market", "shop", "store", "buy").any { lowerDesc.contains(it) } -> TransactionCategory.GROCERIES
            listOf("mobile", "call", "phone", "airtime", "topup", "data").any { lowerDesc.contains(it) } -> TransactionCategory.MOBILE
            listOf("transport", "taxi", "bus", "gas", "fuel", "petrol").any { lowerDesc.contains(it) } -> TransactionCategory.TRANSPORT
            listOf("bill", "electricity", "water", "internet", "service").any { lowerDesc.contains(it) } -> TransactionCategory.BILLS
            listOf("entertainment", "movie", "game", "theater", "fun").any { lowerDesc.contains(it) } -> TransactionCategory.ENTERTAINMENT
            else -> TransactionCategory.OTHER
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - timestamp
        val dayInMillis = 24 * 60 * 60 * 1000L

        return when {
            timeDiff < 24 * 60 * 60 * 1000L -> "Today • ${android.text.format.DateFormat.format("h:mm a", timestamp)}"
            timeDiff < 2 * 24 * 60 * 60 * 1000L -> "Yesterday • ${android.text.format.DateFormat.format("h:mm a", timestamp)}"
            timeDiff < 7 * dayInMillis -> "${(timeDiff / dayInMillis).toInt()} days ago • ${android.text.format.DateFormat.format("h:mm a", timestamp)}"
            else -> android.text.format.DateFormat.format("MMM dd • h:mm a", timestamp).toString()
        }
    }

    /**
     * Validates FinanceSummary data
     * Ensures all numeric values are valid (not NaN or Infinity)
     */
    private fun validateFinanceSummary(summary: FinanceSummary) {
        if (summary.totalSpent.isNaN() || summary.totalSpent.isInfinite()) {
            Log.e(TAG, "Invalid totalSpent: ${summary.totalSpent}")
            throw IllegalStateException("FinanceSummary.totalSpent is invalid: ${summary.totalSpent}")
        }

        if (summary.totalReceived.isNaN() || summary.totalReceived.isInfinite()) {
            Log.e(TAG, "Invalid totalReceived: ${summary.totalReceived}")
            throw IllegalStateException("FinanceSummary.totalReceived is invalid: ${summary.totalReceived}")
        }

        if (summary.trendPercentage.isNaN() || summary.trendPercentage.isInfinite()) {
            Log.e(TAG, "Invalid trendPercentage: ${summary.trendPercentage}")
            throw IllegalStateException("FinanceSummary.trendPercentage is invalid: ${summary.trendPercentage}")
        }

        if (summary.month.isBlank()) {
            Log.e(TAG, "Invalid month: empty or blank")
            throw IllegalStateException("FinanceSummary.month cannot be blank")
        }

        Log.d(TAG, "FinanceSummary validated: totalSpent=${summary.totalSpent}, totalReceived=${summary.totalReceived}")
    }

    /**
     * Validates QuickStats list
     * Ensures list is not null and all entries have valid data
     */
    private fun validateQuickStats(quickStats: List<QuickStat>) {
        if (quickStats.isEmpty()) {
            Log.w(TAG, "QuickStats list is empty")
        }

        quickStats.forEachIndexed { index, stat ->
            if (stat.value.isBlank()) {
                Log.e(TAG, "QuickStat[$index] has blank value")
                throw IllegalStateException("QuickStat at index $index has blank value")
            }

            if (stat.label.isBlank()) {
                Log.e(TAG, "QuickStat[$index] has blank label")
                throw IllegalStateException("QuickStat at index $index has blank label")
            }
        }

        Log.d(TAG, "QuickStats validated: ${quickStats.size} items")
    }

    /**
     * Validates Transactions list
     * Ensures list is not null and all transactions have valid data
     */
    private fun validateTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            Log.w(TAG, "Transactions list is empty")
        }

        transactions.forEachIndexed { index, transaction ->
            if (transaction.id.isBlank()) {
                Log.e(TAG, "Transaction[$index] has blank id")
                throw IllegalStateException("Transaction at index $index has blank id")
            }

            if (transaction.title.isBlank()) {
                Log.e(TAG, "Transaction[$index] has blank title")
                throw IllegalStateException("Transaction at index $index has blank title")
            }

            if (transaction.amount.isNaN() || transaction.amount.isInfinite()) {
                Log.e(TAG, "Transaction[$index] has invalid amount: ${transaction.amount}")
                throw IllegalStateException("Transaction at index $index has invalid amount: ${transaction.amount}")
            }

            if (transaction.timestamp < 0) {
                Log.e(TAG, "Transaction[$index] has invalid timestamp: ${transaction.timestamp}")
                throw IllegalStateException("Transaction at index $index has invalid timestamp: ${transaction.timestamp}")
            }
        }

        Log.d(TAG, "Transactions validated: ${transactions.size} items")
    }

    /**
     * Validates AIInsight data
     * Ensures message is not blank and timestamp is valid
     */
    private fun validateAIInsight(insight: AIInsight) {
        if (insight.message.isBlank()) {
            Log.e(TAG, "AIInsight has blank message")
            throw IllegalStateException("AIInsight.message cannot be blank")
        }

        if (insight.timestamp < 0) {
            Log.e(TAG, "AIInsight has invalid timestamp: ${insight.timestamp}")
            throw IllegalStateException("AIInsight.timestamp is invalid: ${insight.timestamp}")
        }

        Log.d(TAG, "AIInsight validated")
    }

    private fun getTemplateFinanceData(): FinanceData {
        Log.w(TAG, "Returning template finance data due to error")
        val currentTime = System.currentTimeMillis()

        return FinanceData(
            summary = FinanceSummary(
                totalSpent = 0.0,
                totalReceived = 0.0,
                trendPercentage = 0.0,
                trendDirection = TrendDirection.FLAT,
                month = android.text.format.DateFormat.format("MMMM yyyy", currentTime).toString()
            ),
            quickStats = listOf(),
            transactions = listOf(),
            insight = AIInsight(
                message = "No financial data available yet. Start receiving bank SMS messages to track your spending.",
                type = InsightType.SAVING_TIP,
                timestamp = currentTime
            )
        )
    }
}
