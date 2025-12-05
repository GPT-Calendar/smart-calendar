package com.example.voicereminder.domain.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Transaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val timestamp: Long,
    val isManual: Boolean = false,      // True if manually entered by user
    val isEdited: Boolean = false,      // True if user has edited this transaction
    val notes: String? = null           // Optional user notes
)

enum class TransactionType {
    SPENT, RECEIVED;
    
    /**
     * Returns user-friendly display name for the transaction type
     */
    fun getDisplayName(): String {
        return when (this) {
            SPENT -> "Withdrawal"
            RECEIVED -> "Deposit"
        }
    }
    
    /**
     * Returns a short label for compact displays
     */
    fun getShortLabel(): String {
        return when (this) {
            SPENT -> "Out"
            RECEIVED -> "In"
        }
    }
}

enum class TransactionCategory {
    BANK, FOOD, SALARY, MOBILE, GROCERIES, SHOPPING, TRANSPORT, BILLS, ENTERTAINMENT, OTHER;

    fun getIcon(): ImageVector {
        return when (this) {
            BANK -> Icons.Default.AccountBalance
            FOOD -> Icons.Default.Restaurant
            SALARY -> Icons.Default.AccountBalanceWallet
            MOBILE -> Icons.Default.PhoneAndroid
            GROCERIES -> Icons.Default.ShoppingCart
            SHOPPING -> Icons.Default.ShoppingCart
            TRANSPORT -> Icons.Default.DirectionsCar
            BILLS -> Icons.Default.Description
            ENTERTAINMENT -> Icons.Default.Tv
            OTHER -> Icons.Default.MoreHoriz
        }
    }
}
