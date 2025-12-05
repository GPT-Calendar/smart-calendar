package com.example.voicereminder.sms

import android.content.Context
import android.util.Log
import com.example.voicereminder.data.ai.AIMessage
import com.example.voicereminder.data.ai.AIServiceFactory
import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.TransactionType
import com.example.voicereminder.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.Date

/**
 * Service that uses AI to extract structured finance data from SMS messages.
 * Sends SMS content to configured AI provider and parses the response into
 * a FinanceTransaction entity for database storage.
 */
class SmsFinanceExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "SmsFinanceExtractor"
        
        private const val EXTRACTION_PROMPT = """You are a financial data extraction assistant. Extract transaction details from the following bank SMS message and return ONLY a valid JSON object with no additional text.

Required JSON format:
{
    "amount": <number>,
    "currency": "<string>",
    "type": "<CREDIT or DEBIT>",
    "description": "<string>",
    "category": "<string>",
    "fromAccount": "<string or null>",
    "toAccount": "<string or null>",
    "balanceAfter": <number or null>,
    "merchant": "<string or null>",
    "isTransaction": <boolean>
}

Rules:
- "amount" must be a positive number (no currency symbols)
- "type" must be exactly "CREDIT" for money received/deposited or "DEBIT" for money spent/withdrawn
- "category" should be one of: FOOD, TRANSPORT, SHOPPING, BILLS, SALARY, TRANSFER, ATM, OTHER
- "isTransaction" should be false if the SMS is not a financial transaction (e.g., OTP, promotional)
- If you cannot extract a field, use null
- Return ONLY the JSON object, no explanations

SMS Message:
"""
    }
    
    private val settingsRepository = SettingsRepository(context)

    /**
     * Extract finance data from SMS using AI
     * @param bankName The name of the bank (from configured pattern)
     * @param phoneNumber The sender's phone number
     * @param smsContent The SMS message content
     * @param timestamp When the SMS was received
     * @return FinanceTransaction if extraction successful, null otherwise
     */
    suspend fun extractFromSms(
        bankName: String,
        phoneNumber: String,
        smsContent: String,
        timestamp: Date
    ): FinanceTransaction? {
        return try {
            Log.d(TAG, "Starting AI extraction for SMS from $bankName")
            
            // Get AI settings
            val settings = settingsRepository.aiSettings.first()
            val aiService = AIServiceFactory.create(settings.provider)
            
            // Build the extraction prompt
            val messages = listOf(
                AIMessage(
                    role = "system",
                    content = "You are a precise financial data extraction assistant. Always respond with valid JSON only."
                ),
                AIMessage(
                    role = "user",
                    content = "$EXTRACTION_PROMPT$smsContent"
                )
            )
            
            // Collect AI response
            val responseBuilder = StringBuilder()
            aiService.chatStream(messages, settings).collect { chunk ->
                responseBuilder.append(chunk)
            }
            
            val response = responseBuilder.toString().trim()
            Log.d(TAG, "AI response: $response")
            
            // Parse JSON response
            parseAiResponse(response, bankName, phoneNumber, smsContent, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting finance data from SMS", e)
            null
        }
    }
    
    /**
     * Parse AI response JSON into FinanceTransaction
     */
    private fun parseAiResponse(
        response: String,
        bankName: String,
        phoneNumber: String,
        smsContent: String,
        timestamp: Date
    ): FinanceTransaction? {
        return try {
            // Extract JSON from response (handle cases where AI adds extra text)
            val jsonString = extractJsonFromResponse(response)
            if (jsonString == null) {
                Log.w(TAG, "Could not extract JSON from AI response")
                return null
            }
            
            val json = JSONObject(jsonString)
            
            // Check if this is actually a transaction
            val isTransaction = json.optBoolean("isTransaction", true)
            if (!isTransaction) {
                Log.d(TAG, "SMS is not a financial transaction, skipping")
                return null
            }
            
            val amount = json.optDouble("amount", 0.0)
            if (amount <= 0) {
                Log.w(TAG, "Invalid amount extracted: $amount")
                return null
            }
            
            val typeStr = json.optString("type", "DEBIT")
            val transactionType = when (typeStr.uppercase()) {
                "CREDIT" -> TransactionType.CREDIT
                "DEBIT" -> TransactionType.DEBIT
                else -> TransactionType.DEBIT
            }
            
            val currency = json.optString("currency", "AFN").ifEmpty { "AFN" }
            val description = json.optString("description", "Bank transaction").ifEmpty { 
                "Transaction from $bankName" 
            }
            val category = json.optString("category", "OTHER").ifEmpty { "OTHER" }
            val fromAccount = json.optString("fromAccount", null)?.takeIf { it.isNotEmpty() && it != "null" }
            val toAccount = json.optString("toAccount", null)?.takeIf { it.isNotEmpty() && it != "null" }
            val balanceAfter = json.optDouble("balanceAfter", Double.NaN).takeIf { !it.isNaN() }
            
            Log.d(TAG, "Successfully extracted: $amount $currency ($transactionType) - $description")
            
            FinanceTransaction(
                amount = amount,
                currency = currency,
                description = description,
                fromAccount = fromAccount,
                toAccount = toAccount,
                transactionType = transactionType,
                bankName = bankName,
                phoneNumber = phoneNumber,
                smsContent = smsContent,
                timestamp = timestamp,
                category = category,
                balanceAfter = balanceAfter,
                isManual = false,
                isEdited = false,
                notes = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response JSON", e)
            null
        }
    }
    
    /**
     * Extract JSON object from AI response that might contain extra text
     */
    private fun extractJsonFromResponse(response: String): String? {
        // Try to find JSON object in response
        val startIndex = response.indexOf('{')
        val endIndex = response.lastIndexOf('}')
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1)
        }
        
        return null
    }
}
