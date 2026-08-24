package com.pigeonpost.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pigeonpost.data.model.Message
import com.pigeonpost.data.model.MessageStatus
import com.pigeonpost.data.model.User
import com.pigeonpost.data.repository.AuthRepository
import com.pigeonpost.data.repository.MessageRepository
import com.pigeonpost.data.repository.UserRepository
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
    private val userRepository: UserRepository,
    private val deliverySimulator: DeliverySimulator,
    private val calculator: PigeonDeliveryCalculator,
    private val locationProvider: LocationProvider,
    private val soundManager: SoundManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    companion object {
        /**
         * Distance used when the recipient has never stored coordinates.
         * Keeps the flight meaningful (~8h20m at 60 km/h) instead of collapsing
         * into a zero-distance instant delivery.
         */
        const val FALLBACK_DISTANCE_KM = 500.0

        /** Approximate kilometres per degree of latitude. */
        private const val KM_PER_DEGREE_LATITUDE = 111.32

        /** Tolerance for treating stored coordinates as "unset" (0,0). */
        private const val COORDINATE_EPSILON = 0.0001
    }

    private val otherUserId: String = savedStateHandle["userId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Recipient profile, loaded once and reused for name + coordinates. */
    private var recipientProfile: User? = null

    init {
        val currentUser = authRepository.getCurrentUser()
        _uiState.update { it.copy(currentUserId = currentUser?.id ?: "") }
        loadRecipientProfile()
        loadMessages()
        observeIncomingMessages()
    }

    /**
     * Load the recipient's profile so the top bar shows their real display name
     * and messages can be routed over a real geographic distance.
     */
    private fun loadRecipientProfile() {
        if (otherUserId.isBlank()) return
        viewModelScope.launch {
            val profile = userRepository.getUserById(otherUserId)
            recipientProfile = profile
            val name = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: profile?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            if (name != null) {
                _uiState.update { it.copy(otherUserName = name) }
            }
        }
    }

    /**
     * Resolve the receiver's flight destination.
     *
     * Uses the recipient's last known coordinates when available. When they have
     * never stored a location (0,0), falls back to a point [FALLBACK_DISTANCE_KM]
     * due north (or south near the pole) of the sender, so the pigeon still has a
     * real journey to make.
     */
    private fun resolveReceiverPosition(
        senderLat: Double,
        senderLng: Double
    ): Pair<Double, Double> {
        val profile = recipientProfile
        val hasKnownLocation = profile != null &&
            (kotlin.math.abs(profile.lastLatitude) > COORDINATE_EPSILON ||
                kotlin.math.abs(profile.lastLongitude) > COORDINATE_EPSILON)

        if (hasKnownLocation && profile != null) {
            return profile.lastLatitude to profile.lastLongitude
        }

        val latOffset = FALLBACK_DISTANCE_KM / KM_PER_DEGREE_LATITUDE
        val fallbackLat = if (senderLat + latOffset > 85.0) {
            senderLat - latOffset
        } else {
            senderLat + latOffset
        }
        return fallbackLat to senderLng
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

            // Persist our own coordinates so the other party can compute a real
            // return distance for their pigeons.
            if (currentUserId.isNotBlank()) {
                userRepository.updateMyLocation(currentUserId, senderLat, senderLng)
            }

            // Ensure we have the recipient's profile before computing the route.
            if (recipientProfile == null && otherUserId.isNotBlank()) {
                recipientProfile = userRepository.getUserById(otherUserId)
            }

            // Receiver coordinates come from the recipient's stored profile
            val (receiverLat, receiverLng) = resolveReceiverPosition(senderLat, senderLng)

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
                notificationHelper.showPigeonLostNotification(
                    receiverName = _uiState.value.otherUserName
                )
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
