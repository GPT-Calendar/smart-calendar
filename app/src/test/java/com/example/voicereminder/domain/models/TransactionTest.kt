package com.example.voicereminder.domain.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Transaction data class and related enums
 * Tests TransactionCategory.getIcon() mapping and enum values
 */
class TransactionTest {

    @Test
    fun `TransactionCategory BANK returns AccountBalance icon`() {
        val icon = TransactionCategory.BANK.getIcon()
        assertEquals(Icons.Default.AccountBalance, icon)
    }

    @Test
    fun `TransactionCategory FOOD returns Restaurant icon`() {
        val icon = TransactionCategory.FOOD.getIcon()
        assertEquals(Icons.Default.Restaurant, icon)
    }

    @Test
    fun `TransactionCategory SALARY returns AccountBalanceWallet icon`() {
        val icon = TransactionCategory.SALARY.getIcon()
        assertEquals(Icons.Default.AccountBalanceWallet, icon)
    }

    @Test
    fun `TransactionCategory MOBILE returns PhoneAndroid icon`() {
        val icon = TransactionCategory.MOBILE.getIcon()
        assertEquals(Icons.Default.PhoneAndroid, icon)
    }

    @Test
    fun `TransactionCategory GROCERIES returns ShoppingCart icon`() {
        val icon = TransactionCategory.GROCERIES.getIcon()
        assertEquals(Icons.Default.ShoppingCart, icon)
    }

    @Test
    fun `TransactionCategory OTHER returns MoreHoriz icon`() {
        val icon = TransactionCategory.OTHER.getIcon()
        assertEquals(Icons.Default.MoreHoriz, icon)
    }

    @Test
    fun `TransactionCategory enum has all expected values`() {
        val values = TransactionCategory.values()
        
        assertEquals(6, values.size)
        assertTrue(values.contains(TransactionCategory.BANK))
        assertTrue(values.contains(TransactionCategory.FOOD))
        assertTrue(values.contains(TransactionCategory.SALARY))
        assertTrue(values.contains(TransactionCategory.MOBILE))
        assertTrue(values.contains(TransactionCategory.GROCERIES))
        assertTrue(values.contains(TransactionCategory.OTHER))
    }

    @Test
    fun `TransactionCategory valueOf works correctly`() {
        assertEquals(TransactionCategory.BANK, TransactionCategory.valueOf("BANK"))
        assertEquals(TransactionCategory.FOOD, TransactionCategory.valueOf("FOOD"))
        assertEquals(TransactionCategory.SALARY, TransactionCategory.valueOf("SALARY"))
        assertEquals(TransactionCategory.MOBILE, TransactionCategory.valueOf("MOBILE"))
        assertEquals(TransactionCategory.GROCERIES, TransactionCategory.valueOf("GROCERIES"))
        assertEquals(TransactionCategory.OTHER, TransactionCategory.valueOf("OTHER"))
    }

    @Test
    fun `TransactionType enum has all expected values`() {
        val values = TransactionType.values()
        
        assertEquals(2, values.size)
        assertTrue(values.contains(TransactionType.SPENT))
        assertTrue(values.contains(TransactionType.RECEIVED))
    }

    @Test
    fun `TransactionType valueOf works correctly`() {
        assertEquals(TransactionType.SPENT, TransactionType.valueOf("SPENT"))
        assertEquals(TransactionType.RECEIVED, TransactionType.valueOf("RECEIVED"))
    }

    @Test
    fun `Transaction data class equality works correctly`() {
        val transaction1 = Transaction(
            id = "1",
            title = "Bakhtar Bank – ATM Withdrawal",
            subtitle = "Yesterday • 3:12 PM",
            amount = -1200.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.BANK,
            timestamp = 1700000000000L
        )
        
        val transaction2 = Transaction(
            id = "1",
            title = "Bakhtar Bank – ATM Withdrawal",
            subtitle = "Yesterday • 3:12 PM",
            amount = -1200.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.BANK,
            timestamp = 1700000000000L
        )
        
        assertEquals(transaction1, transaction2)
        assertEquals(transaction1.hashCode(), transaction2.hashCode())
    }

    @Test
    fun `Transaction with different values are not equal`() {
        val transaction1 = Transaction(
            id = "1",
            title = "Bakhtar Bank – ATM Withdrawal",
            subtitle = "Yesterday • 3:12 PM",
            amount = -1200.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.BANK,
            timestamp = 1700000000000L
        )
        
        val transaction2 = Transaction(
            id = "2",
            title = "Food Corner – Lunch",
            subtitle = "Today • 12:45 PM",
            amount = -350.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.FOOD,
            timestamp = 1700000000000L
        )
        
        assertNotEquals(transaction1, transaction2)
    }

    @Test
    fun `Transaction copy function works correctly`() {
        val original = Transaction(
            id = "1",
            title = "Bakhtar Bank – ATM Withdrawal",
            subtitle = "Yesterday • 3:12 PM",
            amount = -1200.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.BANK,
            timestamp = 1700000000000L
        )
        
        val copied = original.copy()
        
        assertEquals(original, copied)
        assertEquals(original.id, copied.id)
        assertEquals(original.title, copied.title)
        assertEquals(original.subtitle, copied.subtitle)
        assertEquals(original.amount, copied.amount, 0.01)
        assertEquals(original.type, copied.type)
        assertEquals(original.category, copied.category)
        assertEquals(original.timestamp, copied.timestamp)
    }

    @Test
    fun `Transaction copy with modified amount works correctly`() {
        val original = Transaction(
            id = "1",
            title = "Bakhtar Bank – ATM Withdrawal",
            subtitle = "Yesterday • 3:12 PM",
            amount = -1200.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.BANK,
            timestamp = 1700000000000L
        )
        
        val modified = original.copy(amount = -1500.0)
        
        assertEquals(-1500.0, modified.amount, 0.01)
        assertEquals(original.id, modified.id)
        assertEquals(original.title, modified.title)
    }

    @Test
    fun `Transaction handles positive amount for RECEIVED type`() {
        val transaction = Transaction(
            id = "3",
            title = "Salary – Deposit",
            subtitle = "3 days ago • 9:00 AM",
            amount = 15000.0,
            type = TransactionType.RECEIVED,
            category = TransactionCategory.SALARY,
            timestamp = 1700000000000L
        )
        
        assertEquals(15000.0, transaction.amount, 0.01)
        assertEquals(TransactionType.RECEIVED, transaction.type)
        assertEquals(TransactionCategory.SALARY, transaction.category)
    }

    @Test
    fun `Transaction handles negative amount for SPENT type`() {
        val transaction = Transaction(
            id = "2",
            title = "Food Corner – Lunch",
            subtitle = "Today • 12:45 PM",
            amount = -350.0,
            type = TransactionType.SPENT,
            category = TransactionCategory.FOOD,
            timestamp = 1700000000000L
        )
        
        assertEquals(-350.0, transaction.amount, 0.01)
        assertEquals(TransactionType.SPENT, transaction.type)
        assertEquals(TransactionCategory.FOOD, transaction.category)
    }

    @Test
    fun `all TransactionCategory values return non-null icons`() {
        TransactionCategory.values().forEach { category ->
            val icon = category.getIcon()
            assertNotNull("Icon for $category should not be null", icon)
        }
    }
}
