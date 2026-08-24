package com.pigeonpost.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions for geographic distance and interpolation calculations.
 */
object LocationUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates the distance between two geographic coordinates using the Haversine formula.
     * @return Distance in kilometers.
     */
    fun haversineDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Linearly interpolates between two geographic coordinates.
     * @param progress Value between 0.0 (start) and 1.0 (end).
     * @return Pair of (latitude, longitude) at the interpolated position.
     */
    fun interpolatePosition(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
        progress: Double
    ): Pair<Double, Double> {
        val clampedProgress = progress.coerceIn(0.0, 1.0)
        val lat = startLat + (endLat - startLat) * clampedProgress
        val lng = startLng + (endLng - startLng) * clampedProgress
        return Pair(lat, lng)
    }
}
