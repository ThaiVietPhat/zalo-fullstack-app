package com.example.zalo.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zalo.data.local.TokenManager
import com.example.zalo.data.remote.WebSocketManager
import com.example.zalo.data.remote.dto.MessageDto
import com.example.zalo.data.remote.dto.SummarizeResponse
import com.example.zalo.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTyping: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val summary: SummarizeResponse? = null
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentChatId: String? = null

    fun loadMessages(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getMessages(chatId, 0)
            if (result.isSuccess) {
                _uiState.value = ChatUiState(messages = result.getOrNull()?.reversed() ?: emptyList())
                webSocketManager.subscribeToChat(chatId)
                repository.markAsSeen(chatId)
                loadSmartReplies()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
        observeWebSocket()
    }

    private fun loadSmartReplies() {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            val result = repository.getSmartReply(chatId, false)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(suggestions = result.getOrNull()?.suggestions ?: emptyList())
            }
        }
    }

    fun summarize() {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.summarize(chatId, false, 20)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, summary = result.getOrNull())
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Không thể tóm tắt")
            }
        }
    }

    fun clearSummary() {
        _uiState.value = _uiState.value.copy(summary = null)
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.messages.collect { msg ->
                if (msg.chatId == currentChatId) {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + msg,
                        suggestions = emptyList() // Clear suggestions after new message
                    )
                    repository.markAsSeen(currentChatId!!)
                    loadSmartReplies()
                }
            }
        }
        viewModelScope.launch {
            webSocketManager.typingStatus.collect { (chatId, payload) ->
                if (chatId == currentChatId && payload.userId != tokenManager.getUserId()) {
                    _uiState.value = _uiState.value.copy(isTyping = payload.typing)
                }
            }
        }
    }

    fun sendMessage(content: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            val newMessage = MessageDto(
                chatId = chatId,
                content = content,
                type = "TEXT",
                senderId = tokenManager.getUserId()
            )
            repository.sendMessage(newMessage)
        }
    }

    fun uploadMedia(file: File) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.uploadMedia(chatId, file)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun sendTyping(isTyping: Boolean) {
        currentChatId?.let { webSocketManager.sendTyping(it, isTyping) }
    }
}
