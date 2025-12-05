package com.example.voicereminder.data

import android.content.Context
import com.example.voicereminder.data.dao.ChatMessageDao
import com.example.voicereminder.data.entity.ChatMessageEntity
import com.example.voicereminder.data.entity.toChatMessage
import com.example.voicereminder.data.entity.toEntity
import com.example.voicereminder.presentation.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for managing chat history persistence
 */
class ChatHistoryRepository(context: Context) {
    
    private val database = ReminderDatabase.getDatabase(context)
    private val chatMessageDao: ChatMessageDao = database.chatMessageDao()
    
    // Current session ID for grouping messages
    private var currentSessionId: String = generateSessionId()
    
    companion object {
        private const val MAX_MESSAGES = 500 // Keep last 500 messages
        
        @Volatile
        private var INSTANCE: ChatHistoryRepository? = null
        
        fun getInstance(context: Context): ChatHistoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatHistoryRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        private fun generateSessionId(): String {
            return UUID.randomUUID().toString()
        }
    }
    
    /**
     * Start a new chat session
     */
    fun startNewSession() {
        currentSessionId = generateSessionId()
    }
    
    /**
     * Get current session ID
     */
    fun getCurrentSessionId(): String = currentSessionId
    
    /**
     * Save a message to history
     */
    suspend fun saveMessage(message: ChatMessage, status: String = "SENT"): Long {
        val entity = message.toEntity(sessionId = currentSessionId, status = status)
        return chatMessageDao.insert(entity)
    }
    
    /**
     * Save multiple messages
     */
    suspend fun saveMessages(messages: List<ChatMessage>) {
        val entities = messages.map { it.toEntity(sessionId = currentSessionId) }
        chatMessageDao.insertAll(entities)
    }
    
    /**
     * Get all messages as Flow
     */
    fun getAllMessagesFlow(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages().map { entities ->
            entities.map { it.toChatMessage() }
        }
    }
    
    /**
     * Get all messages as list
     */
    suspend fun getAllMessages(): List<ChatMessage> {
        return chatMessageDao.getAllMessagesList().map { it.toChatMessage() }
    }
    
    /**
     * Get recent messages
     */
    suspend fun getRecentMessages(limit: Int = 50): List<ChatMessage> {
        return chatMessageDao.getRecentMessages(limit)
            .reversed() // Reverse to get chronological order
            .map { it.toChatMessage() }
    }
    
    /**
     * Search messages
     */
    suspend fun searchMessages(query: String): List<ChatMessage> {
        return chatMessageDao.searchMessages(query).map { it.toChatMessage() }
    }
    
    /**
     * Get messages in date range
     */
    suspend fun getMessagesInRange(startTime: Long, endTime: Long): List<ChatMessage> {
        return chatMessageDao.getMessagesInRange(startTime, endTime).map { it.toChatMessage() }
    }
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: Int) {
        chatMessageDao.deleteById(messageId.toLong())
    }
    
    /**
     * Clear all chat history
     */
    suspend fun clearHistory() {
        chatMessageDao.clearAll()
        startNewSession()
    }
    
    /**
     * Update message status
     */
    suspend fun updateMessageStatus(messageId: Long, status: String) {
        chatMessageDao.updateStatus(messageId, status)
    }
    
    /**
     * Get queued offline messages
     */
    suspend fun getQueuedMessages(): List<ChatMessageEntity> {
        return chatMessageDao.getQueuedMessages()
    }
    
    /**
     * Cleanup old messages to prevent database bloat
     */
    suspend fun cleanupOldMessages() {
        val count = chatMessageDao.getMessageCount()
        if (count > MAX_MESSAGES) {
            chatMessageDao.deleteOldMessages(MAX_MESSAGES)
        }
    }
    
    /**
     * Export conversation as text
     */
    suspend fun exportAsText(): String {
        val messages = chatMessageDao.getAllMessagesList()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        
        return buildString {
            appendLine("=== Chat History Export ===")
            appendLine("Exported: ${dateFormat.format(java.util.Date())}")
            appendLine()
            
            messages.forEach { message ->
                val sender = if (message.isUser) "You" else "Assistant"
                val time = dateFormat.format(java.util.Date(message.timestamp))
                appendLine("[$time] $sender:")
                appendLine(message.text)
                appendLine()
            }
        }
    }
    
    /**
     * Export conversation as JSON
     */
    suspend fun exportAsJson(): String {
        val messages = chatMessageDao.getAllMessagesList()
        
        val jsonArray = org.json.JSONArray()
        messages.forEach { message ->
            val jsonObject = org.json.JSONObject().apply {
                put("id", message.id)
                put("text", message.text)
                put("isUser", message.isUser)
                put("timestamp", message.timestamp)
                put("messageType", message.messageType)
                put("status", message.status)
            }
            jsonArray.put(jsonObject)
        }
        
        return org.json.JSONObject().apply {
            put("exportDate", System.currentTimeMillis())
            put("messageCount", messages.size)
            put("messages", jsonArray)
        }.toString(2)
    }
}
