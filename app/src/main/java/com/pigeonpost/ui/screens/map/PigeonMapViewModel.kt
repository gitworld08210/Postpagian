package com.pigeonpost.ui.screens.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pigeonpost.data.model.MessageStatus
import com.pigeonpost.data.repository.MessageRepository
import com.pigeonpost.domain.DeliverySimulator
import com.pigeonpost.domain.PigeonDeliveryCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PigeonMapUiState(
    val pigeonLat: Double = 0.0,
    val pigeonLng: Double = 0.0,
    val senderLat: Double = 0.0,
    val senderLng: Double = 0.0,
    val receiverLat: Double = 0.0,
    val receiverLng: Double = 0.0,
    val progress: Double = 0.0,
    val status: MessageStatus = MessageStatus.FLYING,
    val estimatedHoursRemaining: Double = 0.0,
    val distanceKm: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PigeonMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val deliverySimulator: DeliverySimulator,
    private val calculator: PigeonDeliveryCalculator
) : ViewModel() {

    private val messageId: String = savedStateHandle["messageId"] ?: ""

    private val _uiState = MutableStateFlow(PigeonMapUiState())
    val uiState: StateFlow<PigeonMapUiState> = _uiState.asStateFlow()

    init {
        loadMessageAndTrack()
    }

    /**
     * Loads the message and tracks it against the real wall clock.
     *
     * The pigeon's position, progress and fate all come from [DeliverySimulator] - the
     * exact same logic the chat screen uses - so the two screens can never disagree.
     */
    private fun loadMessageAndTrack() {
        viewModelScope.launch {
            val message = messageRepository.getMessageById(messageId)

            if (message == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Message not found")
                }
                return@launch
            }

            val distanceKm = calculator.calculateDistance(
                message.senderLat, message.senderLng,
                message.receiverLat, message.receiverLng
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    senderLat = message.senderLat,
                    senderLng = message.senderLng,
                    receiverLat = message.receiverLat,
                    receiverLng = message.receiverLng,
                    distanceKm = distanceKm
                )
            }

            // Emits the current state immediately, then re-reads the clock once a second
            // and completes as soon as the flight is delivered or lost.
            deliverySimulator.trackDelivery(message).collect { update ->
                _uiState.update {
                    it.copy(
                        pigeonLat = update.currentLat,
                        pigeonLng = update.currentLng,
                        progress = update.progress,
                        status = update.status,
                        estimatedHoursRemaining = if (update.status == MessageStatus.FLYING) {
                            deliverySimulator.hoursRemaining(message)
                        } else {
                            0.0
                        }
                    )
                }
            }

            // The journey finished while we were watching (or before we arrived):
            // persist the outcome so every client reads the same terminal status.
            val finalStatus = _uiState.value.status
            if (finalStatus != MessageStatus.FLYING && message.status != finalStatus) {
                messageRepository.updateMessageStatus(messageId, finalStatus)
            }
        }
    }
}
