package com.example.railchatbot.domain.model

/**
 * Events emitted during chat orchestration.
 * Used by the UI layer to display progress.
 */
sealed class OrchestratorEvent {
    data class Thinking(val message: String) : OrchestratorEvent()
    data class ToolCallStarted(val toolName: String, val arguments: Map<String, Any?>) : OrchestratorEvent()
    data class ToolCallCompleted(val toolName: String, val result: String) : OrchestratorEvent()
    data class ToolCallFailed(val toolName: String, val error: String) : OrchestratorEvent()
    data class ResponseComplete(val message: ChatMessage) : OrchestratorEvent()
    data class Error(val message: String, val cause: Throwable? = null) : OrchestratorEvent()
}
