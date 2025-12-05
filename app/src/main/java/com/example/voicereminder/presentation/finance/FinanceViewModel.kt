package com.example.voicereminder.presentation.finance

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.FinanceRepository
import com.example.voicereminder.domain.models.Budget
import com.example.voicereminder.domain.models.DateRange
import com.example.voicereminder.domain.models.FinanceData
import com.example.voicereminder.domain.models.Transaction
import com.example.voicereminder.domain.models.TransactionCategory
import com.example.voicereminder.domain.models.TransactionFilter
import com.example.voicereminder.domain.models.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for the Finance Tab feature
 * Manages UI state and data loading for financial information display
 * Enhanced with budget management, filtering, and transaction CRUD operations
 */
class FinanceViewModel(
    private val financeRepository: com.example.voicereminder.data.FinanceRepository,
    private val financeTrackingService: com.example.voicereminder.domain.FinanceTrackingService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<FinanceUiState>(FinanceUiState.Loading)
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private val _smsSources = MutableStateFlow<List<com.example.voicereminder.data.entity.SmsSource>>(emptyList())
    val smsSources: StateFlow<List<com.example.voicereminder.data.entity.SmsSource>> = _smsSources.asStateFlow()
    
    // New: Transaction filter state
    private val _filterState = MutableStateFlow(TransactionFilter())
    val filterState: StateFlow<TransactionFilter> = _filterState.asStateFlow()
    
    // New: Filtered transactions based on current filter
    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredTransactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()
    
    // New: Budgets state
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()
    
    // New: Dialog states
    private val _showAddTransactionDialog = MutableStateFlow(false)
    val showAddTransactionDialog: StateFlow<Boolean> = _showAddTransactionDialog.asStateFlow()
    
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction.asStateFlow()
    
    private val _showBudgetDialog = MutableStateFlow(false)
    val showBudgetDialog: StateFlow<Boolean> = _showBudgetDialog.asStateFlow()
    
    private val _editingBudget = MutableStateFlow<Budget?>(null)
    val editingBudget: StateFlow<Budget?> = _editingBudget.asStateFlow()
    
    // Track if data has been loaded to avoid repeated loading
    private var patternsLoaded = false
    private var budgetsLoaded = false
    
    // Job for observing transactions flow
    private var transactionObserverJob: Job? = null
    
    init {
        Log.d(TAG, "FinanceViewModel initialized")
        // Start observing database changes immediately
        // This ensures UI updates automatically when data changes
        observeTransactions()
    }
    
    /**
     * Observe transactions from database using Flow
     * This automatically updates UI when database changes
     */
    private fun observeTransactions() {
        transactionObserverJob?.cancel()
        transactionObserverJob = viewModelScope.launch {
            Log.d(TAG, "Starting to observe transactions flow")
            financeTrackingService.getAllTransactionsFlow()
                .catch { e ->
                    Log.e(TAG, "Error observing transactions", e)
                    _uiState.value = FinanceUiState.Error("Failed to load transactions: ${e.message}")
                }
                .collect { transactions ->
                    Log.d(TAG, "Received ${transactions.size} transactions from database")
                    processTransactions(transactions)
                }
        }
    }
    
    /**
     * Process transactions from database and update UI state
     */
    private fun processTransactions(transactions: List<com.example.voicereminder.data.entity.FinanceTransaction>) {
        try {
            // Convert database entities to UI models
            val uiTransactions = transactions.map { transaction ->
                Transaction(
                    id = transaction.id.toString(),
                    title = transaction.description,
                    subtitle = formatTimestamp(transaction.timestamp.time),
                    amount = if (transaction.transactionType == com.example.voicereminder.data.entity.TransactionType.CREDIT) 
                        transaction.amount else -transaction.amount,
                    type = if (transaction.transactionType == com.example.voicereminder.data.entity.TransactionType.CREDIT) 
                        TransactionType.RECEIVED else TransactionType.SPENT,
                    category = mapTransactionCategory(transaction.description),
                    timestamp = transaction.timestamp.time,
                    isManual = transaction.isManual,
                    isEdited = transaction.isEdited,
                    notes = transaction.notes
                )
            }
            
            if (uiTransactions.isEmpty()) {
                Log.d(TAG, "No transactions - showing empty state")
                _uiState.value = FinanceUiState.Empty
            } else {
                // Calculate summary and stats
                val summary = calculateFinanceSummary(uiTransactions)
                val quickStats = calculateQuickStats(uiTransactions)
                val insight = generateAIInsight(uiTransactions)
                
                val financeData = com.example.voicereminder.domain.models.FinanceData(
                    summary = summary,
                    quickStats = quickStats,
                    transactions = uiTransactions,
                    insight = insight
                )
                
                Log.d(TAG, "Updating UI with ${uiTransactions.size} transactions")
                _uiState.value = FinanceUiState.Success(financeData)
                
                // Apply current filter
                applyFilter()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing transactions", e)
            _uiState.value = FinanceUiState.Error("Error processing data: ${e.message}")
        }
    }
    
    /**
     * Called when the screen becomes visible
     * Loads additional data lazily
     */
    fun onScreenVisible() {
        if (!patternsLoaded) {
            loadSmsSources()
        }
        if (!budgetsLoaded) {
            loadBudgets()
        }
    }
    
    /**
     * Force refresh finance data - triggers re-observation
     */
    fun refreshData() {
        Log.d(TAG, "refreshData() called - restarting observation")
        observeTransactions()
    }
    
    // ==================== Filter Operations ====================
    
    /**
     * Update the transaction filter
     */
    fun updateFilter(newFilter: TransactionFilter) {
        Log.d(TAG, "updateFilter() called with: $newFilter")
        _filterState.value = newFilter
        applyFilter()
    }
    
    /**
     * Apply current filter to transactions
     */
    private fun applyFilter() {
        val currentState = _uiState.value
        if (currentState !is FinanceUiState.Success) return
        
        val allTransactions = currentState.data.transactions ?: return
        val filter = _filterState.value
        
        var filtered = allTransactions
        
        // Apply date range filter
        filtered = when (filter.dateRange) {
            DateRange.TODAY -> {
                val today = getStartOfDay(System.currentTimeMillis())
                filtered.filter { it.timestamp >= today }
            }
            DateRange.THIS_WEEK -> {
                val weekStart = getStartOfWeek()
                filtered.filter { it.timestamp >= weekStart }
            }
            DateRange.THIS_MONTH -> {
                val monthStart = getStartOfMonth()
                filtered.filter { it.timestamp >= monthStart }
            }
            DateRange.LAST_MONTH -> {
                val (start, end) = getLastMonthRange()
                filtered.filter { it.timestamp in start..end }
            }
            DateRange.THIS_YEAR -> {
                val yearStart = getStartOfYear()
                filtered.filter { it.timestamp >= yearStart }
            }
            DateRange.CUSTOM -> {
                val start = filter.customStartDate ?: 0L
                val end = filter.customEndDate ?: System.currentTimeMillis()
                filtered.filter { it.timestamp in start..end }
            }
            DateRange.ALL -> filtered
        }
        
        // Apply category filter
        if (filter.categories.isNotEmpty()) {
            filtered = filtered.filter { it.category in filter.categories }
        }
        
        // Apply type filter
        if (filter.types.isNotEmpty()) {
            filtered = filtered.filter { it.type in filter.types }
        }
        
        // Apply search query
        if (filter.searchQuery.isNotBlank()) {
            val query = filter.searchQuery.lowercase()
            filtered = filtered.filter { 
                it.title.lowercase().contains(query) ||
                it.subtitle.lowercase().contains(query) ||
                it.notes?.lowercase()?.contains(query) == true
            }
        }
        
        _filteredTransactions.value = filtered
        Log.d(TAG, "Filter applied: ${filtered.size} transactions match")
    }
    
    // ==================== Transaction CRUD Operations ====================
    
    /**
     * Show add transaction dialog
     */
    fun showAddTransaction() {
        _showAddTransactionDialog.value = true
    }
    
    /**
     * Hide add transaction dialog
     */
    fun hideAddTransaction() {
        _showAddTransactionDialog.value = false
    }
    
    /**
     * Show edit transaction dialog
     */
    fun showEditTransaction(transaction: Transaction) {
        _editingTransaction.value = transaction
    }
    
    /**
     * Hide edit transaction dialog
     */
    fun hideEditTransaction() {
        _editingTransaction.value = null
    }
    
    /**
     * Add a new manual transaction
     */
    fun addManualTransaction(
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        description: String,
        date: Long,
        notes: String?
    ) {
        Log.d(TAG, "addManualTransaction() called")
        viewModelScope.launch {
            try {
                val transactionType = if (type == TransactionType.RECEIVED) {
                    com.example.voicereminder.data.entity.TransactionType.CREDIT
                } else {
                    com.example.voicereminder.data.entity.TransactionType.DEBIT
                }
                
                val transaction = com.example.voicereminder.data.entity.FinanceTransaction(
                    amount = amount,
                    currency = "AFN",
                    description = description,
                    transactionType = transactionType,
                    bankName = "Manual Entry",
                    phoneNumber = "",
                    smsContent = "",
                    timestamp = java.util.Date(date),
                    category = category.name,
                    isManual = true,
                    isEdited = false,
                    notes = notes
                )
                
                financeTrackingService.addTransaction(transaction)
                Log.d(TAG, "Manual transaction added successfully")
                
                // Refresh data immediately
                refreshData()
                hideAddTransaction()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding manual transaction", e)
            }
        }
    }
    
    /**
     * Edit an existing transaction
     */
    fun editTransaction(
        transactionId: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        description: String,
        notes: String?
    ) {
        Log.d(TAG, "editTransaction() called for id: $transactionId")
        viewModelScope.launch {
            try {
                // For now, we'll need to implement this in the tracking service
                // This is a placeholder for the edit functionality
                Log.d(TAG, "Transaction edit requested - implementation pending")
                
                // Flow will auto-refresh when database changes
                hideEditTransaction()
            } catch (e: Exception) {
                Log.e(TAG, "Error editing transaction", e)
            }
        }
    }
    
    /**
     * Delete a transaction
     */
    fun deleteTransaction(transaction: Transaction) {
        Log.d(TAG, "deleteTransaction() called for: ${transaction.title}")
        viewModelScope.launch {
            try {
                // Convert domain model back to entity for deletion
                // This is simplified - in production you'd fetch the entity by ID
                Log.d(TAG, "Transaction deletion requested - implementation pending")
                
                // Flow will auto-refresh when database changes
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting transaction", e)
            }
        }
    }
    
    // ==================== Budget Operations ====================
    
    /**
     * Show budget dialog for creating new budget
     */
    fun showCreateBudget() {
        _editingBudget.value = null
        _showBudgetDialog.value = true
    }
    
    /**
     * Show budget dialog for editing existing budget
     */
    fun showEditBudget(budget: Budget) {
        _editingBudget.value = budget
        _showBudgetDialog.value = true
    }
    
    /**
     * Hide budget dialog
     */
    fun hideBudgetDialog() {
        _showBudgetDialog.value = false
        _editingBudget.value = null
    }
    
    /**
     * Load budgets from database
     */
    private fun loadBudgets() {
        Log.d(TAG, "loadBudgets() called")
        viewModelScope.launch {
            try {
                // For now, return empty list - will be implemented with BudgetDao
                _budgets.value = emptyList()
                budgetsLoaded = true
                Log.d(TAG, "Budgets loaded: ${_budgets.value.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budgets", e)
            }
        }
    }
    
    /**
     * Create or update a budget
     */
    fun saveBudget(category: TransactionCategory, limit: Double, month: String) {
        Log.d(TAG, "saveBudget() called for $category with limit $limit")
        viewModelScope.launch {
            try {
                // Implementation will use BudgetDao
                Log.d(TAG, "Budget save requested - implementation pending")
                hideBudgetDialog()
                loadBudgets()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving budget", e)
            }
        }
    }
    
    /**
     * Delete a budget
     */
    fun deleteBudget(budget: Budget) {
        Log.d(TAG, "deleteBudget() called for ${budget.category}")
        viewModelScope.launch {
            try {
                // Implementation will use BudgetDao
                Log.d(TAG, "Budget deletion requested - implementation pending")
                loadBudgets()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting budget", e)
            }
        }
    }
    
    // ==================== Helper Functions ====================
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getLastMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        
        return Pair(start, end)
    }
    
    private fun getStartOfYear(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - timestamp
        val dayInMillis = 24 * 60 * 60 * 1000L

        return when {
            timeDiff < dayInMillis -> "Today • ${android.text.format.DateFormat.format("h:mm a", timestamp)}"
            timeDiff < 2 * dayInMillis -> "Yesterday • ${android.text.format.DateFormat.format("h:mm a", timestamp)}"
            timeDiff < 7 * dayInMillis -> "${(timeDiff / dayInMillis).toInt()} days ago"
            else -> android.text.format.DateFormat.format("MMM dd", timestamp).toString()
        }
    }
    
    /**
     * Map transaction description to category
     */
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
    
    /**
     * Calculate finance summary from transactions
     */
    private fun calculateFinanceSummary(transactions: List<Transaction>): com.example.voicereminder.domain.models.FinanceSummary {
        val totalReceived = transactions.filter { it.type == TransactionType.RECEIVED }.sumOf { it.amount }
        val totalSpent = transactions.filter { it.type == TransactionType.SPENT }.sumOf { kotlin.math.abs(it.amount) }

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
        } else if (recentSpent > 0) 100.0 else 0.0

        val trendDirection = when {
            trendPercentage > 0 -> com.example.voicereminder.domain.models.TrendDirection.UP
            trendPercentage < 0 -> com.example.voicereminder.domain.models.TrendDirection.DOWN
            else -> com.example.voicereminder.domain.models.TrendDirection.FLAT
        }

        return com.example.voicereminder.domain.models.FinanceSummary(
            totalSpent = totalSpent,
            totalReceived = totalReceived,
            trendPercentage = kotlin.math.abs(trendPercentage),
            trendDirection = trendDirection,
            month = android.text.format.DateFormat.format("MMMM yyyy", currentTime).toString()
        )
    }
    
    /**
     * Calculate quick stats from transactions
     */
    private fun calculateQuickStats(transactions: List<Transaction>): List<com.example.voicereminder.domain.models.QuickStat> {
        val currentTime = System.currentTimeMillis()
        val oneWeekAgo = currentTime - (7 * 24 * 60 * 60 * 1000L)

        val weeklySpending = transactions
            .filter { it.type == TransactionType.SPENT && it.timestamp > oneWeekAgo }
            .sumOf { kotlin.math.abs(it.amount) }

        val biggestExpense = transactions
            .filter { it.type == TransactionType.SPENT }
            .maxByOrNull { kotlin.math.abs(it.amount) }

        val topCategory = transactions
            .filter { it.type == TransactionType.SPENT }
            .groupBy { it.category }
            .maxByOrNull { it.value.sumOf { t -> kotlin.math.abs(t.amount) } }

        return listOf(
            com.example.voicereminder.domain.models.QuickStat(
                icon = Icons.Filled.AccountBalanceWallet,
                value = "AFN ${String.format("%.0f", weeklySpending)}",
                label = "This Week",
                type = com.example.voicereminder.domain.models.StatType.WEEKLY_SPEND
            ),
            com.example.voicereminder.domain.models.QuickStat(
                icon = Icons.Filled.ShoppingCart,
                value = if (biggestExpense != null) "AFN ${String.format("%.0f", kotlin.math.abs(biggestExpense.amount))}" else "N/A",
                label = "Biggest",
                type = com.example.voicereminder.domain.models.StatType.BIGGEST_EXPENSE
            ),
            com.example.voicereminder.domain.models.QuickStat(
                icon = Icons.Filled.MoreHoriz,
                value = topCategory?.key?.name ?: "N/A",
                label = "Top Category",
                type = com.example.voicereminder.domain.models.StatType.TOP_CATEGORY
            )
        )
    }
    
    /**
     * Generate AI insight from transactions
     */
    private fun generateAIInsight(transactions: List<Transaction>): com.example.voicereminder.domain.models.AIInsight? {
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
                "You spent ${String.format("%.1f", percentageChange)}% more this week."
            } else if (percentageChange < 0) {
                "You spent ${String.format("%.1f", kotlin.math.abs(percentageChange))}% less this week. Great job!"
            } else {
                "Your spending stayed the same as last week."
            }

            return com.example.voicereminder.domain.models.AIInsight(
                message = message,
                type = if (percentageChange > 20) com.example.voicereminder.domain.models.InsightType.SPENDING_INCREASE
                       else if (percentageChange < -20) com.example.voicereminder.domain.models.InsightType.SPENDING_DECREASE
                       else com.example.voicereminder.domain.models.InsightType.SAVING_TIP,
                timestamp = currentTime
            )
        }
        return null
    }
    
    /**
     * Load financial data from repository (legacy - kept for compatibility)
     * Handles state transitions: Loading -> Success/Error/Empty
     */
    private fun loadFinanceData() {
        Log.d(TAG, "loadFinanceData() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "State transition: -> Loading")
                _uiState.value = FinanceUiState.Loading
                Log.d(TAG, "State set to Loading successfully")

                Log.d(TAG, "Calling repository.getFinanceData()")
                val data = financeRepository.getFinanceData()
                Log.d(TAG, "Repository returned data: ${data != null}")

                // Comprehensive null safety checks with detailed error messages
                
                // Check if data object itself is null
                if (data == null) {
                    Log.e(TAG, "VALIDATION FAILED: Repository returned null data object")
                    Log.e(TAG, "Error path: NULL_DATA_OBJECT")
                    Log.d(TAG, "State transition: Loading -> Error (null data object)")
                    _uiState.value = FinanceUiState.Error(
                        message = "Unable to load financial data. The data source returned no information."
                    )
                    return@launch
                }
                
                // Check if summary is null
                if (data.summary == null) {
                    Log.e(TAG, "VALIDATION FAILED: Summary is null")
                    Log.e(TAG, "Error path: NULL_SUMMARY")
                    Log.e(TAG, "Data state - transactions: ${data.transactions != null}, quickStats: ${data.quickStats != null}, insight: ${data.insight != null}")
                    Log.d(TAG, "State transition: Loading -> Error (null summary)")
                    _uiState.value = FinanceUiState.Error(
                        message = "Unable to load financial summary. Please try again."
                    )
                    return@launch
                }
                
                // Check if quickStats is null
                if (data.quickStats == null) {
                    Log.e(TAG, "VALIDATION FAILED: QuickStats is null")
                    Log.e(TAG, "Error path: NULL_QUICK_STATS")
                    Log.e(TAG, "Data state - summary: ${data.summary != null}, transactions: ${data.transactions != null}, insight: ${data.insight != null}")
                    Log.d(TAG, "State transition: Loading -> Error (null quickStats)")
                    _uiState.value = FinanceUiState.Error(
                        message = "Unable to load statistics. Please try again."
                    )
                    return@launch
                }
                
                // Check if transactions list is null
                if (data.transactions == null) {
                    Log.e(TAG, "VALIDATION FAILED: Transactions list is null")
                    Log.e(TAG, "Error path: NULL_TRANSACTIONS")
                    Log.e(TAG, "Data state - summary: ${data.summary != null}, quickStats: ${data.quickStats != null}, insight: ${data.insight != null}")
                    Log.d(TAG, "State transition: Loading -> Error (null transactions)")
                    _uiState.value = FinanceUiState.Error(
                        message = "Unable to load transaction history. Please try again."
                    )
                    return@launch
                }
                
                // Check if insight is null (this is acceptable, but log it)
                if (data.insight == null) {
                    Log.w(TAG, "VALIDATION WARNING: Insight is null (acceptable)")
                    Log.d(TAG, "Insight will not be displayed")
                }

                // Validate summary fields for invalid values
                try {
                    if (data.summary.totalSpent.isNaN() || data.summary.totalSpent.isInfinite()) {
                        Log.e(TAG, "VALIDATION FAILED: Summary totalSpent is invalid: ${data.summary.totalSpent}")
                        Log.e(TAG, "Error path: INVALID_TOTAL_SPENT")
                        _uiState.value = FinanceUiState.Error(
                            message = "Financial data contains invalid values. Please try again."
                        )
                        return@launch
                    }
                    
                    if (data.summary.totalReceived.isNaN() || data.summary.totalReceived.isInfinite()) {
                        Log.e(TAG, "VALIDATION FAILED: Summary totalReceived is invalid: ${data.summary.totalReceived}")
                        Log.e(TAG, "Error path: INVALID_TOTAL_RECEIVED")
                        _uiState.value = FinanceUiState.Error(
                            message = "Financial data contains invalid values. Please try again."
                        )
                        return@launch
                    }
                    
                    if (data.summary.trendPercentage.isNaN() || data.summary.trendPercentage.isInfinite()) {
                        Log.e(TAG, "VALIDATION FAILED: Summary trendPercentage is invalid: ${data.summary.trendPercentage}")
                        Log.e(TAG, "Error path: INVALID_TREND_PERCENTAGE")
                        _uiState.value = FinanceUiState.Error(
                            message = "Financial data contains invalid values. Please try again."
                        )
                        return@launch
                    }
                    
                    if (data.summary.month.isNullOrBlank()) {
                        Log.e(TAG, "VALIDATION FAILED: Summary month is null or blank")
                        Log.e(TAG, "Error path: INVALID_MONTH")
                        _uiState.value = FinanceUiState.Error(
                            message = "Financial data is incomplete. Please try again."
                        )
                        return@launch
                    }
                } catch (e: NullPointerException) {
                    Log.e(TAG, "VALIDATION FAILED: NullPointerException during summary validation", e)
                    Log.e(TAG, "Error path: NPE_SUMMARY_VALIDATION")
                    _uiState.value = FinanceUiState.Error(
                        message = "Financial data is incomplete. Please try again."
                    )
                    return@launch
                }

                Log.d(TAG, "Data validation passed successfully")
                Log.d(TAG, "Transaction count: ${data.transactions.size}")
                Log.d(TAG, "Summary - totalSpent: ${data.summary.totalSpent}, totalReceived: ${data.summary.totalReceived}")
                Log.d(TAG, "QuickStats count: ${data.quickStats.size}")
                Log.d(TAG, "Insight: ${data.insight?.message ?: "null"}")

                // Check if data is empty (no transactions)
                if (data.transactions.isEmpty()) {
                    Log.d(TAG, "State transition: Loading -> Empty (no transactions)")
                    _uiState.value = FinanceUiState.Empty
                } else {
                    Log.d(TAG, "State transition: Loading -> Success (${data.transactions.size} transactions)")
                    _uiState.value = FinanceUiState.Success(data)
                }
                
                Log.d(TAG, "loadFinanceData() completed successfully")
            } catch (e: CancellationException) {
                // Don't catch cancellation - let it propagate
                Log.d(TAG, "Coroutine cancelled - rethrowing CancellationException")
                throw e
            } catch (e: NullPointerException) {
                Log.e(TAG, "NullPointerException in loadFinanceData()", e)
                Log.e(TAG, "Error path: NPE_LOAD_FINANCE_DATA")
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                Log.d(TAG, "State transition: Loading -> Error (NullPointerException)")
                _uiState.value = FinanceUiState.Error(
                    message = "Unable to load financial data due to missing information. Please try again."
                )
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException in loadFinanceData()", e)
                Log.e(TAG, "Error path: ILLEGAL_STATE")
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                Log.d(TAG, "State transition: Loading -> Error (IllegalStateException)")
                _uiState.value = FinanceUiState.Error(
                    message = "Unable to load financial data. The app is in an invalid state. Please restart."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception in loadFinanceData()", e)
                Log.e(TAG, "Error path: UNEXPECTED_EXCEPTION")
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                Log.d(TAG, "State transition: Loading -> Error (unexpected exception)")
                _uiState.value = FinanceUiState.Error(
                    message = "Unable to load financial data. ${e.message ?: "An unexpected error occurred."}"
                )
            }
        }
    }
    
    /**
     * Load SMS sources from service
     */
    private fun loadSmsSources() {
        Log.d(TAG, "loadSmsSources() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching SMS sources from service")
                val sources = financeTrackingService.getAllActiveSources()
                Log.d(TAG, "Service returned ${sources.size} SMS sources")
                _smsSources.value = sources
                patternsLoaded = true
            } catch (e: Exception) {
                Log.e(TAG, "Error loading SMS sources", e)
            }
        }
    }

    /**
     * Refresh SMS sources
     */
    fun refreshSmsSources() {
        Log.d(TAG, "refreshSmsSources() called")
        loadSmsSources()
    }

    /**
     * Retry loading data after an error
     */
    fun retry() {
        Log.d(TAG, "retry() called - reloading finance data")
        loadFinanceData()
    }
    
    // Methods for managing SMS sources (bank/service phone numbers for auto-tracking)
    fun addSmsSource(name: String, phoneNumber: String, description: String?) {
        viewModelScope.launch {
            try {
                val source = com.example.voicereminder.data.entity.SmsSource(
                    name = name,
                    phoneNumber = phoneNumber,
                    description = description,
                    isActive = true
                )
                financeTrackingService.addSmsSource(source)
                Log.d(TAG, "SMS source added: $name - $phoneNumber")
                loadSmsSources()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding SMS source: ${e.message}", e)
            }
        }
    }

    fun updateSmsSource(source: com.example.voicereminder.data.entity.SmsSource) {
        viewModelScope.launch {
            try {
                financeTrackingService.updateSmsSource(source)
                Log.d(TAG, "SMS source updated: ${source.name}")
                loadSmsSources()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating SMS source: ${e.message}", e)
            }
        }
    }

    fun deleteSmsSource(source: com.example.voicereminder.data.entity.SmsSource) {
        viewModelScope.launch {
            try {
                financeTrackingService.removeSmsSource(source)
                Log.d(TAG, "SMS source deleted: ${source.name}")
                loadSmsSources()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting SMS source: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "FinanceViewModel"
    }
}

/**
 * Sealed class representing different UI states for the Finance screen
 */
sealed class FinanceUiState {
    /**
     * Loading state - data is being fetched
     */
    object Loading : FinanceUiState()
    
    /**
     * Success state - data loaded successfully
     * @param data The financial data to display
     */
    data class Success(val data: FinanceData) : FinanceUiState()
    
    /**
     * Error state - data loading failed
     * @param message Error message to display to user
     */
    data class Error(val message: String) : FinanceUiState()
    
    /**
     * Empty state - no transactions available
     */
    object Empty : FinanceUiState()
}

/**
 * Factory for creating FinanceViewModel instances with dependency injection
 */
class FinanceViewModelFactory(
    private val repository: com.example.voicereminder.data.FinanceRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "FinanceViewModelFactory.create() called for ${modelClass.simpleName}")
        try {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                val service = com.example.voicereminder.domain.FinanceTrackingService(context)
                @Suppress("UNCHECKED_CAST")
                val viewModel = FinanceViewModel(repository, service) as T
                Log.d(TAG, "FinanceViewModel created successfully")
                return viewModel
            }
            Log.e(TAG, "Unknown ViewModel class requested: ${modelClass.name}")
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating ViewModel", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "FinanceViewModelFactory"
    }
}
