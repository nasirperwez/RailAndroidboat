package com.example.railchatbot.presentation.chat

import com.example.railchatbot.domain.model.ChatMessage

/**
 * UI state for the chat screen
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = true,
    val currentThinking: String? = null,
    val currentToolCall: CurrentToolCall? = null,
    val error: String? = null
)

/**
 * Represents a tool call in progress
 */
data class CurrentToolCall(
    val toolName: String,
    val status: ToolCallUiStatus
)

enum class ToolCallUiStatus {
    EXECUTING,
    COMPLETED,
    FAILED
}

/**
 * User intents for the chat screen
 */
sealed class ChatIntent {
    data class UpdateInput(val text: String) : ChatIntent()
    data class SendMessage(val text: String) : ChatIntent()
    object Retry : ChatIntent()
    object ClearError : ChatIntent()
    object Reconnect : ChatIntent()
}
