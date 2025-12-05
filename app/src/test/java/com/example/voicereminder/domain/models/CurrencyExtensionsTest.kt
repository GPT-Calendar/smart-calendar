package com.example.voicereminder.domain.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for currency formatting extension function
 * Tests Double.formatCurrency() with various values and edge cases
 */
class CurrencyExtensionsTest {

    @Test
    fun `formatCurrency formats whole numbers correctly`() {
        val result = 12450.0.formatCurrency()
        assertEquals("12,450", result)
    }

    @Test
    fun `formatCurrency formats small numbers correctly`() {
        val result = 350.0.formatCurrency()
        assertEquals("350", result)
    }

    @Test
    fun `formatCurrency formats large numbers with thousand separators`() {
        val result = 1234567.0.formatCurrency()
        assertEquals("1,234,567", result)
    }

    @Test
    fun `formatCurrency formats zero correctly`() {
        val result = 0.0.formatCurrency()
        assertEquals("0", result)
    }

    @Test
    fun `formatCurrency formats negative numbers correctly`() {
        val result = (-1200.0).formatCurrency()
        assertEquals("-1,200", result)
    }

    @Test
    fun `formatCurrency formats decimal values without decimal places`() {
        val result = 12450.99.formatCurrency()
        assertEquals("12,451", result) // Rounds to nearest integer
    }

    @Test
    fun `formatCurrency formats decimal values with rounding`() {
        val result = 1234.50.formatCurrency()
        assertEquals("1,235", result) // Rounds up
    }

    @Test
    fun `formatCurrency formats decimal values rounding down`() {
        val result = 1234.49.formatCurrency()
        assertEquals("1,234", result) // Rounds down
    }

    @Test
    fun `formatCurrency formats very large numbers correctly`() {
        val result = 1000000.0.formatCurrency()
        assertEquals("1,000,000", result)
    }

    @Test
    fun `formatCurrency formats numbers less than 1000 without separator`() {
        val result = 999.0.formatCurrency()
        assertEquals("999", result)
    }

    @Test
    fun `formatCurrency formats exactly 1000 with separator`() {
        val result = 1000.0.formatCurrency()
        assertEquals("1,000", result)
    }

    @Test
    fun `formatCurrency formats negative large numbers correctly`() {
        val result = (-15000.0).formatCurrency()
        assertEquals("-15,000", result)
    }

    @Test
    fun `formatCurrency formats template data totalSpent correctly`() {
        val result = 12450.0.formatCurrency()
        assertEquals("12,450", result)
    }

    @Test
    fun `formatCurrency formats template data totalReceived correctly`() {
        val result = 4200.0.formatCurrency()
        assertEquals("4,200", result)
    }

    @Test
    fun `formatCurrency formats template transaction amounts correctly`() {
        assertEquals("1,200", 1200.0.formatCurrency())
        assertEquals("350", 350.0.formatCurrency())
        assertEquals("15,000", 15000.0.formatCurrency())
        assertEquals("200", 200.0.formatCurrency())
        assertEquals("720", 720.0.formatCurrency())
    }

    @Test
    fun `formatCurrency handles very small positive numbers`() {
        val result = 0.01.formatCurrency()
        assertEquals("0", result) // Rounds to 0
    }

    @Test
    fun `formatCurrency handles very small negative numbers`() {
        val result = (-0.01).formatCurrency()
        assertEquals("0", result) // Rounds to 0
    }

    @Test
    fun `formatCurrency formats millions correctly`() {
        val result = 5000000.0.formatCurrency()
        assertEquals("5,000,000", result)
    }

    @Test
    fun `formatCurrency formats numbers with multiple thousand separators`() {
        val result = 123456789.0.formatCurrency()
        assertEquals("123,456,789", result)
    }
}
