package com.example.voicereminder.domain.models

/**
 * Data class representing filter criteria for transactions
 */
data class TransactionFilter(
    val dateRange: DateRange = DateRange.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val categories: Set<TransactionCategory> = emptySet(), // empty = all categories
    val types: Set<TransactionType> = emptySet(),          // empty = all types
    val searchQuery: String = ""
) {
    /**
     * Check if any filter is active
     */
    fun hasActiveFilters(): Boolean {
        return dateRange != DateRange.ALL ||
                categories.isNotEmpty() ||
                types.isNotEmpty() ||
                searchQuery.isNotBlank()
    }
    
    /**
     * Reset all filters to default
     */
    fun reset(): TransactionFilter = TransactionFilter()
}

/**
 * Enum representing predefined date ranges for filtering
 */
enum class DateRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    CUSTOM,
    ALL;
    
    fun getDisplayName(): String {
        return when (this) {
            TODAY -> "Today"
            THIS_WEEK -> "This Week"
            THIS_MONTH -> "This Month"
            LAST_MONTH -> "Last Month"
            THIS_YEAR -> "This Year"
            CUSTOM -> "Custom"
            ALL -> "All Time"
        }
    }
}
