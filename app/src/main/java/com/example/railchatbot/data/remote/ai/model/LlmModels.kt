package com.example.railchatbot.data.remote.ai.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents a function/tool definition for LLM
 */
@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

/**
 * Represents a tool call requested by the LLM
 */
data class LlmToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?>
)

/**
 * Response from the LLM
 */
sealed class LlmResponse {
    /**
     * LLM returned a text message
     */
    data class TextResponse(val text: String) : LlmResponse()

    /**
     * LLM requested tool calls
     */
    data class ToolCallsResponse(val toolCalls: List<LlmToolCall>) : LlmResponse()

    /**
     * LLM call failed
     */
    data class Error(val message: String, val cause: Throwable? = null) : LlmResponse()
}

/**
 * Message in a conversation
 */
data class ConversationMessage(
    val role: String, // "system", "user", "assistant", "tool"
    val content: String?,
    val toolCallId: String? = null,
    val toolCalls: List<LlmToolCall>? = null
)
