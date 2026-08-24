package com.pigeonpost.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for whether the app may read the device location.
 *
 * [MainActivity][com.pigeonpost.MainActivity] pushes the runtime permission result in
 * here, and everything that cares - the location provider and the tracker map's
 * my-location layer - observes it instead of each re-querying the system on its own.
 */
class LocationPermissionMonitor(private val context: Context) {

    private val _granted = MutableStateFlow(hasLocationPermission())

    /** Emits true while at least one location permission is held. */
    val granted: StateFlow<Boolean> = _granted.asStateFlow()

    /** True when either fine or coarse location has been granted. */
    fun hasLocationPermission(): Boolean =
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    /** True when the user actually granted precise location. */
    fun hasFineLocationPermission(): Boolean =
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * True when the device has GPS or network positioning switched on. A granted
     * permission is useless if location services themselves are disabled.
     */
    fun isLocationEnabled(): Boolean {
        val manager = ContextCompat.getSystemService(context, LocationManager::class.java)
            ?: return false
        return runCatching {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    /** Re-reads the live permission state, e.g. after returning from system settings. */
    fun refresh() {
        _granted.value = hasLocationPermission()
    }

    /**
     * Records the outcome of a runtime permission request.
     *
     * @return true when the user granted a location permission
     */
    fun onPermissionResult(results: Map<String, Boolean>): Boolean {
        val grantedNow = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission()
        _granted.value = grantedNow
        return grantedNow
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
}
