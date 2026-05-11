package com.example.zalo.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zalo.data.remote.dto.UserDto
import com.example.zalo.domain.repository.ChatRepository
import com.example.zalo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGroupUiState(
    val contacts: List<UserDto> = emptyList(),
    val selectedUserIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userRepository.getContacts().onSuccess { contacts ->
                _uiState.value = _uiState.value.copy(contacts = contacts, isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleSelection(userId: String) {
        val currentSelection = _uiState.value.selectedUserIds.toMutableSet()
        if (currentSelection.contains(userId)) {
            currentSelection.remove(userId)
        } else {
            currentSelection.add(userId)
        }
        _uiState.value = _uiState.value.copy(selectedUserIds = currentSelection)
    }

    fun createGroup(name: String, onCreated: (String) -> Unit) {
        if (name.isBlank() || _uiState.value.selectedUserIds.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            chatRepository.createGroup(name, _uiState.value.selectedUserIds.toList()).onSuccess { group ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                onCreated(group.id)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
