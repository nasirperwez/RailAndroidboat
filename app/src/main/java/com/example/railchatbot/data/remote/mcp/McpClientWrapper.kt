package com.example.railchatbot.data.remote.mcp

import android.util.Log
import com.example.railchatbot.data.remote.mcp.model.JsonRpcRequest
import com.example.railchatbot.data.remote.mcp.model.JsonRpcResponse
import com.example.railchatbot.data.remote.mcp.model.McpContent
import com.example.railchatbot.data.remote.mcp.model.McpTool
import com.example.railchatbot.data.remote.mcp.model.McpToolResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for MCP client that handles communication with the IRCTC MCP server.
 * Implements JSON-RPC 2.0 protocol over HTTP.
 */
@Singleton
class McpClientWrapper @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val serverUrl: String,
    private val apiHost: String,
    private val apiKey: String
) {
    companion object {
        private const val TAG = "McpClientWrapper"
        private const val MCP_PROTOCOL_VERSION = "2025-03-26"
        private const val CLIENT_NAME = "RailChatbot"
        private const val CLIENT_VERSION = "1.0.0"
    }

    private val requestIdCounter = AtomicInteger(0)
    private val mutex = Mutex()

    private var isInitialized = false
    private var sessionId: String? = null
    private var cachedTools: List<McpTool> = emptyList()

    /**
     * Initialize connection to the MCP server
     */
    suspend fun initialize(): Result<Unit> = runCatching {
        mutex.withLock {
            if (isInitialized) {
                Log.d(TAG, "Already initialized")
                return@runCatching
            }

            val params = buildJsonObject {
                put("protocolVersion", MCP_PROTOCOL_VERSION)
                put("capabilities", buildJsonObject {
                    put("tools", buildJsonObject { })
                })
                put("clientInfo", buildJsonObject {
                    put("name", CLIENT_NAME)
                    put("version", CLIENT_VERSION)
                })
            }

            val response = sendRequest("initialize", params)

            if (response.error != null) {
                throw McpException(
                    response.error.code,
                    response.error.message
                )
            }

            // Send initialized notification
            sendNotification("notifications/initialized")

            isInitialized = true
            Log.d(TAG, "MCP client initialized successfully")
        }
    }

    /**
     * List available tools from the MCP server
     */
    suspend fun listTools(): Result<List<McpTool>> = runCatching {
        ensureInitialized()

        if (cachedTools.isNotEmpty()) {
            return@runCatching cachedTools
        }

        val response = sendRequest("tools/list", null)

        if (response.error != null) {
            throw McpException(response.error.code, response.error.message)
        }

        val result = response.result ?: throw McpException(-1, "No result in response")
        val toolsArray = result["tools"]?.jsonArray ?: JsonArray(emptyList())

        cachedTools = toolsArray.map { toolJson ->
            val obj = toolJson.jsonObject
            McpTool(
                name = obj["name"]?.jsonPrimitive?.content ?: "",
                description = obj["description"]?.jsonPrimitive?.content,
                inputSchema = obj["inputSchema"]?.jsonObject
            )
        }

        Log.d(TAG, "Fetched ${cachedTools.size} tools")
        cachedTools
    }

    /**
     * Call a tool on the MCP server
     */
    suspend fun callTool(name: String, arguments: Map<String, Any?>): Result<McpToolResult> = runCatching {
        ensureInitialized()

        val argsJsonObject = buildJsonObject {
            arguments.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, value)
                    null -> put(key, JsonPrimitive(null as String?))
                    else -> put(key, value.toString())
                }
            }
        }

        val params = buildJsonObject {
            put("name", name)
            put("arguments", argsJsonObject)
        }

        Log.d(TAG, "Calling tool: $name with args: $arguments")

        val response = sendRequest("tools/call", params)

        if (response.error != null) {
            throw McpException(response.error.code, response.error.message)
        }

        val result = response.result ?: throw McpException(-1, "No result in response")

        val contentArray = result["content"]?.jsonArray ?: JsonArray(emptyList())
        val contents = contentArray.map { contentJson ->
            val obj = contentJson.jsonObject
            McpContent(
                type = obj["type"]?.jsonPrimitive?.content ?: "text",
                text = obj["text"]?.jsonPrimitive?.content
            )
        }

        val isError = result["isError"]?.jsonPrimitive?.content?.toBoolean() ?: false

        McpToolResult(content = contents, isError = isError)
    }

    /**
     * Check if the client is connected and initialized
     */
    fun isConnected(): Boolean = isInitialized

    /**
     * Disconnect and reset the client
     */
    suspend fun disconnect() {
        mutex.withLock {
            isInitialized = false
            sessionId = null
            cachedTools = emptyList()
        }
    }

    private suspend fun ensureInitialized() {
        if (!isInitialized) {
            initialize().getOrThrow()
        }
    }

    private suspend fun sendRequest(method: String, params: JsonObject?): JsonRpcResponse {
        val requestId = requestIdCounter.incrementAndGet()

        val request = JsonRpcRequest(
            id = requestId,
            method = method,
            params = params
        )

        val requestBody = json.encodeToString(JsonRpcRequest.serializer(), request)
        Log.d(TAG, "Sending request: $requestBody")

        val responseText: String = httpClient.post(serverUrl) {
            contentType(ContentType.Application.Json)
            headers {
                append("x-api-host", apiHost)
                append("x-api-key", apiKey)
                append("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
                sessionId?.let { append("Mcp-Session-Id", it) }
            }
            setBody(requestBody)
        }.body()

        Log.d(TAG, "Response: $responseText")

        val response = json.decodeFromString(JsonRpcResponse.serializer(), responseText)

        // Extract session ID from response if present (for future requests)
        // This is typically handled by the transport layer

        return response
    }

    private suspend fun sendNotification(method: String) {
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
        }

        val requestBody = json.encodeToString(JsonObject.serializer(), request)

        try {
            httpClient.post(serverUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    append("x-api-host", apiHost)
                    append("x-api-key", apiKey)
                    append("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
                    sessionId?.let { append("Mcp-Session-Id", it) }
                }
                setBody(requestBody)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send notification: ${e.message}")
        }
    }
}

/**
 * Exception for MCP protocol errors
 */
class McpException(val code: Int, message: String) : Exception(message)
