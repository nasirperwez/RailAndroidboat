package com.example.railchatbot.data.remote.ai

import android.util.Log
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionMode
import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.chat.ToolChoice
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.railchatbot.data.remote.ai.model.ConversationMessage
import com.example.railchatbot.data.remote.ai.model.FunctionDefinition
import com.example.railchatbot.data.remote.ai.model.LlmResponse
import com.example.railchatbot.data.remote.ai.model.LlmToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI implementation of LlmClient using GPT-4
 */
@Singleton
class OpenAiLlmClient @Inject constructor(
    private val openAI: OpenAI
) : LlmClient {

    companion object {
        private const val TAG = "OpenAiLlmClient"
        private const val MODEL = "gpt-4o-mini"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(
        messages: List<ConversationMessage>,
        tools: List<FunctionDefinition>?
    ): LlmResponse {
        return try {
            val openAiMessages = messages.map { msg ->
                when (msg.role) {
                    "system" -> ChatMessage(
                        role = ChatRole.System,
                        content = msg.content
                    )
                    "user" -> ChatMessage(
                        role = ChatRole.User,
                        content = msg.content
                    )
                    "assistant" -> {
                        if (msg.toolCalls != null) {
                            ChatMessage(
                                role = ChatRole.Assistant,
                                content = msg.content,
                                toolCalls = msg.toolCalls.map { tc ->
                                    ToolCall.Function(
                                        id = com.aallam.openai.api.chat.ToolId(tc.id),
                                        function = com.aallam.openai.api.chat.FunctionCall(
                                            nameOrNull = tc.name,
                                            argumentsOrNull = json.encodeToString(
                                                JsonObject.serializer(),
                                                buildArgumentsJson(tc.arguments)
                                            )
                                        )
                                    )
                                }
                            )
                        } else {
                            ChatMessage(
                                role = ChatRole.Assistant,
                                content = msg.content
                            )
                        }
                    }
                    "tool" -> ChatMessage(
                        role = ChatRole.Tool,
                        content = msg.content,
                        toolCallId = msg.toolCallId?.let { com.aallam.openai.api.chat.ToolId(it) }
                    )
                    else -> ChatMessage(
                        role = ChatRole.User,
                        content = msg.content
                    )
                }
            }

            val openAiTools = tools?.map { func ->
                Tool.function(
                    name = func.name,
                    description = func.description,
                    parameters = Parameters.fromJsonString(
                        json.encodeToString(JsonObject.serializer(), func.parameters)
                    )
                )
            }

            val request = ChatCompletionRequest(
                model = ModelId(MODEL),
                messages = openAiMessages,
                tools = openAiTools,
                toolChoice = if (openAiTools != null) ToolChoice.Auto else null
            )

            Log.d(TAG, "Sending chat request with ${messages.size} messages and ${tools?.size ?: 0} tools")

            val completion = openAI.chatCompletion(request)
            val choice = completion.choices.firstOrNull()
                ?: return LlmResponse.Error("No response from OpenAI")

            val message = choice.message

            // Check if LLM wants to call tools
            val toolCalls = message.toolCalls
            if (!toolCalls.isNullOrEmpty()) {
                val calls = toolCalls.mapNotNull { toolCall ->
                    when (toolCall) {
                        is ToolCall.Function -> {
                            val args = try {
                                json.decodeFromString(
                                    JsonObject.serializer(),
                                    toolCall.function.arguments
                                ).toMap()
                            } catch (e: Exception) {
                                emptyMap()
                            }

                            LlmToolCall(
                                id = toolCall.id.id,
                                name = toolCall.function.name,
                                arguments = args
                            )
                        }
                        else -> null
                    }
                }

                Log.d(TAG, "LLM requested ${calls.size} tool calls")
                return LlmResponse.ToolCallsResponse(calls)
            }

            // Return text response
            val text = message.content ?: ""
            Log.d(TAG, "LLM returned text response: ${text.take(100)}...")
            LlmResponse.TextResponse(text)

        } catch (e: Exception) {
            Log.e(TAG, "Chat request failed", e)
            LlmResponse.Error("Failed to get response: ${e.message}", e)
        }
    }

    override fun supportsToolCalling(): Boolean = true

    private fun buildArgumentsJson(arguments: Map<String, Any?>): JsonObject {
        return kotlinx.serialization.json.buildJsonObject {
            arguments.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
                    is Number -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
                    is Boolean -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
                    null -> put(key, kotlinx.serialization.json.JsonPrimitive(null as String?))
                    else -> put(key, kotlinx.serialization.json.JsonPrimitive(value.toString()))
                }
            }
        }
    }

    private fun JsonObject.toMap(): Map<String, Any?> {
        return entries.associate { (key, value) ->
            key to when {
                value is kotlinx.serialization.json.JsonPrimitive -> {
                    if (value.isString) value.content
                    else value.content
                }
                else -> value.toString()
            }
        }
    }
}
