package com.example.voicereminder.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FinanceRepository
 * Tests data validation and integrity
 */
class FinanceRepositoryTest {
    
    private lateinit var repository: FinanceRepository
    
    @Before
    fun setup() {
        repository = FinanceRepository()
    }
    
    @Test
    fun `getFinanceData returns non-null data`() {
        val data = repository.getFinanceData()
        
        assertNotNull("FinanceData should not be null", data)
    }
    
    @Test
    fun `getFinanceData returns valid summary`() {
        val data = repository.getFinanceData()
        
        assertNotNull("Summary should not be null", data.summary)
        assertFalse("totalSpent should not be NaN", data.summary.totalSpent.isNaN())
        assertFalse("totalSpent should not be Infinity", data.summary.totalSpent.isInfinite())
        assertFalse("totalReceived should not be NaN", data.summary.totalReceived.isNaN())
        assertFalse("totalReceived should not be Infinity", data.summary.totalReceived.isInfinite())
        assertFalse("trendPercentage should not be NaN", data.summary.trendPercentage.isNaN())
        assertFalse("trendPercentage should not be Infinity", data.summary.trendPercentage.isInfinite())
        assertTrue("month should not be blank", data.summary.month.isNotBlank())
    }
    
    @Test
    fun `getFinanceData returns valid quickStats list`() {
        val data = repository.getFinanceData()
        
        assertNotNull("QuickStats should not be null", data.quickStats)
        assertTrue("QuickStats should not be empty", data.quickStats.isNotEmpty())
        
        data.quickStats.forEach { stat ->
            assertTrue("QuickStat value should not be blank", stat.value.isNotBlank())
            assertTrue("QuickStat label should not be blank", stat.label.isNotBlank())
        }
    }
    
    @Test
    fun `getFinanceData returns valid transactions list`() {
        val data = repository.getFinanceData()
        
        assertNotNull("Transactions should not be null", data.transactions)
        assertTrue("Transactions should not be empty", data.transactions.isNotEmpty())
        
        data.transactions.forEach { transaction ->
            assertTrue("Transaction id should not be blank", transaction.id.isNotBlank())
            assertTrue("Transaction title should not be blank", transaction.title.isNotBlank())
            assertFalse("Transaction amount should not be NaN", transaction.amount.isNaN())
            assertFalse("Transaction amount should not be Infinity", transaction.amount.isInfinite())
            assertTrue("Transaction timestamp should be positive", transaction.timestamp >= 0)
        }
    }
    
    @Test
    fun `getFinanceData returns valid insight`() {
        val data = repository.getFinanceData()
        
        assertNotNull("Insight should not be null", data.insight)
        assertTrue("Insight message should not be blank", data.insight.message.isNotBlank())
        assertTrue("Insight timestamp should be positive", data.insight.timestamp >= 0)
    }
    
    @Test
    fun `getFinanceData validates all numeric values`() {
        val data = repository.getFinanceData()
        
        // Validate summary numeric values
        assertFalse("Summary totalSpent should be valid", data.summary.totalSpent.isNaN() || data.summary.totalSpent.isInfinite())
        assertFalse("Summary totalReceived should be valid", data.summary.totalReceived.isNaN() || data.summary.totalReceived.isInfinite())
        assertFalse("Summary trendPercentage should be valid", data.summary.trendPercentage.isNaN() || data.summary.trendPercentage.isInfinite())
        
        // Validate transaction amounts
        data.transactions.forEach { transaction ->
            assertFalse("Transaction amount should be valid", transaction.amount.isNaN() || transaction.amount.isInfinite())
        }
    }
}
