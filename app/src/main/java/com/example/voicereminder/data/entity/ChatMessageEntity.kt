package com.example.voicereminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting chat messages
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val messageType: String = "TEXT", // TEXT, REMINDER_CARD, TASK_CARD, etc.
    val status: String = "SENT", // SENDING, SENT, DELIVERED, ERROR, QUEUED_OFFLINE
    val richContentJson: String? = null, // JSON serialized rich content
    val sessionId: String? = null // Group messages by session
)

/**
 * Extension to convert entity to domain model
 */
fun ChatMessageEntity.toChatMessage(): com.example.voicereminder.presentation.chat.ChatMessage {
    return com.example.voicereminder.presentation.chat.ChatMessage(
        id = this.id.toInt(),
        text = this.text,
        isUser = this.isUser,
        timestamp = this.timestamp
    )
}

/**
 * Extension to convert domain model to entity
 */
fun com.example.voicereminder.presentation.chat.ChatMessage.toEntity(
    sessionId: String? = null,
    status: String = "SENT"
): ChatMessageEntity {
    return ChatMessageEntity(
        id = if (this.id > 0) this.id.toLong() else 0,
        text = this.text,
        isUser = this.isUser,
        timestamp = this.timestamp,
        sessionId = sessionId,
        status = status
    )
}
