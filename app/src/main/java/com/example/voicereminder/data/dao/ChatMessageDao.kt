package com.example.voicereminder.data.dao

import androidx.room.*
import com.example.voicereminder.data.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for chat message persistence
 */
@Dao
interface ChatMessageDao {
    
    /**
     * Insert a new message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long
    
    /**
     * Insert multiple messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)
    
    /**
     * Update an existing message
     */
    @Update
    suspend fun update(message: ChatMessageEntity)
    
    /**
     * Delete a message
     */
    @Delete
    suspend fun delete(message: ChatMessageEntity)
    
    /**
     * Delete message by ID
     */
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)
    
    /**
     * Get all messages ordered by timestamp
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>
    
    /**
     * Get all messages as a list (non-flow)
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesList(): List<ChatMessageEntity>
    
    /**
     * Get messages by session ID
     */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: String): Flow<List<ChatMessageEntity>>
    
    /**
     * Get recent messages (last N)
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>
    
    /**
     * Search messages by text
     */
    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMessages(query: String): List<ChatMessageEntity>
    
    /**
     * Get messages by type
     */
    @Query("SELECT * FROM chat_messages WHERE messageType = :type ORDER BY timestamp DESC")
    suspend fun getMessagesByType(type: String): List<ChatMessageEntity>
    
    /**
     * Get messages in date range
     */
    @Query("SELECT * FROM chat_messages WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getMessagesInRange(startTime: Long, endTime: Long): List<ChatMessageEntity>
    
    /**
     * Get queued offline messages
     */
    @Query("SELECT * FROM chat_messages WHERE status = 'QUEUED_OFFLINE' ORDER BY timestamp ASC")
    suspend fun getQueuedMessages(): List<ChatMessageEntity>
    
    /**
     * Update message status
     */
    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: Long, status: String)
    
    /**
     * Clear all messages
     */
    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
    
    /**
     * Get message count
     */
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int
    
    /**
     * Delete old messages (keep last N)
     */
    @Query("""
        DELETE FROM chat_messages 
        WHERE id NOT IN (
            SELECT id FROM chat_messages ORDER BY timestamp DESC LIMIT :keepCount
        )
    """)
    suspend fun deleteOldMessages(keepCount: Int)
}
