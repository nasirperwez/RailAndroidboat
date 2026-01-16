package com.example.railchatbot.domain.usecase

import com.example.railchatbot.data.remote.backend.BackendClient
import com.example.railchatbot.data.remote.backend.BackendChatMessage
import com.example.railchatbot.data.remote.backend.BackendEvent
import com.example.railchatbot.domain.model.ChatMessage
import com.example.railchatbot.domain.model.MessageRole
import com.example.railchatbot.domain.model.OrchestratorEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for sending a message to the backend and receiving events.
 */
class SendMessageUseCase @Inject constructor(
    private val backendClient: BackendClient
) {
    /**
     * Send a message and receive a flow of orchestrator events.
     */
    operator fun invoke(
        message: String,
        conversationHistory: List<ChatMessage>
    ): Flow<OrchestratorEvent> {
        // Convert history to backend format
        val history = conversationHistory.map { msg ->
            BackendChatMessage(
                role = when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                    MessageRole.TOOL -> "tool"
                },
                content = msg.content
            )
        }

        // Send message and map backend events to orchestrator events
        return backendClient.sendMessage(message, history)
            .map { event -> event.toOrchestratorEvent() }
            .catch { e ->
                emit(OrchestratorEvent.Error("Connection error: ${e.message}", e))
            }
    }

    /**
     * Map BackendEvent to OrchestratorEvent.
     */
    private fun BackendEvent.toOrchestratorEvent(): OrchestratorEvent {
        return when (type) {
            BackendEvent.TYPE_THINKING -> {
                OrchestratorEvent.Thinking(text ?: "Processing...")
            }

            BackendEvent.TYPE_TOOL_START -> {
                @Suppress("UNCHECKED_CAST")
                OrchestratorEvent.ToolCallStarted(
                    toolName = name ?: "Unknown",
                    arguments = (args ?: emptyMap()) as Map<String, Any?>
                )
            }

            BackendEvent.TYPE_TOOL_COMPLETE -> {
                OrchestratorEvent.ToolCallCompleted(
                    toolName = name ?: "Unknown",
                    result = result ?: ""
                )
            }

            BackendEvent.TYPE_TOOL_ERROR -> {
                OrchestratorEvent.ToolCallFailed(
                    toolName = name ?: "Unknown",
                    error = result ?: "Unknown error"
                )
            }

            BackendEvent.TYPE_RESPONSE -> {
                OrchestratorEvent.ResponseComplete(
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = text ?: ""
                    )
                )
            }

            BackendEvent.TYPE_ERROR -> {
                OrchestratorEvent.Error(text ?: "Unknown error")
            }

            BackendEvent.TYPE_DONE -> {
                // Done event - we can ignore this or emit a special event
                // For now, just emit a thinking event that will be filtered
                OrchestratorEvent.Thinking("Done")
            }

            else -> {
                OrchestratorEvent.Error("Unknown event type: $type")
            }
        }
    }
}
