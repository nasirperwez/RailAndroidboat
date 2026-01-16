# Rail Chatbot - Android MCP Client

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white)

**An Android chatbot that connects AI with Indian Railways data through the Model Context Protocol (MCP)**

[Features](#features) • [Architecture](#architecture) • [Installation](#installation) • [Usage](#usage) • [Documentation](#documentation)

</div>

---

## Overview

Rail Chatbot is a sophisticated Android application demonstrating how to build an **MCP (Model Context Protocol) Client** that connects a Large Language Model (LLM) with external tools and APIs. Users can query Indian Railways information using natural language, with AI automatically selecting and calling the appropriate IRCTC API tools.

### What is MCP?

The **Model Context Protocol (MCP)** is an open protocol that enables AI applications to securely connect with external data sources and tools. It uses JSON-RPC 2.0 for communication and provides a standardized way for LLMs to discover and invoke tools.

---

## Features

- **Natural Language Interface** - Ask questions in plain English like "Find trains from Delhi to Mumbai"
- **AI-Powered Tool Selection** - GPT-4o-mini automatically chooses the right API tool
- **17 IRCTC Tools Available**:
  - PNR Status Check
  - Train Search Between Stations
  - Live Train Status
  - Train Schedule
  - Seat Availability
  - Fare Enquiry
  - Station Search
  - And more...
- **Modern Material 3 UI** - Beautiful chat interface built with Jetpack Compose
- **Real-time Status** - See connection status, tool execution progress, and typing indicators

---

## Architecture

The app follows **Clean Architecture** with **MVVM** pattern:

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                        │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────────┐  │
│  │ ChatScreen  │───▶│ ChatViewModel│───▶│ ChatUiState (MVI) │  │
│  │ (Compose)   │    │  (Hilt VM)   │    │                   │  │
│  └─────────────┘    └──────────────┘    └───────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                         DOMAIN LAYER                             │
│  ┌──────────────────┐    ┌─────────────────────────────────┐   │
│  │ SendMessageUseCase│───▶│ ChatOrchestrator (AI+Tools Loop)│   │
│  └──────────────────┘    └─────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                          DATA LAYER                              │
│  ┌─────────────────┐    ┌──────────────────┐    ┌───────────┐  │
│  │ McpClientWrapper│    │ OpenAiLlmClient  │    │ ChatRepo  │  │
│  │  (JSON-RPC 2.0) │    │   (GPT-4o-mini)  │    │           │  │
│  └────────┬────────┘    └────────┬─────────┘    └───────────┘  │
│           │                      │                              │
│           ▼                      ▼                              │
│  ┌─────────────────┐    ┌──────────────────┐                   │
│  │ IRCTC MCP Server│    │   OpenAI API     │                   │
│  │   (RapidAPI)    │    │                  │                   │
│  └─────────────────┘    └──────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **User Input** → User types message in chat
2. **ViewModel** → Receives intent, calls SendMessageUseCase
3. **Orchestrator** → Fetches MCP tools, sends to LLM with user message
4. **LLM Analysis** → GPT-4o-mini decides which tool to call
5. **MCP Tool Call** → McpClientWrapper executes tool via JSON-RPC
6. **Result Processing** → Tool result sent back to LLM for formatting
7. **UI Update** → Final response displayed in chat

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0 |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Hilt |
| Networking | Ktor Client |
| AI/LLM | OpenAI GPT-4o-mini |
| Protocol | MCP (JSON-RPC 2.0) |
| Async | Kotlin Coroutines + Flow |
| Serialization | Kotlinx Serialization |

---

## Installation

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 35
- An OpenAI API key
- A RapidAPI key (subscribed to IRCTC API)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/nasirperwez/RailAndroidboat.git
   cd RailAndroidboat
   ```

2. **Configure API keys**

   Create `local.properties` in the project root:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   OPENAI_API_KEY=sk-your-openai-api-key
   RAPIDAPI_KEY=your-rapidapi-key
   ```

3. **Build and run**
   ```bash
   ./gradlew installDebug
   ```

### Getting API Keys

- **OpenAI API Key**: https://platform.openai.com/api-keys
- **RapidAPI Key**: https://rapidapi.com/IRCTCAPI/api/irctc1 (Subscribe to the API)

---

## Usage

### Example Queries

| Query | Tool Used |
|-------|-----------|
| "Check PNR status 1234567890" | Get_PNR_Status_Detail |
| "Find trains from Delhi to Mumbai" | TrainsBetweenStations |
| "Get schedule for train 12936" | Get_Train_Schedule |
| "Search station Chennai" | SearchStation |
| "Check seat availability on train 19038" | CheckSeatAvailability |
| "What's the live status of train 12345?" | Get_Train_Live_Status |

### Screenshots

The app features a modern chat interface with:
- Message bubbles (user/assistant differentiation)
- Connection status indicator
- Tool execution progress
- Typing/thinking indicators

---

## Project Structure

```
app/src/main/java/com/example/railchatbot/
├── RailChatApplication.kt          # Hilt Application
├── MainActivity.kt                 # Entry point
│
├── di/                             # Dependency Injection
│   ├── NetworkModule.kt            # Ktor HTTP client
│   ├── McpModule.kt                # MCP client config
│   └── AiModule.kt                 # OpenAI client
│
├── data/
│   ├── remote/
│   │   ├── mcp/
│   │   │   ├── McpClientWrapper.kt # JSON-RPC 2.0 client
│   │   │   └── model/              # MCP data models
│   │   └── ai/
│   │       ├── LlmClient.kt        # LLM interface
│   │       └── OpenAiLlmClient.kt  # OpenAI implementation
│   └── repository/
│       └── ChatRepository.kt       # Message storage
│
├── domain/
│   ├── model/
│   │   └── ChatMessage.kt          # Domain models
│   ├── usecase/
│   │   └── SendMessageUseCase.kt   # Business logic
│   └── orchestrator/
│       └── ChatOrchestrator.kt     # AI + Tools loop
│
└── presentation/
    ├── theme/                      # Material 3 theme
    └── chat/
        ├── ChatScreen.kt           # Main UI
        ├── ChatViewModel.kt        # State management
        └── components/             # UI components
```

---

## MCP Protocol Implementation

### Available Tools

The app discovers and uses 17 IRCTC tools:

| Tool Name | Description |
|-----------|-------------|
| `Get_PNR_Status_Detail` | Get detailed PNR status |
| `CheckPNRStatus` | Check PNR booking status |
| `TrainsBetweenStations` | Find trains between two stations |
| `Get_Train_Schedule` | Get train schedule |
| `Get_Train_Live_Status` | Live running status |
| `CheckSeatAvailability` | Check seat availability |
| `Get_Fare` | Get fare information |
| `SearchTrain` | Search trains by number/name |
| `SearchStation` | Search stations |
| `GetTrainClasses` | Get available classes |
| And 7 more... | |

### Protocol Flow

```
1. Initialize:    POST /mcp { "method": "initialize", ... }
2. List Tools:    POST /mcp { "method": "tools/list" }
3. Call Tool:     POST /mcp { "method": "tools/call", "params": { "name": "...", "arguments": {...} }}
```

---

## Documentation

- **[documentation.html](documentation.html)** - Detailed visual documentation with architecture diagrams
- **[CLAUDE.md](CLAUDE.md)** - AI assistant context file

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [Model Context Protocol](https://modelcontextprotocol.io/) - The MCP specification
- [OpenAI](https://openai.com/) - GPT-4o-mini for natural language understanding
- [RapidAPI](https://rapidapi.com/) - IRCTC API hosting
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit

---

<div align="center">

**Built with ❤️ using Kotlin, Jetpack Compose, and MCP**

[⬆ Back to Top](#rail-chatbot---android-mcp-client)

</div>
