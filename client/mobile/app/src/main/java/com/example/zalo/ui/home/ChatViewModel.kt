package com.example.zalo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zalo.data.remote.WebSocketManager
import com.example.zalo.data.remote.dto.ChatDto
import com.example.zalo.data.remote.dto.GroupDto
import com.example.zalo.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val chats: List<ChatDto> = emptyList(),
    val groups: List<GroupDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        webSocketManager.connect()
        observeWebSocket()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val chatsResult = repository.getChats()
            val groupsResult = repository.getGroups()
            
            _uiState.value = HomeUiState(
                chats = chatsResult.getOrNull() ?: emptyList(),
                groups = groupsResult.getOrNull() ?: emptyList(),
                isLoading = false
            )
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.messages.collect { msg ->
                _uiState.value = _uiState.value.copy(
                    chats = _uiState.value.chats.map { chat ->
                        if (chat.id == msg.chatId) {
                            chat.copy(
                                lastMessage = msg.content,
                                lastMessageTime = msg.createdAt,
                                unreadCount = chat.unreadCount + 1
                            )
                        } else chat
                    }
                )
            }
        }
        
        viewModelScope.launch {
            webSocketManager.groupMessages.collect { msg ->
                _uiState.value = _uiState.value.copy(
                    groups = _uiState.value.groups.map { group ->
                        if (group.id == msg.groupId) {
                            group.copy(
                                lastMessage = msg.content,
                                lastMessageTime = msg.createdDate
                                // Note: GroupDto might need more fields for last message
                            )
                        } else group
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
