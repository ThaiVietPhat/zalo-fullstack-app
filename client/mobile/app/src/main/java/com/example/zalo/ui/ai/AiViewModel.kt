package com.example.zalo.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zalo.data.remote.dto.AiMessageDto
import com.example.zalo.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUiState(
    val messages: List<AiMessageDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAiHistory(0).onSuccess { history ->
                _uiState.value = _uiState.value.copy(
                    messages = history.reversed(),
                    isLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val userMsg = AiMessageDto(role = "USER", content = content)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            isSending = true
        )

        viewModelScope.launch {
            repository.chatWithAi(content).onSuccess { aiMsg ->
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMsg,
                    isSending = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAiHistory().onSuccess {
                _uiState.value = _uiState.value.copy(messages = emptyList())
            }
        }
    }
}
