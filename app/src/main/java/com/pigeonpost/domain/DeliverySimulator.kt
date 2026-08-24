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
 * Represents an update about a pigeon's flight status at a given moment in time.
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
 * Derives a pigeon's flight state from the real wall clock.
 *
 * There is no time compression: a pigeon flies at [PigeonDeliveryCalculator.PIGEON_SPEED_KMH]
 * (60 km/h), so a journey genuinely takes `distance / 60` hours of real time.
 *
 * The state of a message is a *pure function* of:
 *  - `sentAt`            (when the pigeon left)
 *  - `deliveryTime`      (how long the whole journey takes, in millis)
 *  - `deathAtProgress`   (the fraction of the route where this pigeon perishes, or null)
 *  - the current clock
 *
 * Because nothing is random at read time, the app can be closed and reopened hours
 * later and still show the correct position, and the sender and the recipient always
 * agree on the same fate and the same position.
 */
@Singleton
class DeliverySimulator @Inject constructor(
    private val calculator: PigeonDeliveryCalculator
) {

    companion object {
        /** How often the clock is re-read while a pigeon is in flight (1 second). */
        const val TICK_INTERVAL_MS = 1000L

        /** Earliest point of the route at which a doomed pigeon may perish. */
        const val MIN_DEATH_PROGRESS = 0.05

        /** Latest point of the route at which a doomed pigeon may perish. */
        const val MAX_DEATH_PROGRESS = 0.95

        private const val MS_PER_HOUR = 3_600_000.0
    }

    /**
     * Decides a pigeon's fate. Called **once**, by the sender, when the message is created.
     *
     * @return the fraction of the route at which the pigeon perishes
     *         ([MIN_DEATH_PROGRESS]..[MAX_DEATH_PROGRESS]), or `null` if it survives.
     *         Doomed with [PigeonDeliveryCalculator.DEATH_PROBABILITY] (7%) probability.
     */
    fun assignDeathProgress(random: Random = Random): Double? {
        if (!calculator.doesPigeonDie(random)) return null
        val span = MAX_DEATH_PROGRESS - MIN_DEATH_PROGRESS
        return (MIN_DEATH_PROGRESS + random.nextDouble() * span)
            .coerceIn(MIN_DEATH_PROGRESS, MAX_DEATH_PROGRESS)
    }

    /**
     * Total duration of the journey in milliseconds. Prefers the value persisted with
     * the message and falls back to recomputing it from the route at 60 km/h.
     */
    fun totalDeliveryMs(message: Message): Long {
        if (message.deliveryTime > 0) return message.deliveryTime
        val distanceKm = calculator.calculateDistance(
            message.senderLat, message.senderLng,
            message.receiverLat, message.receiverLng
        )
        return calculator.calculateDeliveryTimeMs(distanceKm)
    }

    /**
     * The single source of truth for a pigeon's state. Both the chat screen and the
     * tracker screen go through here, so they can never disagree.
     *
     * @param now current epoch millis (injectable for testing)
     */
    fun snapshot(message: Message, now: Long = System.currentTimeMillis()): DeliveryUpdate {
        val totalMs = totalDeliveryMs(message)
        val elapsedMs = (now - message.sentAt).coerceAtLeast(0L)

        // rawProgress = (now - sent_at) / delivery_time
        val rawProgress = if (totalMs <= 0L) {
            1.0
        } else {
            (elapsedMs.toDouble() / totalMs).coerceIn(0.0, 1.0)
        }

        val deathAt = message.deathAtProgress

        // A doomed pigeon freezes forever at its death point.
        if (deathAt != null && rawProgress >= deathAt) {
            val frozen = deathAt.coerceIn(0.0, 1.0)
            val (lat, lng) = positionAt(message, frozen)
            return DeliveryUpdate(
                messageId = message.id,
                currentLat = lat,
                currentLng = lng,
                progress = frozen,
                status = MessageStatus.LOST,
                elapsedMs = (frozen * totalMs).toLong()
            )
        }

        if (rawProgress >= 1.0) {
            return DeliveryUpdate(
                messageId = message.id,
                currentLat = message.receiverLat,
                currentLng = message.receiverLng,
                progress = 1.0,
                status = MessageStatus.DELIVERED,
                elapsedMs = totalMs
            )
        }

        val (lat, lng) = positionAt(message, rawProgress)
        return DeliveryUpdate(
            messageId = message.id,
            currentLat = lat,
            currentLng = lng,
            progress = rawProgress,
            status = MessageStatus.FLYING,
            elapsedMs = elapsedMs
        )
    }

    /**
     * Real hours of flight still remaining, based on the wall clock. Zero once the
     * flight has reached a terminal state.
     */
    fun hoursRemaining(message: Message, now: Long = System.currentTimeMillis()): Double {
        val update = snapshot(message, now)
        if (update.status != MessageStatus.FLYING) return 0.0
        val totalMs = totalDeliveryMs(message)
        val remainingMs = (totalMs - update.elapsedMs).coerceAtLeast(0L)
        return remainingMs / MS_PER_HOUR
    }

    /** True once the flight can no longer change. */
    fun isTerminal(status: MessageStatus): Boolean = status != MessageStatus.FLYING

    /**
     * Tracks a delivery in real time. Emits the current state immediately, then
     * re-reads the clock every [TICK_INTERVAL_MS] and recomputes. Completes as soon
     * as the flight reaches a terminal state (DELIVERED or LOST), so no ticker is
     * left running for a finished journey.
     *
     * @param clock supplies the current epoch millis (injectable for testing)
     */
    fun trackDelivery(
        message: Message,
        clock: () -> Long = { System.currentTimeMillis() }
    ): Flow<DeliveryUpdate> = flow {
        while (true) {
            val update = snapshot(message, clock())
            emit(update)
            if (isTerminal(update.status)) return@flow
            delay(TICK_INTERVAL_MS)
        }
    }

    private fun positionAt(message: Message, progress: Double): Pair<Double, Double> {
        return calculator.calculateCurrentPosition(
            message.senderLat, message.senderLng,
            message.receiverLat, message.receiverLng,
            elapsedMs = (progress * PROGRESS_SCALE).toLong(),
            totalDeliveryMs = PROGRESS_SCALE
        )
    }
}

/** Fixed denominator used to express a progress fraction as an elapsed/total pair. */
private const val PROGRESS_SCALE = 1_000_000L
