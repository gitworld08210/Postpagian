package com.pigeonpost.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pigeonpost.data.model.Message
import com.pigeonpost.data.model.MessageStatus
import com.pigeonpost.data.repository.AuthRepository
import com.pigeonpost.data.repository.MessageRepository
import com.pigeonpost.domain.DeliverySimulator
import com.pigeonpost.domain.DeliveryUpdate
import com.pigeonpost.domain.LocationProvider
import com.pigeonpost.domain.PigeonDeliveryCalculator
import com.pigeonpost.utils.NotificationHelper
import com.pigeonpost.utils.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val otherUserName: String = "Fellow Messenger",
    val currentUserId: String = "",
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val deliverySimulator: DeliverySimulator,
    private val calculator: PigeonDeliveryCalculator,
    private val locationProvider: LocationProvider,
    private val soundManager: SoundManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val otherUserId: String = savedStateHandle["userId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val currentUser = authRepository.getCurrentUser()
        _uiState.update { it.copy(currentUserId = currentUser?.id ?: "") }
        loadMessages()
        observeIncomingMessages()
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank()) return

        val currentUserId = _uiState.value.currentUserId

        viewModelScope.launch {
            // Get actual sender location from location provider
            val (senderLat, senderLng) = locationProvider.getCurrentLocation()

            // Receiver coordinates - in production these would come from receiver's profile
            // For now, use a different default to demonstrate distance-based delivery
            val receiverLat = PigeonDeliveryCalculator.NYC_LAT
            val receiverLng = PigeonDeliveryCalculator.NYC_LNG

            val distance = calculator.calculateDistance(senderLat, senderLng, receiverLat, receiverLng)
            val deliveryTimeMs = calculator.calculateDeliveryTimeMs(distance)

            val message = Message(
                id = UUID.randomUUID().toString(),
                senderId = currentUserId,
                receiverId = otherUserId,
                content = content,
                sentAt = System.currentTimeMillis(),
                deliveryTime = deliveryTimeMs,
                status = MessageStatus.FLYING,
                senderLat = senderLat,
                senderLng = senderLng,
                receiverLat = receiverLat,
                receiverLng = receiverLng,
                pigeonCurrentLat = senderLat,
                pigeonCurrentLng = senderLng
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + message,
                    inputText = ""
                )
            }

            soundManager.playPigeonCoo()

            messageRepository.sendMessage(message)
            simulateDelivery(message)
        }
    }

    private fun simulateDelivery(message: Message) {
        viewModelScope.launch {
            deliverySimulator.simulateDelivery(message).collect { update ->
                handleDeliveryUpdate(update)
            }
        }
    }

    private fun handleDeliveryUpdate(update: DeliveryUpdate) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == update.messageId) {
                    msg.copy(
                        status = update.status,
                        pigeonCurrentLat = update.currentLat,
                        pigeonCurrentLng = update.currentLng
                    )
                } else msg
            }
            state.copy(messages = updatedMessages)
        }

        when (update.status) {
            MessageStatus.DELIVERED -> {
                viewModelScope.launch {
                    messageRepository.updateMessageStatus(update.messageId, MessageStatus.DELIVERED)
                }
                soundManager.playDeliveryChime()
                notificationHelper.showDeliveryNotification(
                    senderName = "Your Pigeon",
                    messagePreview = "Message delivered by faithful pigeon"
                )
            }
            MessageStatus.LOST -> {
                viewModelScope.launch {
                    messageRepository.updateMessageStatus(update.messageId, MessageStatus.LOST)
                }
                soundManager.playDeathSound()
                notificationHelper.showPigeonLostNotification(receiverName = "your recipient")
            }
            else -> { /* Flying - no action */ }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = _uiState.value.currentUserId
            messageRepository.getMessages(userId, otherUserId).fold(
                onSuccess = { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            val userId = _uiState.value.currentUserId
            try {
                messageRepository.observeMessages(userId).collect { message ->
                    if (message.senderId == otherUserId) {
                        _uiState.update { it.copy(messages = it.messages + message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Lost connection to message stream. Pull to refresh.")
                }
            }
        }
    }
}
