package com.example.railchatbot.domain.orchestrator

import android.util.Log
import com.example.railchatbot.data.remote.ai.LlmClient
import com.example.railchatbot.data.remote.ai.model.ConversationMessage
import com.example.railchatbot.data.remote.ai.model.FunctionDefinition
import com.example.railchatbot.data.remote.ai.model.LlmResponse
import com.example.railchatbot.data.remote.ai.model.LlmToolCall
import com.example.railchatbot.data.remote.mcp.McpClientWrapper
import com.example.railchatbot.data.remote.mcp.model.McpTool
import com.example.railchatbot.domain.model.ChatMessage
import com.example.railchatbot.domain.model.MessageRole
import com.example.railchatbot.domain.model.ToolCall
import com.example.railchatbot.domain.model.ToolCallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Events emitted during chat orchestration
 */
sealed class OrchestratorEvent {
    data class Thinking(val message: String) : OrchestratorEvent()
    data class ToolCallStarted(val toolName: String, val arguments: Map<String, Any?>) : OrchestratorEvent()
    data class ToolCallCompleted(val toolName: String, val result: String) : OrchestratorEvent()
    data class ToolCallFailed(val toolName: String, val error: String) : OrchestratorEvent()
    data class ResponseComplete(val message: ChatMessage) : OrchestratorEvent()
    data class Error(val message: String, val cause: Throwable? = null) : OrchestratorEvent()
}

/**
 * Orchestrates the chat flow between user, LLM, and MCP tools.
 * Implements the tool calling loop:
 * 1. Send user message + available tools to LLM
 * 2. If LLM requests tool calls -> execute via MCP -> return results to LLM
 * 3. Repeat until LLM returns final text response
 */
@Singleton
class ChatOrchestrator @Inject constructor(
    private val llmClient: LlmClient,
    private val mcpClient: McpClientWrapper
) {
    companion object {
        private const val TAG = "ChatOrchestrator"
        private const val MAX_TOOL_ITERATIONS = 10

        private val SYSTEM_PROMPT = """
            You are a helpful Indian Railways assistant that can help users with train-related queries.
            You have access to IRCTC tools that can:
            - Search for trains between stations
            - Check PNR status
            - Get train schedules and availability
            - Find train routes and timings

            When users ask about trains, use the available tools to get accurate information.
            Be concise and helpful in your responses.
            If a tool call fails, inform the user and suggest alternatives.

            Always respond in a friendly, professional manner.
        """.trimIndent()
    }

    private var cachedTools: List<McpTool>? = null

    /**
     * Process a user message and return a flow of orchestration events
     */
    fun processMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): Flow<OrchestratorEvent> = flow {
        try {
            emit(OrchestratorEvent.Thinking("Processing your request..."))

            // Ensure MCP client is initialized
            mcpClient.initialize().getOrElse {
                emit(OrchestratorEvent.Error("Failed to connect to train service: ${it.message}", it))
                return@flow
            }

            // Get available tools
            val tools = getTools()
            val functionDefinitions = tools.map { it.toFunctionDefinition() }

            // Build conversation messages for LLM
            val messages = buildConversationMessages(userMessage, conversationHistory)

            // Start the tool calling loop
            var iterations = 0
            var currentMessages = messages
            var finalResponse: String? = null

            while (iterations < MAX_TOOL_ITERATIONS && finalResponse == null) {
                iterations++
                Log.d(TAG, "Tool calling iteration $iterations")

                emit(OrchestratorEvent.Thinking("Analyzing your request..."))

                val llmResponse = llmClient.chat(currentMessages, functionDefinitions)

                when (llmResponse) {
                    is LlmResponse.TextResponse -> {
                        finalResponse = llmResponse.text
                    }

                    is LlmResponse.ToolCallsResponse -> {
                        // Execute each tool call
                        val toolResults = mutableListOf<Pair<LlmToolCall, String>>()

                        for (toolCall in llmResponse.toolCalls) {
                            emit(OrchestratorEvent.ToolCallStarted(toolCall.name, toolCall.arguments))

                            val result = executeToolCall(toolCall)

                            if (result.isSuccess) {
                                val resultText = result.getOrThrow()
                                emit(OrchestratorEvent.ToolCallCompleted(toolCall.name, resultText))
                                toolResults.add(toolCall to resultText)
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                                emit(OrchestratorEvent.ToolCallFailed(toolCall.name, error))
                                toolResults.add(toolCall to "Error: $error")
                            }
                        }

                        // Add assistant message with tool calls
                        currentMessages = currentMessages + ConversationMessage(
                            role = "assistant",
                            content = null,
                            toolCalls = llmResponse.toolCalls
                        )

                        // Add tool results
                        for ((toolCall, result) in toolResults) {
                            currentMessages = currentMessages + ConversationMessage(
                                role = "tool",
                                content = result,
                                toolCallId = toolCall.id
                            )
                        }
                    }

                    is LlmResponse.Error -> {
                        emit(OrchestratorEvent.Error(llmResponse.message, llmResponse.cause))
                        return@flow
                    }
                }
            }

            if (finalResponse != null) {
                emit(
                    OrchestratorEvent.ResponseComplete(
                        ChatMessage(
                            role = MessageRole.ASSISTANT,
                            content = finalResponse
                        )
                    )
                )
            } else {
                emit(OrchestratorEvent.Error("Maximum iterations reached without response"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
            emit(OrchestratorEvent.Error("An error occurred: ${e.message}", e))
        }
    }

    /**
     * Initialize the orchestrator and fetch available tools
     */
    suspend fun initialize(): Result<Unit> {
        return mcpClient.initialize().map {
            cachedTools = mcpClient.listTools().getOrNull()
            Log.d(TAG, "Initialized with ${cachedTools?.size ?: 0} tools")
        }
    }

    /**
     * Check if the orchestrator is ready
     */
    fun isReady(): Boolean = mcpClient.isConnected()

    private suspend fun getTools(): List<McpTool> {
        if (cachedTools == null) {
            cachedTools = mcpClient.listTools().getOrNull() ?: emptyList()
        }
        return cachedTools ?: emptyList()
    }

    private fun buildConversationMessages(
        userMessage: String,
        history: List<ChatMessage>
    ): List<ConversationMessage> {
        val messages = mutableListOf<ConversationMessage>()

        // Add system prompt
        messages.add(ConversationMessage(role = "system", content = SYSTEM_PROMPT))

        // Add conversation history
        for (msg in history) {
            messages.add(
                ConversationMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                        MessageRole.TOOL -> "tool"
                    },
                    content = msg.content,
                    toolCallId = msg.toolCallId,
                    toolCalls = msg.toolCalls?.map { tc ->
                        LlmToolCall(tc.id, tc.name, tc.arguments)
                    }
                )
            )
        }

        // Add current user message
        messages.add(ConversationMessage(role = "user", content = userMessage))

        return messages
    }

    private suspend fun executeToolCall(toolCall: LlmToolCall): Result<String> {
        return try {
            val result = mcpClient.callTool(toolCall.name, toolCall.arguments)

            result.map { mcpResult ->
                mcpResult.content.mapNotNull { it.text }.joinToString("\n")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun McpTool.toFunctionDefinition(): FunctionDefinition {
        return FunctionDefinition(
            name = name,
            description = description ?: "No description available",
            parameters = inputSchema ?: kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("object"))
                put("properties", kotlinx.serialization.json.buildJsonObject { })
            }
        )
    }
}
