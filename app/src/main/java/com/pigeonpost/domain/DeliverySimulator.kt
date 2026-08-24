package com.pigeonpost.domain

import com.pigeonpost.data.model.Message
import com.pigeonpost.data.model.MessageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Represents an update from the delivery simulator about a pigeon's flight status.
 */
data class DeliveryUpdate(
    val messageId: String,
    val currentLat: Double,
    val currentLng: Double,
    val progress: Double,
    val status: MessageStatus,
    val elapsedMs: Long
)

/**
 * Coroutine-based delivery simulator that tracks pigeon position over time.
 * Emits position updates via Flow, simulates pigeon death at random intervals.
 */
@Singleton
class DeliverySimulator @Inject constructor(
    private val calculator: PigeonDeliveryCalculator
) {

    companion object {
        /** Update interval in milliseconds (every 5 seconds for simulation) */
        const val UPDATE_INTERVAL_MS = 5000L

        /** Simulated time step per update (30 minutes of flight per update tick) */
        const val SIMULATED_TIME_STEP_MS = 30 * 60 * 1000L
    }

    /**
     * Simulates a pigeon delivery, emitting position updates as a Flow.
     * The pigeon may die at any point during the journey (7% total chance spread across updates).
     *
     * @param message The message being delivered
     * @param random Random instance for death check (injectable for testing)
     * @return Flow of DeliveryUpdate
     */
    fun simulateDelivery(
        message: Message,
        random: Random = Random
    ): Flow<DeliveryUpdate> = flow {
        val distanceKm = calculator.calculateDistance(
            message.senderLat, message.senderLng,
            message.receiverLat, message.receiverLng
        )
        val totalDeliveryMs = calculator.calculateDeliveryTimeMs(distanceKm)

        if (totalDeliveryMs <= 0) {
            // Instant delivery for same location
            emit(
                DeliveryUpdate(
                    messageId = message.id,
                    currentLat = message.receiverLat,
                    currentLng = message.receiverLng,
                    progress = 1.0,
                    status = MessageStatus.DELIVERED,
                    elapsedMs = 0
                )
            )
            return@flow
        }

        // Calculate how many update steps the journey will take
        val totalSteps = (totalDeliveryMs / SIMULATED_TIME_STEP_MS).toInt().coerceAtLeast(1)
        // Spread death probability across all steps
        val deathProbabilityPerStep = 1.0 - Math.pow(1.0 - PigeonDeliveryCalculator.DEATH_PROBABILITY, 1.0 / totalSteps)

        var elapsedMs = 0L

        while (elapsedMs < totalDeliveryMs) {
            elapsedMs += SIMULATED_TIME_STEP_MS

            // Check for pigeon death at this step
            if (random.nextDouble() < deathProbabilityPerStep) {
                val (deathLat, deathLng) = calculator.calculateCurrentPosition(
                    message.senderLat, message.senderLng,
                    message.receiverLat, message.receiverLng,
                    elapsedMs, totalDeliveryMs
                )
                emit(
                    DeliveryUpdate(
                        messageId = message.id,
                        currentLat = deathLat,
                        currentLng = deathLng,
                        progress = calculator.calculateProgress(elapsedMs, totalDeliveryMs),
                        status = MessageStatus.LOST,
                        elapsedMs = elapsedMs
                    )
                )
                return@flow
            }

            // Emit position update
            val (currentLat, currentLng) = calculator.calculateCurrentPosition(
                message.senderLat, message.senderLng,
                message.receiverLat, message.receiverLng,
                elapsedMs, totalDeliveryMs
            )
            val progress = calculator.calculateProgress(elapsedMs, totalDeliveryMs)

            emit(
                DeliveryUpdate(
                    messageId = message.id,
                    currentLat = currentLat,
                    currentLng = currentLng,
                    progress = progress,
                    status = if (progress >= 1.0) MessageStatus.DELIVERED else MessageStatus.FLYING,
                    elapsedMs = elapsedMs
                )
            )

            if (progress >= 1.0) return@flow

            delay(UPDATE_INTERVAL_MS)
        }

        // Final delivery
        emit(
            DeliveryUpdate(
                messageId = message.id,
                currentLat = message.receiverLat,
                currentLng = message.receiverLng,
                progress = 1.0,
                status = MessageStatus.DELIVERED,
                elapsedMs = totalDeliveryMs
            )
        )
    }
}
