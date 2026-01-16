package com.example.railchatbot.data.remote.ai

import com.example.railchatbot.data.remote.ai.model.ConversationMessage
import com.example.railchatbot.data.remote.ai.model.FunctionDefinition
import com.example.railchatbot.data.remote.ai.model.LlmResponse

/**
 * Interface for LLM providers (OpenAI, Gemini, etc.)
 */
interface LlmClient {

    /**
     * Send a chat completion request to the LLM
     *
     * @param messages Conversation history
     * @param tools Available tools/functions the LLM can call
     * @return LLM response (text or tool calls)
     */
    suspend fun chat(
        messages: List<ConversationMessage>,
        tools: List<FunctionDefinition>? = null
    ): LlmResponse

    /**
     * Whether this LLM supports tool/function calling
     */
    fun supportsToolCalling(): Boolean
}
