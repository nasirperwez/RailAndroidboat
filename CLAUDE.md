# CLAUDE.md - AI Assistant Context File

> This file provides context for AI assistants (like Claude) to quickly understand the project structure, architecture, and key implementation details.

---

## Project Overview

**Rail Chatbot** is an Android application that serves as an **MCP (Model Context Protocol) Client**. It connects users to Indian Railways (IRCTC) data through natural language, using AI to automatically select and execute appropriate API tools.

### Core Concept

```
User Query → LLM (GPT-4o-mini) → MCP Tool Selection → IRCTC API → Response
```

---

## Quick Reference

### Tech Stack
- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Network**: Ktor Client
- **AI**: OpenAI GPT-4o-mini
- **Protocol**: MCP (JSON-RPC 2.0)
- **Architecture**: Clean Architecture + MVVM

### Key Files to Understand

| File | Purpose |
|------|---------|
| `domain/orchestrator/ChatOrchestrator.kt` | **THE BRAIN** - Implements AI + Tool calling loop |
| `data/remote/mcp/McpClientWrapper.kt` | MCP JSON-RPC 2.0 client implementation |
| `data/remote/ai/OpenAiLlmClient.kt` | OpenAI integration with tool/function calling |
| `presentation/chat/ChatViewModel.kt` | UI state management (MVI pattern) |
| `presentation/chat/ChatScreen.kt` | Main Compose UI |

---

## Architecture Details

### Layer Structure

```
┌─────────────────────────────────────────┐
│ PRESENTATION (UI)                       │
│ - ChatScreen.kt (Compose)               │
│ - ChatViewModel.kt (StateFlow)          │
│ - ChatUiState.kt (MVI State)            │
├─────────────────────────────────────────┤
│ DOMAIN (Business Logic)                 │
│ - ChatOrchestrator.kt ← MOST IMPORTANT  │
│ - SendMessageUseCase.kt                 │
│ - ChatMessage.kt (Domain Model)         │
├─────────────────────────────────────────┤
│ DATA (External Services)                │
│ - McpClientWrapper.kt (MCP Client)      │
│ - OpenAiLlmClient.kt (LLM Client)       │
│ - ChatRepository.kt (Message Storage)   │
├─────────────────────────────────────────┤
│ DI (Hilt Modules)                       │
│ - NetworkModule.kt (Ktor)               │
│ - McpModule.kt (MCP Config)             │
│ - AiModule.kt (OpenAI Config)           │
└─────────────────────────────────────────┘
```

### Data Flow (Step by Step)

1. **User types message** → `ChatInputBar` composable
2. **Intent dispatched** → `ChatViewModel.onIntent(ChatIntent.SendMessage)`
3. **Use case invoked** → `SendMessageUseCase.invoke()`
4. **Orchestrator processes** → `ChatOrchestrator.processMessage()`
   - Ensures MCP connection (`mcpClient.initialize()`)
   - Fetches available tools (`mcpClient.listTools()`)
   - Sends to LLM with tools (`llmClient.chat(messages, tools)`)
5. **LLM responds** with either:
   - `TextResponse` → Final answer
   - `ToolCallsResponse` → Needs to call MCP tools
6. **If tool calls** → Execute via `mcpClient.callTool(name, args)`
7. **Tool results** → Sent back to LLM for final response
8. **UI updates** → `OrchestratorEvent` → `ChatUiState` → Recompose

---

## MCP Implementation Details

### Server Configuration

```kotlin
Server URL: https://mcp.rapidapi.com
Headers:
  - x-api-host: irctc1.p.rapidapi.com
  - x-api-key: [from local.properties]
```

### JSON-RPC Methods Used

| Method | Purpose |
|--------|---------|
| `initialize` | Establish connection, exchange capabilities |
| `tools/list` | Discover available IRCTC tools (returns 17) |
| `tools/call` | Execute a specific tool with arguments |

### Available IRCTC Tools (17 total)

```
Get_PNR_Status_Detail    CheckPNRStatus         Get_PNR_Status_V3
TrainsBetweenStations    TrainsBetweenStations_V3
Get_Train_Schedule       Get_Train_Schedule_V2
Get_Train_Live_Status    Get_Live_Station       Get_Trains_By_Station
CheckSeatAvailability    CheckSeatAvailability_V2
Get_Fare                 GetFare                GetTrainClasses
SearchTrain              SearchStation
```

---

## Key Implementation Patterns

### ChatOrchestrator - The Core Loop

