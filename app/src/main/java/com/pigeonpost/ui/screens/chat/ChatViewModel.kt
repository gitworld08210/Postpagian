package com.pigeonpost.ui.screens.chat

import android.content.Context
import android.net.Uri
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
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
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
    val error: String? = null,
    val pendingAttachmentUri: String? = null,
    val pendingAttachmentName: String? = null,
    val isUploading: Boolean = false,
    val deliveryAnimationMessage: Message? = null,
    /**
     * Set when the last pigeon was dispatched from a fallback position instead of the
     * device's real one, so the user is told rather than silently misplaced.
     */
    val locationNotice: String? = null
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
    private val notificationHelper: NotificationHelper,
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val appContext: Context
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

    /** Messages that already have a real-time ticker running, so we never double-track. */
    private val trackedMessageIds = mutableSetOf<String>()

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

    fun setAttachment(uri: Uri, fileName: String?) {
        _uiState.update {
            it.copy(
                pendingAttachmentUri = uri.toString(),
                pendingAttachmentName = fileName ?: "attachment"
            )
        }
    }

    fun clearAttachment() {
        _uiState.update { it.copy(pendingAttachmentUri = null, pendingAttachmentName = null) }
    }

    fun dismissDeliveryAnimation() {
        _uiState.update { it.copy(deliveryAnimationMessage = null) }
    }

    /** Dismisses the "approximate roost" notice after the user has read it. */
    fun dismissLocationNotice() {
        _uiState.update { it.copy(locationNotice = null) }
    }

    private suspend fun uploadAttachment(messageId: String, uri: Uri, fileName: String): String? {
        return try {
            val currentUserId = _uiState.value.currentUserId
            val contentResolver = appContext.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return null
            val path = "$currentUserId/$messageId/$fileName"
            supabaseClient.storage.from("pigeon-attachments").upload(path, bytes)
            val publicUrl = supabaseClient.storage.from("pigeon-attachments").publicUrl(path)
            publicUrl
        } catch (e: Exception) {
            null
        }
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        val attachmentUri = _uiState.value.pendingAttachmentUri
        val attachmentName = _uiState.value.pendingAttachmentName

        if (content.isBlank() && attachmentUri == null) return

        val currentUserId = _uiState.value.currentUserId

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = attachmentUri != null) }

            // The device's real GPS position, or a flagged fallback if it cannot be read.
            val fix = locationProvider.getCurrentLocationFix()
            val senderLat = fix.latitude
            val senderLng = fix.longitude
            _uiState.update { it.copy(locationNotice = fix.fallbackReason?.message) }

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

            // The sender decides this pigeon's fate exactly once, here. It is persisted
            // with the message so every client derives the same outcome forever after.
            val deathAtProgress = deliverySimulator.assignDeathProgress()

            val messageId = UUID.randomUUID().toString()

            // Upload attachment if present
            var attachmentUrl: String? = null
            if (attachmentUri != null && attachmentName != null) {
                attachmentUrl = uploadAttachment(messageId, Uri.parse(attachmentUri), attachmentName)
            }

            val message = Message(
                id = messageId,
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
                pigeonCurrentLng = senderLng,
                attachmentUrl = attachmentUrl,
                deathAtProgress = deathAtProgress
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + message,
                    inputText = "",
                    pendingAttachmentUri = null,
                    pendingAttachmentName = null,
                    isUploading = false
                )
            }

            soundManager.playPigeonCoo()

            messageRepository.sendMessage(message)
            trackDelivery(message)
        }
    }

    /**
     * Re-derives every message's flight state from the real clock and starts a
     * real-time ticker for the ones still in the air. Messages that already reached a
     * terminal state while the app was closed are applied silently - their sounds and
     * notifications already happened, and their fate must not be re-rolled.
     */
    private fun refreshFlightStates(messages: List<Message>): List<Message> {
        val now = System.currentTimeMillis()
        return messages.map { message ->
            val update = deliverySimulator.snapshot(message, now)
            val settled = message.copy(
                status = update.status,
                pigeonCurrentLat = update.currentLat,
                pigeonCurrentLng = update.currentLng
            )
            if (update.status == MessageStatus.FLYING) {
                trackDelivery(settled)
            } else if (message.status == MessageStatus.FLYING) {
                // The journey finished while nobody was watching - persist the outcome.
                trackedMessageIds.add(message.id)
                viewModelScope.launch {
                    messageRepository.updateMessageStatus(message.id, update.status)
                }
            }
            settled
        }
    }

    /**
     * Starts a wall-clock ticker for a single in-flight message. Idempotent.
     */
    private fun trackDelivery(message: Message) {
        if (!trackedMessageIds.add(message.id)) return
        viewModelScope.launch {
            deliverySimulator.trackDelivery(message).collect { update ->
                handleDeliveryUpdate(update)
            }
        }
    }

    private fun handleDeliveryUpdate(update: DeliveryUpdate) {
        var previousStatus: MessageStatus? = null
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == update.messageId) {
                    previousStatus = msg.status
                    msg.copy(
                        status = update.status,
                        pigeonCurrentLat = update.currentLat,
                        pigeonCurrentLng = update.currentLng
                    )
                } else msg
            }
            state.copy(messages = updatedMessages)
        }

        // Only fire sounds/notifications/animations on a real transition out of FLYING,
        // never when simply re-reading an already finished flight.
        if (previousStatus != null && previousStatus != MessageStatus.FLYING) return

        when (update.status) {
            MessageStatus.DELIVERED -> {
                viewModelScope.launch {
                    messageRepository.updateMessageStatus(update.messageId, MessageStatus.DELIVERED)
                }
                soundManager.playDeliveryChime()

                // Trigger delivery animation for incoming messages
                val message = _uiState.value.messages.find { it.id == update.messageId }
                if (message != null && message.senderId != _uiState.value.currentUserId) {
                    _uiState.update { it.copy(deliveryAnimationMessage = message) }
                    viewModelScope.launch {
                        delay(3000L)
                        _uiState.update { state ->
                            if (state.deliveryAnimationMessage?.id == update.messageId) {
                                state.copy(deliveryAnimationMessage = null)
                            } else state
                        }
                    }
                }

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
                    // Derive each pigeon's real current state from the wall clock, so
                    // reopening the chat hours later shows the truth.
                    val settled = refreshFlightStates(messages)
                    _uiState.update { it.copy(messages = settled, isLoading = false) }
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
                        val update = deliverySimulator.snapshot(message)
                        val settled = message.copy(
                            status = update.status,
                            pigeonCurrentLat = update.currentLat,
                            pigeonCurrentLng = update.currentLng
                        )
                        _uiState.update { it.copy(messages = it.messages + settled) }
                        if (update.status == MessageStatus.FLYING) {
                            trackDelivery(settled)
                        }
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
