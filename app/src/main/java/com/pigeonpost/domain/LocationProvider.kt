package com.pigeonpost.domain

/**
 * Provides the current device location. Abstracted behind an interface
 * so it can be injected with a real FusedLocationProvider implementation
 * or a mock for testing/demo purposes.
 */
interface LocationProvider {
    /**
     * Returns the current device location as a latitude/longitude pair.
     * Falls back to a default location if the actual location is unavailable.
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>
}

/**
 * Default implementation that returns a mock location.
 * In production, this should be replaced with a FusedLocationProviderClient-based
 * implementation that reads the actual device GPS coordinates.
 */
class DefaultLocationProvider : LocationProvider {

    companion object {
        // Default location (Los Angeles) used when actual location is unavailable
        private const val DEFAULT_LAT = 34.0522
        private const val DEFAULT_LNG = -118.2437
    }

    override suspend fun getCurrentLocation(): Pair<Double, Double> {
        return Pair(DEFAULT_LAT, DEFAULT_LNG)
    }
}
