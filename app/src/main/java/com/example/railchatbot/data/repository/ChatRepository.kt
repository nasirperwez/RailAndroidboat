package com.example.railchatbot.data.repository

import com.example.railchatbot.domain.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing chat messages
 */
@Singleton
class ChatRepository @Inject constructor() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /**
     * Add a message to the conversation
     */
    fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    /**
     * Get all messages in the conversation
     */
    fun getMessages(): List<ChatMessage> = _messages.value

    /**
     * Clear all messages
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * Update a specific message
     */
    fun updateMessage(messageId: String, update: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) update(msg) else msg
        }
    }
}
