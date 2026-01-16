package com.example.railchatbot.domain.usecase

import com.example.railchatbot.domain.model.ChatMessage
import com.example.railchatbot.domain.orchestrator.ChatOrchestrator
import com.example.railchatbot.domain.orchestrator.OrchestratorEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for sending a message and getting a response
 */
class SendMessageUseCase @Inject constructor(
    private val orchestrator: ChatOrchestrator
) {
    /**
     * Send a message and receive a flow of events
     */
    operator fun invoke(
        message: String,
        conversationHistory: List<ChatMessage>
    ): Flow<OrchestratorEvent> {
        return orchestrator.processMessage(message, conversationHistory)
    }
}
