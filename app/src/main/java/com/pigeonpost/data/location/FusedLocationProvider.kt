package com.pigeonpost.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.pigeonpost.domain.DefaultLocationProvider
import com.pigeonpost.domain.LocationFallbackReason
import com.pigeonpost.domain.LocationFix
import com.pigeonpost.domain.LocationProvider
import com.pigeonpost.utils.LocationPermissionMonitor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Real device location, read through Google Play services' fused location provider.
 *
 * The lookup is deliberately defensive: a pigeon must always get *some* origin, so every
 * failure path (permission refused, location services off, no fix in time, Play services
 * unavailable) resolves to the default coordinates with a [LocationFallbackReason]
 * attached, rather than throwing or hanging the send.
 */
class FusedLocationProvider(
    private val context: Context,
    private val permissionMonitor: LocationPermissionMonitor
) : LocationProvider {

    private companion object {
        /** How long to wait for a brand-new fix before giving up. */
        const val FRESH_FIX_TIMEOUT_MS = 8_000L

        /** How long to wait on the cached last-known location. */
        const val LAST_LOCATION_TIMEOUT_MS = 2_000L

        /** A cached fix older than this is treated as absent and a fresh one requested. */
        const val MAX_CACHED_AGE_MS = 10 * 60 * 1000L
    }

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun getCurrentLocation(): Pair<Double, Double> {
        val fix = getCurrentLocationFix()
        return fix.latitude to fix.longitude
    }

    override suspend fun getCurrentLocationFix(): LocationFix {
        if (!permissionMonitor.hasLocationPermission()) {
            return fallback(LocationFallbackReason.PERMISSION_DENIED)
        }

        return try {
            // A cached fix is instant and usually good enough for a 60 km/h pigeon.
            val cached = lastKnownLocation()?.takeIf { it.isFresh() }
            if (cached != null) return cached.toFix()

            // Nothing cached: ask the hardware for a new fix, but never block forever.
            if (!permissionMonitor.isLocationEnabled()) {
                return fallback(LocationFallbackReason.LOCATION_DISABLED)
            }
            freshLocation()?.toFix() ?: fallback(LocationFallbackReason.NO_FIX)
        } catch (e: SecurityException) {
            fallback(LocationFallbackReason.PERMISSION_DENIED)
        } catch (e: Exception) {
            fallback(LocationFallbackReason.ERROR)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Location? =
        withTimeoutOrNull(LAST_LOCATION_TIMEOUT_MS) {
            client.lastLocation.awaitOrNull()
        }

    @SuppressLint("MissingPermission")
    private suspend fun freshLocation(): Location? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(
                if (permissionMonitor.hasFineLocationPermission()) {
                    Priority.PRIORITY_HIGH_ACCURACY
                } else {
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                }
            )
            .setDurationMillis(FRESH_FIX_TIMEOUT_MS)
            .setMaxUpdateAgeMillis(MAX_CACHED_AGE_MS)
            .build()

        return withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS + 1_000L) {
            suspendCancellableCoroutine { continuation ->
                val cancellation = CancellationTokenSource()
                continuation.invokeOnCancellation { cancellation.cancel() }
                client.getCurrentLocation(request, cancellation.token)
                    .resumeWith(continuation)
            }
        }
    }

    private fun fallback(reason: LocationFallbackReason) = LocationFix(
        latitude = DefaultLocationProvider.DEFAULT_LAT,
        longitude = DefaultLocationProvider.DEFAULT_LNG,
        fallbackReason = reason
    )

    private fun Location.toFix() = LocationFix(latitude, longitude, fallbackReason = null)

    private fun Location.isFresh(): Boolean {
        val ageMs = SystemClock.elapsedRealtime() - elapsedRealtimeNanos / 1_000_000L
        return ageMs in 0..MAX_CACHED_AGE_MS
    }
}

/** Bridges a Play services [Task] into a coroutine, treating any failure as "no value". */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    resumeWith(continuation)
}

private fun <T> Task<T>.resumeWith(
    continuation: kotlinx.coroutines.CancellableContinuation<T?>
) {
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener {
        if (continuation.isActive) continuation.resume(null)
    }
    addOnCanceledListener {
        if (continuation.isActive) continuation.resume(null)
    }
}
