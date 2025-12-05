package com.example.voicereminder.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.voicereminder.R
import com.example.voicereminder.data.FinanceDatabase
import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.SmsSource
import com.example.voicereminder.data.entity.TransactionType
import com.example.voicereminder.data.settings.AppSettingsRepository
import com.example.voicereminder.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

/**
 * BroadcastReceiver that listens for incoming SMS messages.
 * When an SMS is received from a configured bank number, it:
 * 1. Sends the SMS content to AI for extraction
 * 2. Gets structured finance data back
 * 3. Saves the transaction to the database
 */
class BankSmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BankSmsReceiver"
        private const val CHANNEL_ID = "finance_tracking_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val bundle = intent.extras
            if (bundle != null) {
                val format = bundle.getString("format")
                val pdus = bundle.get("pdus") as? Array<*> ?: return

                for (pdu in pdus) {
                    val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }

                    if (smsMessage != null) {
                        val phoneNumber = smsMessage.displayOriginatingAddress ?: continue
                        val messageBody = smsMessage.messageBody ?: continue
                        val timestamp = Date(smsMessage.timestampMillis)

                        Log.d(TAG, "SMS received from: $phoneNumber")
                        
                        // Process the SMS to see if it's from a bank we're tracking
                        CoroutineScope(Dispatchers.IO).launch {
                            processBankSms(context, phoneNumber, messageBody, timestamp)
                        }
                    }
                }
            }
        }
    }

    private suspend fun processBankSms(context: Context, phoneNumber: String, message: String, timestamp: Date) {
        try {
            // Check if SMS tracking is enabled in settings
            val appSettings = AppSettingsRepository.getInstance(context).appSettings.first()
            if (!appSettings.smsTrackingEnabled) {
                Log.d(TAG, "SMS tracking is disabled, skipping")
                return
            }
            
            // Get the database instance
            val database = FinanceDatabase.getDatabase(context)

            // Try to find matching SMS source with flexible phone number matching
            val smsSource = findMatchingSmsSource(database, phoneNumber)
            
            Log.d(TAG, "Looking for SMS source matching: $phoneNumber, found: ${smsSource?.name ?: "none"}")

            if (smsSource != null && smsSource.isActive) {
                Log.d(TAG, "SMS matches tracked source: ${smsSource.name}")
                
                // Use AI to extract transaction details from the SMS
                val extractor = SmsFinanceExtractor(context)
                val transaction = extractor.extractFromSms(
                    bankName = smsSource.name,
                    phoneNumber = phoneNumber,
                    smsContent = message,
                    timestamp = timestamp
                )

                if (transaction != null) {
                    // Save the transaction to the database
                    database.financeTransactionDao().insertTransaction(transaction)
                    Log.d(TAG, "✓ AI extracted and saved transaction: ${transaction.description} - ${transaction.amount} ${transaction.currency}")
                    
                    // Show notification if finance notifications are enabled
                    if (appSettings.financeNotifications) {
                        showTransactionNotification(context, transaction)
                    }
                } else {
                    Log.d(TAG, "AI could not extract transaction from SMS, trying fallback regex")
                    // Fallback to regex extraction if AI fails
                    val fallbackTransaction = extractWithRegex(smsSource.name, phoneNumber, message, timestamp)
                    if (fallbackTransaction != null) {
                        database.financeTransactionDao().insertTransaction(fallbackTransaction)
                        Log.d(TAG, "✓ Regex fallback saved transaction: ${fallbackTransaction.description} - ${fallbackTransaction.amount}")
                        
                        // Show notification if finance notifications are enabled
                        if (appSettings.financeNotifications) {
                            showTransactionNotification(context, fallbackTransaction)
                        }
                    }
                }
            } else {
                Log.d(TAG, "SMS from $phoneNumber is not from a tracked bank")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing bank SMS", e)
        }
    }

    /**
     * Show notification when a transaction is auto-extracted
     */
    private fun showTransactionNotification(context: Context, transaction: FinanceTransaction) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create notification channel for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Finance Tracking",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for auto-tracked financial transactions"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // Create intent to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "finance")
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val typeEmoji = if (transaction.transactionType == TransactionType.CREDIT) "💰" else "💸"
            val typeText = if (transaction.transactionType == TransactionType.CREDIT) "Received" else "Spent"
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("$typeEmoji Transaction Tracked")
                .setContentText("$typeText ${transaction.currency} ${String.format("%.2f", transaction.amount)} - ${transaction.bankName}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing transaction notification", e)
        }
    }
    
    /**
     * Fallback regex extraction when AI is unavailable or fails
     */
    private fun extractWithRegex(bankName: String, phoneNumber: String, message: String, timestamp: Date): FinanceTransaction? {
        // Common patterns for bank SMS
        val amountPatterns = listOf(
            Regex("""(?:ETB|AFN|USD|EUR|GBP|Rs\.?|Ksh|GHS|\$)\s*([0-9,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""([0-9,]+\.?\d*)\s*(?:ETB|AFN|USD|EUR|GBP|birr|afghani)""", RegexOption.IGNORE_CASE),
        )
        
        for (pattern in amountPatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val amountStr = match.groups[1]?.value?.replace(",", "") ?: continue
                val amount = amountStr.toDoubleOrNull() ?: continue
                
                if (amount <= 0) continue
                
                // Determine transaction type based on keywords
                val isDebit = listOf("debited", "withdrawn", "deducted", "spent", "used", "paid", "sent", "transfer out")
                    .any { message.contains(it, ignoreCase = true) }
                
                val isCredit = listOf("credited", "deposited", "received", "added", "income", "salary", "transfer in")
                    .any { message.contains(it, ignoreCase = true) }
                
                val transactionType = when {
                    isDebit -> TransactionType.DEBIT
                    isCredit -> TransactionType.CREDIT
                    else -> TransactionType.DEBIT
                }
                
                return FinanceTransaction(
                    amount = amount,
                    currency = "AFN",
                    description = "Transaction from $bankName",
                    transactionType = transactionType,
                    bankName = bankName,
                    phoneNumber = phoneNumber,
                    smsContent = message,
                    timestamp = timestamp,
                    category = "OTHER",
                    isManual = false,
                    isEdited = false
                )
            }
        }
        
        return null
    }
    
    /**
     * Find matching SMS source with flexible phone number matching.
     * Handles different phone number formats:
     * - With/without country code (+93, 93, 0)
     * - With/without leading zeros
     * - Alphanumeric sender IDs (e.g., "MYBANK")
     */
    private suspend fun findMatchingSmsSource(database: FinanceDatabase, incomingNumber: String): SmsSource? {
        // First try exact match
        val exactMatch = database.smsSourceDao().getSourceByPhoneNumber(incomingNumber)
        if (exactMatch != null) {
            Log.d(TAG, "Found exact match for $incomingNumber")
            return exactMatch
        }
        
        // Get all sources and try flexible matching
        val allSources = database.smsSourceDao().getAllSources()
        
        // Normalize the incoming number (remove non-alphanumeric except +)
        val normalizedIncoming = normalizePhoneNumber(incomingNumber)
        
        for (source in allSources) {
            val normalizedSource = normalizePhoneNumber(source.phoneNumber)
            
            // Check various matching conditions
            val isMatch = when {
                // Exact normalized match
                normalizedIncoming == normalizedSource -> true
                // Incoming ends with source (source might be without country code)
                normalizedIncoming.endsWith(normalizedSource) && normalizedSource.length >= 7 -> true
                // Source ends with incoming (incoming might be without country code)
                normalizedSource.endsWith(normalizedIncoming) && normalizedIncoming.length >= 7 -> true
                // Case-insensitive match for alphanumeric sender IDs
                normalizedIncoming.equals(normalizedSource, ignoreCase = true) -> true
                // Contains match for short codes or alphanumeric IDs
                normalizedIncoming.contains(normalizedSource, ignoreCase = true) && normalizedSource.length >= 4 -> true
                normalizedSource.contains(normalizedIncoming, ignoreCase = true) && normalizedIncoming.length >= 4 -> true
                else -> false
            }
            
            if (isMatch) {
                Log.d(TAG, "Found flexible match: incoming=$incomingNumber, source=${source.phoneNumber}")
                return source
            }
        }
        
        Log.d(TAG, "No match found for $incomingNumber among ${allSources.size} sources")
        return null
    }
    
    /**
     * Normalize phone number for comparison.
     * Removes spaces, dashes, parentheses, and handles country codes.
     */
    private fun normalizePhoneNumber(number: String): String {
        // Remove common separators and whitespace
        var normalized = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
        
        // Remove leading + if present
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1)
        }
        
        // Remove leading zeros (common in local formats)
        while (normalized.startsWith("0") && normalized.length > 7) {
            normalized = normalized.substring(1)
        }
        
        return normalized
    }
}