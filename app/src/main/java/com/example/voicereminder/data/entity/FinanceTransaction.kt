package com.example.voicereminder.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "finance_transactions")
data class FinanceTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val currency: String = "AFN", // Afghan Afghani as default
    val description: String,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val transactionType: TransactionType,
    val bankName: String,
    val phoneNumber: String,
    val smsContent: String,
    val timestamp: Date,
    val category: String? = null,
    val balanceAfter: Double? = null,
    // New fields for enhanced user control
    val isManual: Boolean = false,      // True if manually entered by user
    val isEdited: Boolean = false,      // True if user has edited this transaction
    val notes: String? = null,          // Optional user notes
    val originalAmount: Double? = null  // Original amount before edit (for audit trail)
)

/**
 * Represents a configured SMS source for automatic finance tracking.
 * When SMS is received from this phone number, it will be sent to AI for extraction.
 */
@Entity(
    tableName = "sms_sources",
    indices = [Index(value = ["phoneNumber"])]
)
data class SmsSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,              // Display name (e.g., "Afghanistan International Bank")
    val phoneNumber: String,       // SMS sender number to track
    val isActive: Boolean = true,  // Whether tracking is enabled for this source
    val description: String? = null // Optional description/notes
)

// Keep old name as typealias for backward compatibility during migration
@Deprecated("Use SmsSource instead", ReplaceWith("SmsSource"))
typealias BankSmsPattern = SmsSource