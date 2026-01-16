package com.example.railchatbot.domain.model

import java.util.UUID

/**
 * Represents a message in the chat conversation
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null
)

/**
 * Role of the message sender
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

/**
 * Represents a tool call made by the assistant
 */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?>,
    val result: String? = null,
    val status: ToolCallStatus = ToolCallStatus.PENDING
)

/**
 * Status of a tool call
 */
enum class ToolCallStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED
}