```kotlin
fun processMessage(userMessage: String): Flow<OrchestratorEvent> = flow {
    // 1. Initialize MCP if needed
    mcpClient.initialize()

    // 2. Get available tools
    val tools = mcpClient.listTools()

    // 3. Build conversation with system prompt
    val messages = buildConversationMessages(userMessage, history)

    // 4. Tool calling loop (max 10 iterations)
    while (iterations < MAX_ITERATIONS && !done) {
        val response = llmClient.chat(messages, tools)

        when (response) {
            is TextResponse -> emit(ResponseComplete(response.text))
            is ToolCallsResponse -> {
                // Execute each tool call
                for (toolCall in response.toolCalls) {
                    emit(ToolCallStarted(toolCall.name))
                    val result = mcpClient.callTool(toolCall.name, toolCall.args)
                    emit(ToolCallCompleted(result))
                    // Add result to messages for next LLM call
                }
            }
        }
    }
}
```

### MCP Client - JSON-RPC 2.0

```kotlin
// Request format
{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
        "name": "Get_PNR_Status_Detail",
        "arguments": { "pnrNumber": "1234567890" }
    }
}

// Response format
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": {
        "content": [{ "type": "text", "text": "{...json data...}" }]
    }
}
```

### OpenAI Tool Calling

MCP tools are converted to OpenAI function definitions:

```kotlin
Tool.function(
    name = mcpTool.name,
    description = mcpTool.description,
    parameters = Parameters.fromJsonString(mcpTool.inputSchema)
)
```

LLM response with tool calls:
```kotlin
when (response) {
    is ToolCallsResponse -> {
        // response.toolCalls contains: id, name, arguments
        // Execute via MCP and return results
    }
}
```

---

## Configuration

### API Keys (local.properties)

```properties
OPENAI_API_KEY=sk-...
RAPIDAPI_KEY=...
```

### Build Config Fields

```kotlin
BuildConfig.MCP_SERVER_URL      // https://mcp.rapidapi.com
BuildConfig.RAPIDAPI_HOST       // irctc1.p.rapidapi.com
BuildConfig.RAPIDAPI_KEY        // from local.properties
BuildConfig.OPENAI_API_KEY      // from local.properties
```

---

## Common Tasks

### Adding a New LLM Provider

1. Implement `LlmClient` interface in `data/remote/ai/`
2. Add provider to `AiModule.kt`
3. Handle tool calling format differences

### Connecting to Different MCP Server

1. Update `BuildConfig` fields in `app/build.gradle.kts`
2. Adjust headers in `McpClientWrapper.kt` if needed
3. Tool discovery is automatic via `tools/list`

### Adding UI Features

1. Update `ChatUiState.kt` with new state
2. Handle in `ChatViewModel.kt`
3. Update `ChatScreen.kt` and components

---

## Debugging Tips

### Enable Logging

Check Logcat with tags:
- `McpClientWrapper` - MCP requests/responses
- `ChatOrchestrator` - Tool calling loop
- `OpenAiLlmClient` - LLM interactions

### Common Issues

| Issue | Solution |
|-------|----------|
| 502 on tools/list | Check `x-api-host` header (must be `irctc1.p.rapidapi.com`) |
| "Not subscribed" | Subscribe to IRCTC API on RapidAPI |
| Auth error | Check API keys in `local.properties` |
| Tools not discovered | Ensure `initialize()` called before `listTools()` |

---

## File Locations Quick Reference

```
app/src/main/java/com/example/railchatbot/
├── di/
│   ├── NetworkModule.kt         ← Ktor HTTP client setup
│   ├── McpModule.kt             ← MCP client configuration
│   └── AiModule.kt              ← OpenAI client setup
├── data/remote/
│   ├── mcp/
│   │   ├── McpClientWrapper.kt  ← JSON-RPC client (IMPORTANT)
│   │   └── model/McpTool.kt     ← MCP data models
│   └── ai/
│       ├── LlmClient.kt         ← Interface
│       └── OpenAiLlmClient.kt   ← OpenAI implementation (IMPORTANT)
├── domain/
│   ├── orchestrator/
│   │   └── ChatOrchestrator.kt  ← CORE LOGIC (MOST IMPORTANT)
│   └── usecase/
│       └── SendMessageUseCase.kt
└── presentation/chat/
    ├── ChatViewModel.kt         ← State management
    ├── ChatScreen.kt            ← Main UI
    └── components/              ← UI components
```

---

## Summary for Quick Understanding

1. **This is an MCP Client** - It connects to MCP servers (IRCTC via RapidAPI)
2. **Uses AI for tool selection** - GPT-4o-mini decides which tool to call
3. **Clean Architecture** - Presentation → Domain → Data layers
4. **Key file is `ChatOrchestrator.kt`** - Contains the AI + Tools loop
5. **MCP uses JSON-RPC 2.0** - initialize → tools/list → tools/call
6. **17 IRCTC tools available** - PNR, trains, schedules, fares, etc.

---

*Last updated: January 2025*
