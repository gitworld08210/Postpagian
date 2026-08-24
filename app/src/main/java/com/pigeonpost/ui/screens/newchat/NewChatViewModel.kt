package com.pigeonpost.ui.screens.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pigeonpost.data.model.User
import com.pigeonpost.data.repository.AuthRepository
import com.pigeonpost.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewChatUiState(
    val allUsers: List<User> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Users matching the current search query, filtered by display name or email.
     */
    val visibleUsers: List<User>
        get() {
            val query = searchQuery.trim()
            if (query.isEmpty()) return allUsers
            return allUsers.filter { user ->
                user.displayName.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true)
            }
        }
}

/**
 * Supplies the roster of fellow messengers a pigeon may be dispatched to.
 */
@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewChatUiState())
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUserId = authRepository.getCurrentUser()?.id ?: ""
            userRepository.getAllUsersExcept(currentUserId).fold(
                onSuccess = { users ->
                    _uiState.update {
                        it.copy(
                            allUsers = users.sortedBy { user ->
                                user.displayName.ifBlank { user.email }.lowercase()
                            },
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(error = e.message, isLoading = false)
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
