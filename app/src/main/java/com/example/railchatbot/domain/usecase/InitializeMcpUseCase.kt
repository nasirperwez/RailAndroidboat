package com.example.railchatbot.domain.usecase

import com.example.railchatbot.domain.orchestrator.ChatOrchestrator
import javax.inject.Inject

/**
 * Use case for initializing the MCP connection
 */
class InitializeMcpUseCase @Inject constructor(
    private val orchestrator: ChatOrchestrator
) {
    /**
     * Initialize the MCP client and fetch available tools
     */
    suspend operator fun invoke(): Result<Unit> {
        return orchestrator.initialize()
    }

    /**
     * Check if the orchestrator is ready
     */
    fun isReady(): Boolean = orchestrator.isReady()
}
