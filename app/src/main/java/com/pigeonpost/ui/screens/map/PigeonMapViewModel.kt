package com.pigeonpost.ui.screens.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pigeonpost.data.model.Message
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
     * Loads the message from the repository and calculates the current pigeon position
     * based on elapsed time since the message was sent.
     */
    private fun loadMessageAndTrack() {
        viewModelScope.launch {
            // Retrieve the message from the local messages list via repository
            // Use the message's actual coordinates and sentAt time
            val message = messageRepository.getMessageById(messageId)

            if (message == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Message not found")
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    senderLat = message.senderLat,
                    senderLng = message.senderLng,
                    receiverLat = message.receiverLat,
                    receiverLng = message.receiverLng,
                    pigeonLat = message.pigeonCurrentLat,
                    pigeonLng = message.pigeonCurrentLng,
                    status = message.status
                )
            }

            // If already delivered or lost, show final state
            if (message.status == MessageStatus.DELIVERED || message.status == MessageStatus.LOST) {
                val totalDistance = calculator.calculateDistance(
                    message.senderLat, message.senderLng,
                    message.receiverLat, message.receiverLng
                )
                val totalDeliveryMs = calculator.calculateDeliveryTimeMs(totalDistance)
                val elapsedMs = System.currentTimeMillis() - message.sentAt
                val progress = calculator.calculateProgress(elapsedMs, totalDeliveryMs)

                _uiState.update {
                    it.copy(
                        progress = if (message.status == MessageStatus.DELIVERED) 1.0 else progress,
                        estimatedHoursRemaining = 0.0
                    )
                }
                return@launch
            }

            // For in-flight messages, calculate elapsed time and continue simulation
            startTrackingFromCurrentState(message)
        }
    }

    /**
     * Continues tracking a message that is still in flight.
     * Calculates the elapsed time since sentAt and shows the interpolated position.
     */
    private fun startTrackingFromCurrentState(message: Message) {
        val totalDistance = calculator.calculateDistance(
            message.senderLat, message.senderLng,
            message.receiverLat, message.receiverLng
        )
        val totalDeliveryMs = calculator.calculateDeliveryTimeMs(totalDistance)
        val elapsedMs = System.currentTimeMillis() - message.sentAt

        // Calculate current position based on elapsed time
        val progress = calculator.calculateProgress(elapsedMs, totalDeliveryMs)
        val (currentLat, currentLng) = calculator.calculateCurrentPosition(
            message.senderLat, message.senderLng,
            message.receiverLat, message.receiverLng,
            elapsedMs, totalDeliveryMs
        )

        val remainingDistance = totalDistance * (1.0 - progress)
        val hoursRemaining = calculator.calculateDeliveryTimeHours(remainingDistance)

        _uiState.update {
            it.copy(
                pigeonLat = currentLat,
                pigeonLng = currentLng,
                progress = progress,
                estimatedHoursRemaining = hoursRemaining
            )
        }

        // Continue with live simulation updates from current position
        viewModelScope.launch {
            deliverySimulator.simulateDelivery(message).collect { update ->
                // Only apply updates that are past the current elapsed time
                if (update.elapsedMs >= elapsedMs) {
                    val updatedRemainingDistance = totalDistance * (1.0 - update.progress)
                    val updatedHoursRemaining = calculator.calculateDeliveryTimeHours(updatedRemainingDistance)

                    _uiState.update {
                        it.copy(
                            pigeonLat = update.currentLat,
                            pigeonLng = update.currentLng,
                            progress = update.progress,
                            status = update.status,
                            estimatedHoursRemaining = updatedHoursRemaining
                        )
                    }
                }
            }
        }
    }
}
