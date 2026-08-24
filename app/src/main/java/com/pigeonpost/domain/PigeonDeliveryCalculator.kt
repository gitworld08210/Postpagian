package com.pigeonpost.domain

import com.pigeonpost.utils.LocationUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Calculates pigeon delivery parameters including travel time, current position,
 * and whether the pigeon dies during transit.
 *
 * Pigeon speed: 60 km/h
 * Death probability: 7% base chance per delivery
 */
@Singleton
class PigeonDeliveryCalculator @Inject constructor() {

    companion object {
        /** Pigeon flight speed in km/h */
        const val PIGEON_SPEED_KMH = 60.0

        /** Base probability of pigeon death per delivery (7%) */
        const val DEATH_PROBABILITY = 0.07

        // Example: LA to NYC coordinates
        const val LA_LAT = 34.0522
        const val LA_LNG = -118.2437
        const val NYC_LAT = 40.7128
        const val NYC_LNG = -74.0060
    }

    /**
     * Calculates the distance in kilometers between sender and receiver.
     */
    fun calculateDistance(
        senderLat: Double, senderLng: Double,
        receiverLat: Double, receiverLng: Double
    ): Double {
        return LocationUtils.haversineDistance(senderLat, senderLng, receiverLat, receiverLng)
    }

    /**
     * Calculates delivery time in milliseconds based on distance.
     * Uses pigeon speed of 60 km/h.
     */
    fun calculateDeliveryTimeMs(distanceKm: Double): Long {
        val hours = distanceKm / PIGEON_SPEED_KMH
        return (hours * 3600 * 1000).toLong()
    }

    /**
     * Calculates delivery time in hours based on distance.
     */
    fun calculateDeliveryTimeHours(distanceKm: Double): Double {
        return distanceKm / PIGEON_SPEED_KMH
    }

    /**
     * Determines if the pigeon dies during this delivery.
     * Uses a 7% base chance (between 5-10% range).
     * @param random Random instance for testability.
     */
    fun doesPigeonDie(random: Random = Random): Boolean {
        return random.nextDouble() < DEATH_PROBABILITY
    }

    /**
     * Calculates the pigeon's current position based on elapsed time.
     * @param senderLat Sender's latitude
     * @param senderLng Sender's longitude
     * @param receiverLat Receiver's latitude
     * @param receiverLng Receiver's longitude
     * @param elapsedMs Time elapsed since sending in milliseconds
     * @param totalDeliveryMs Total expected delivery time in milliseconds
     * @return Pair of (latitude, longitude) for pigeon's current position
     */
    fun calculateCurrentPosition(
        senderLat: Double, senderLng: Double,
        receiverLat: Double, receiverLng: Double,
        elapsedMs: Long, totalDeliveryMs: Long
    ): Pair<Double, Double> {
        val progress = if (totalDeliveryMs > 0) {
            (elapsedMs.toDouble() / totalDeliveryMs).coerceIn(0.0, 1.0)
        } else {
            1.0
        }
        return LocationUtils.interpolatePosition(
            senderLat, senderLng,
            receiverLat, receiverLng,
            progress
        )
    }

    /**
     * Calculates the flight progress as a fraction between 0.0 and 1.0.
     */
    fun calculateProgress(elapsedMs: Long, totalDeliveryMs: Long): Double {
        if (totalDeliveryMs <= 0) return 1.0
        return (elapsedMs.toDouble() / totalDeliveryMs).coerceIn(0.0, 1.0)
    }
}
