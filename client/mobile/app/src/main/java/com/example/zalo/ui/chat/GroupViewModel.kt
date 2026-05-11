package com.example.zalo.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zalo.data.local.TokenManager
import com.example.zalo.data.remote.WebSocketManager
import com.example.zalo.data.remote.dto.GroupMessageDto
import com.example.zalo.data.remote.dto.SummarizeResponse
import com.example.zalo.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class GroupChatUiState(
    val messages: List<GroupMessageDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTyping: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val summary: SummarizeResponse? = null
)

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null

    fun loadMessages(groupId: String) {
        currentGroupId = groupId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getGroupMessages(groupId, 0)
            if (result.isSuccess) {
                _uiState.value = GroupChatUiState(messages = result.getOrNull()?.reversed() ?: emptyList())
                webSocketManager.subscribeToGroup(groupId)
                loadSmartReplies()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
        observeWebSocket()
    }

    private fun loadSmartReplies() {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            val result = repository.getSmartReply(groupId, true)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(suggestions = result.getOrNull()?.suggestions ?: emptyList())
            }
        }
    }

    fun summarize() {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.summarize(groupId, true, 20)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, summary = result.getOrNull())
            }
        }
    }

    fun clearSummary() {
        _uiState.value = _uiState.value.copy(summary = null)
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.groupMessages.collect { msg ->
                if (msg.groupId == currentGroupId) {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + msg,
                        suggestions = emptyList()
                    )
                    loadSmartReplies()
                }
            }
        }
        viewModelScope.launch {
            webSocketManager.typingStatus.collect { (id, payload) ->
                if (id == currentGroupId && payload.userId != tokenManager.getUserId()) {
                    _uiState.value = _uiState.value.copy(isTyping = payload.typing)
                }
            }
        }
    }

    fun sendMessage(content: String) {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            repository.sendGroupMessage(groupId, content)
        }
    }

    fun uploadMedia(file: File) {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.uploadGroupMedia(groupId, file)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun sendTyping(isTyping: Boolean) {
        currentGroupId?.let { webSocketManager.sendTyping(it, isTyping, true) }
    }
}
