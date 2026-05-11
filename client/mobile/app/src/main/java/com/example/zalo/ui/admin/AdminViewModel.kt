package com.example.zalo.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zalo.data.remote.dto.*
import com.example.zalo.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val stats: AdminStatsDto? = null,
    val users: List<UserDto> = emptyList(),
    val groups: List<GroupDto> = emptyList(),
    val auditLogs: List<AuditLogDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getStats().onSuccess { stats ->
                _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getUsers(0).onSuccess { users ->
                _uiState.value = _uiState.value.copy(users = users, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getGroups(0).onSuccess { groups ->
                _uiState.value = _uiState.value.copy(groups = groups, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAuditLogs(0).onSuccess { logs ->
                _uiState.value = _uiState.value.copy(auditLogs = logs, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            repository.banUser(userId, "Banned by Admin on Mobile").onSuccess {
                loadUsers()
            }
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            repository.unbanUser(userId).onSuccess {
                loadUsers()
            }
        }
    }
}
