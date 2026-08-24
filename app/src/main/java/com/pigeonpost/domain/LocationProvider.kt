package com.pigeonpost.domain

/**
 * Why the reported position is not the device's real one.
 *
 * Each case carries the message shown to the user, so they always know when a pigeon
 * is departing from a guessed roost rather than from where they actually stand.
 */
enum class LocationFallbackReason(val message: String) {
    /** The user declined (or has not yet granted) the runtime location permission. */
    PERMISSION_DENIED(
        "Location permission denied - thy pigeon departs from an approximate roost."
    ),

    /** Location services are switched off on the device. */
    LOCATION_DISABLED(
        "Location services are off - thy pigeon departs from an approximate roost."
    ),

    /** Permission granted, services on, but no fix arrived in time. */
    NO_FIX(
        "No location fix could be obtained - thy pigeon departs from an approximate roost."
    ),

    /** Something went wrong talking to the location services. */
    ERROR(
        "Thy whereabouts could not be determined - thy pigeon departs from an approximate roost."
    )
}

/**
 * A resolved position, plus whether it is genuinely the device's location.
 *
 * @param latitude latitude to dispatch the pigeon from
 * @param longitude longitude to dispatch the pigeon from
 * @param fallbackReason `null` when this is a real device fix, otherwise why it is not
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val fallbackReason: LocationFallbackReason? = null
) {
    /** True when the coordinates are a default rather than the device's real position. */
    val isApproximate: Boolean get() = fallbackReason != null
}

/**
 * Provides the current device location. Abstracted behind an interface so it can be
 * injected with the real FusedLocationProviderClient implementation in the app, or with
 * [DefaultLocationProvider] for testing/demo purposes.
 */
interface LocationProvider {
    /**
     * Returns the current device location as a latitude/longitude pair.
     * Falls back to a default location if the actual location is unavailable.
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>

    /**
     * Same lookup as [getCurrentLocation], but also reports whether the coordinates are
     * the device's real position or an approximate fallback. Implementations that cannot
     * read real hardware simply report the fallback reason.
     */
    suspend fun getCurrentLocationFix(): LocationFix {
        val (latitude, longitude) = getCurrentLocation()
        return LocationFix(latitude, longitude, LocationFallbackReason.NO_FIX)
    }
}

/**
 * Fixed-location implementation used by tests and previews, and as the coordinates every
 * real implementation falls back to when the device cannot be located.
 */
class DefaultLocationProvider : LocationProvider {

    companion object {
        // Default location (Los Angeles) used when actual location is unavailable
        const val DEFAULT_LAT = 34.0522
        const val DEFAULT_LNG = -118.2437
    }

    override suspend fun getCurrentLocation(): Pair<Double, Double> {
        return Pair(DEFAULT_LAT, DEFAULT_LNG)
    }
}
