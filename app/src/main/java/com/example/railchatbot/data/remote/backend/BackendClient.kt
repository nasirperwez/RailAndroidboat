package com.example.railchatbot.data.remote.backend

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for communicating with the Rail Chatbot backend server via SSE.
 */
@Singleton
class BackendClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val backendUrl: String
) {
    companion object {
        private const val TAG = "BackendClient"
    }

    /**
     * Send a message to the backend and stream events.
     *
     * @param message The user's message
     * @param history Conversation history (simplified for now)
     * @return Flow of BackendEvent objects
     */
    fun sendMessage(
        message: String,
        history: List<BackendChatMessage> = emptyList()
    ): Flow<BackendEvent> = flow {
        val request = ChatRequest(message = message, history = history)
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)

        Log.d(TAG, "Sending message to backend: $message")

        httpClient.preparePost("$backendUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.execute { response ->
            val channel = response.bodyAsChannel()

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break

                // SSE format: "data: {...}"
                if (line.startsWith("data: ")) {
                    val jsonStr = line.removePrefix("data: ").trim()
                    if (jsonStr.isNotEmpty()) {
                        try {
                            val event = json.decodeFromString(BackendEvent.serializer(), jsonStr)
                            Log.d(TAG, "Received event: ${event.type}")
                            emit(event)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse event: $jsonStr", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if the backend is healthy.
     */
    suspend fun healthCheck(): Boolean {
        return try {
            httpClient.preparePost("$backendUrl/health").execute { response ->
                response.status.value == 200
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            false
        }
    }
}

/**
 * Request body for the chat endpoint.
 */
@Serializable
data class ChatRequest(
    val message: String,
    val history: List<BackendChatMessage> = emptyList()
)

/**
 * Simplified chat message for history.
 */
@Serializable
data class BackendChatMessage(
    val role: String,
    val content: String
)

/**
 * Event received from the backend SSE stream.
 */
@Serializable
data class BackendEvent(
    val type: String,
    val text: String? = null,
    val name: String? = null,
    val args: Map<String, String>? = null,
    val result: String? = null
) {
    companion object {
        const val TYPE_THINKING = "thinking"
        const val TYPE_TOOL_START = "tool_start"
        const val TYPE_TOOL_COMPLETE = "tool_complete"
        const val TYPE_TOOL_ERROR = "tool_error"
        const val TYPE_RESPONSE = "response"
        const val TYPE_ERROR = "error"
        const val TYPE_DONE = "done"
    }
}
