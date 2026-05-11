package com.example.zalo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zalo.data.remote.dto.LoginRequest
import com.example.zalo.data.remote.dto.RegisterRequest
import com.example.zalo.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.login(request)
            if (result.isSuccess) {
                _uiState.value = AuthUiState(isSuccess = true)
            } else {
                _uiState.value = AuthUiState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.register(request)
            if (result.isSuccess) {
                _uiState.value = AuthUiState(isSuccess = true)
            } else {
                _uiState.value = AuthUiState(error = result.exceptionOrNull()?.message)
            }
        }
    }
}
