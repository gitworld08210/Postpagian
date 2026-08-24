package com.pigeonpost

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pigeonpost.data.repository.AuthRepository
import com.pigeonpost.navigation.PigeonPostNavGraph
import com.pigeonpost.ui.theme.PigeonPostTheme
import com.pigeonpost.utils.LocationPermissionMonitor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var locationPermissionMonitor: LocationPermissionMonitor

    /**
     * Runtime permission result. The outcome is recorded in
     * [LocationPermissionMonitor] so the location provider and the tracker map's
     * my-location layer both react to it, and a refusal is explained to the user
     * rather than being silently swallowed.
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = locationPermissionMonitor.onPermissionResult(permissions)
        if (!granted) {
            Toast.makeText(
                this,
                "Without thy whereabouts, pigeons depart from an approximate roost.",
                Toast.LENGTH_LONG
            ).show()
        } else if (!locationPermissionMonitor.isLocationEnabled()) {
            Toast.makeText(
                this,
                "Location services are off - pigeons depart from an approximate roost.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            PigeonPostTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    PigeonPostNavGraph(
                        navController = navController,
                        authRepository = authRepository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have granted or revoked location in system settings while away.
        locationPermissionMonitor.refresh()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Already granted: nothing to ask, just make sure the monitor agrees.
        if (locationPermissionMonitor.hasLocationPermission() &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            locationPermissionMonitor.refresh()
            return
        }

        locationPermissionRequest.launch(permissions.toTypedArray())
    }
}
