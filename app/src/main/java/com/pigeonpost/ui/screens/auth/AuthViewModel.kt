package com.pigeonpost.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pigeonpost.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val forgotPasswordLoading: Boolean = false,
    val forgotPasswordMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isAuthenticated = authRepository.isAuthenticated()) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isSignUp = !it.isSignUp, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }

        // Email format validation
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailPattern.matches(state.email.trim())) {
            _uiState.update { it.copy(error = "Please enter a valid email address") }
            return
        }

        // Minimum password length validation
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (state.isSignUp) {
                authRepository.signUp(state.email.trim(), state.password)
            } else {
                authRepository.signIn(state.email.trim(), state.password)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                },
                onFailure = { e ->
                    val errorMessage = if (e.message?.contains("timeout", ignoreCase = true) == true) {
                        "The guild's scribes are slow to respond. Pray try once more."
                    } else {
                        e.message ?: "Authentication failed"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            )
        }
    }

    fun showForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = true,
                forgotPasswordEmail = it.email,
                forgotPasswordMessage = null
            )
        }
    }

    fun dismissForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = false,
                forgotPasswordEmail = "",
                forgotPasswordMessage = null
            )
        }
    }

    fun updateForgotPasswordEmail(email: String) {
        _uiState.update { it.copy(forgotPasswordEmail = email, forgotPasswordMessage = null) }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.forgotPasswordEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(forgotPasswordMessage = "Please enter thy electronic address") }
            return
        }

        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailPattern.matches(email)) {
            _uiState.update { it.copy(forgotPasswordMessage = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(forgotPasswordLoading = true, forgotPasswordMessage = null) }
            val result = authRepository.sendPasswordReset(email)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            forgotPasswordLoading = false,
                            forgotPasswordMessage = "A recovery scroll has been dispatched to thy address"
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            forgotPasswordLoading = false,
                            forgotPasswordMessage = e.message ?: "Failed to send recovery scroll"
                        )
                    }
                }
            )
        }
    }
}
