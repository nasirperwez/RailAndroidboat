package com.example.railchatbot.data.remote.mcp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents an MCP tool definition returned from tools/list
 */
@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonObject? = null
)

/**
 * Result from executing an MCP tool
 */
@Serializable
data class McpToolResult(
    val content: List<McpContent>,
    val isError: Boolean = false
)

@Serializable
data class McpContent(
    val type: String,
    val text: String? = null
)

/**
 * JSON-RPC 2.0 request structure
 */
@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: JsonObject? = null
)

/**
 * JSON-RPC 2.0 response structure
 */
@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonObject? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonObject? = null
)
