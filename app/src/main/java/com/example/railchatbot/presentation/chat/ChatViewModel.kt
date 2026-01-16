package com.example.railchatbot.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.railchatbot.data.repository.ChatRepository
import com.example.railchatbot.domain.model.ChatMessage
import com.example.railchatbot.domain.model.MessageRole
import com.example.railchatbot.domain.orchestrator.OrchestratorEvent
import com.example.railchatbot.domain.usecase.InitializeMcpUseCase
import com.example.railchatbot.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val initializeMcpUseCase: InitializeMcpUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> updateInput(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.Retry -> retry()
            is ChatIntent.ClearError -> clearError()
            is ChatIntent.Reconnect -> initialize()
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, error = null) }

            initializeMcpUseCase()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            error = "Failed to connect: ${error.message}"
                        )
                    }
                }
        }
    }

    private fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text.trim()
        )

        // Add user message to state
        chatRepository.addMessage(userMessage)
        _uiState.update {
            it.copy(
                messages = chatRepository.getMessages(),
                inputText = "",
                isLoading = true,
                error = null,
                currentThinking = null,
                currentToolCall = null
            )
        }

        // Get conversation history (excluding the message just added)
        val history = chatRepository.getMessages().dropLast(1)

        // Process message
        sendMessageUseCase(text.trim(), history)
            .onEach { event ->
                handleOrchestratorEvent(event)
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${error.message}",
                        currentThinking = null,
                        currentToolCall = null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleOrchestratorEvent(event: OrchestratorEvent) {
        when (event) {
            is OrchestratorEvent.Thinking -> {
                _uiState.update {
                    it.copy(currentThinking = event.message)
                }
            }

            is OrchestratorEvent.ToolCallStarted -> {
                _uiState.update {
                    it.copy(
                        currentToolCall = CurrentToolCall(
                            toolName = event.toolName,
                            status = ToolCallUiStatus.EXECUTING
                        ),
                        currentThinking = null
                    )
                }
            }

            is OrchestratorEvent.ToolCallCompleted -> {
                _uiState.update {
                    it.copy(
                        currentToolCall = CurrentToolCall(
                            toolName = event.toolName,
                            status = ToolCallUiStatus.COMPLETED
                        )
                    )
                }
            }

            is OrchestratorEvent.ToolCallFailed -> {
                _uiState.update {
                    it.copy(
                        currentToolCall = CurrentToolCall(
                            toolName = event.toolName,
                            status = ToolCallUiStatus.FAILED
                        )
                    )
                }
            }

            is OrchestratorEvent.ResponseComplete -> {
                chatRepository.addMessage(event.message)
                _uiState.update {
                    it.copy(
                        messages = chatRepository.getMessages(),
                        isLoading = false,
                        currentThinking = null,
                        currentToolCall = null
                    )
                }
            }

            is OrchestratorEvent.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = event.message,
                        currentThinking = null,
                        currentToolCall = null
                    )
                }
            }
        }
    }

    private fun retry() {
        val lastUserMessage = chatRepository.getMessages()
            .lastOrNull { it.role == MessageRole.USER }

        if (lastUserMessage != null) {
            // Remove the last user message and any subsequent messages
            val messagesBeforeLast = chatRepository.getMessages()
                .takeWhile { it.id != lastUserMessage.id }

            chatRepository.clearMessages()
            messagesBeforeLast.forEach { chatRepository.addMessage(it) }

            _uiState.update { it.copy(messages = chatRepository.getMessages()) }

            // Resend the message
            sendMessage(lastUserMessage.content)
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
