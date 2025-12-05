package com.example.voicereminder.domain.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FinanceSummary data class
 * Tests data class equality, copying, and TrendDirection enum
 */
class FinanceSummaryTest {

    @Test
    fun `FinanceSummary data class equality works correctly`() {
        val summary1 = FinanceSummary(
            totalSpent = 12450.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )
        
        val summary2 = FinanceSummary(
            totalSpent = 12450.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )
        
        assertEquals(summary1, summary2)
        assertEquals(summary1.hashCode(), summary2.hashCode())
    }

    @Test
    fun `FinanceSummary with different values are not equal`() {
        val summary1 = FinanceSummary(
            totalSpent = 12450.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )
        
        val summary2 = FinanceSummary(
            totalSpent = 10000.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )
        
        assertNotEquals(summary1, summary2)
    }

    @Test
    fun `FinanceSummary copy function works correctly`() {
        val original = FinanceSummary(
            totalSpent = 12450.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )
        
        val copied = original.copy()
        
        assertEquals(original, copied)
        assertEquals(original.totalSpent, copied.totalSpent, 0.01)
        assertEquals(original.totalReceived, copied.totalReceived, 0.01)
        assertEquals(original.trendPercentage, copied.trendPercentage, 0.01)
        assertEquals(original.trendDirection, copied.trendDirection)
        assertEquals(original.month, copied.month)
    }

    @Test
    fun `FinanceSummary copy with modified totalSpent works correctly`() {
        val original = FinanceSummary(
            totalSpent = 12450.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )
        
        val modified = original.copy(totalSpent = 15000.0)
        
        assertEquals(15000.0, modified.totalSpent, 0.01)
        assertEquals(original.totalReceived, modified.totalReceived, 0.01)
        assertEquals(original.trendPercentage, modified.trendPercentage, 0.01)
        assertEquals(original.trendDirection, modified.trendDirection)
        assertEquals(original.month, modified.month)
    }

    @Test
    fun `FinanceSummary copy with modified trendDirection works correctly`() {
        val original = FinanceSummary(
            totalSpent = 12450.0,
            totalReceived = 4200.0,
            trendPercentage = 12.0,
            trendDirection = TrendDirection.UP,
            month = "November 2025"
        )
        
        val modified = original.copy(trendDirection = TrendDirection.DOWN)
        
        assertEquals(TrendDirection.DOWN, modified.trendDirection)
        assertEquals(original.totalSpent, modified.totalSpent, 0.01)
    }

    @Test
    fun `TrendDirection enum has all expected values`() {
        val values = TrendDirection.values()
        
        assertEquals(3, values.size)
        assertTrue(values.contains(TrendDirection.UP))
        assertTrue(values.contains(TrendDirection.DOWN))
        assertTrue(values.contains(TrendDirection.NEUTRAL))
    }

    @Test
    fun `TrendDirection valueOf works correctly`() {
        assertEquals(TrendDirection.UP, TrendDirection.valueOf("UP"))
        assertEquals(TrendDirection.DOWN, TrendDirection.valueOf("DOWN"))
        assertEquals(TrendDirection.NEUTRAL, TrendDirection.valueOf("NEUTRAL"))
    }

    @Test
    fun `FinanceSummary handles zero values correctly`() {
        val summary = FinanceSummary(
            totalSpent = 0.0,
            totalReceived = 0.0,
            trendPercentage = 0.0,
            trendDirection = TrendDirection.NEUTRAL,
            month = "November 2025"
        )
        
        assertEquals(0.0, summary.totalSpent, 0.01)
        assertEquals(0.0, summary.totalReceived, 0.01)
        assertEquals(0.0, summary.trendPercentage, 0.01)
        assertEquals(TrendDirection.NEUTRAL, summary.trendDirection)
    }

    @Test
    fun `FinanceSummary handles negative trend percentage correctly`() {
        val summary = FinanceSummary(
            totalSpent = 8000.0,
            totalReceived = 5000.0,
            trendPercentage = -15.5,
            trendDirection = TrendDirection.DOWN,
            month = "October 2025"
        )
        
        assertEquals(-15.5, summary.trendPercentage, 0.01)
        assertEquals(TrendDirection.DOWN, summary.trendDirection)
    }

    @Test
    fun `FinanceSummary handles large values correctly`() {
        val summary = FinanceSummary(
            totalSpent = 1000000.0,
            totalReceived = 500000.0,
            trendPercentage = 150.0,
            trendDirection = TrendDirection.UP,
            month = "December 2025"
        )
        
        assertEquals(1000000.0, summary.totalSpent, 0.01)
        assertEquals(500000.0, summary.totalReceived, 0.01)
        assertEquals(150.0, summary.trendPercentage, 0.01)
    }
}
