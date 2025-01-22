package com.example.summonerscompass.presentation.home_screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationManager(private val context: Context) {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationOffset: LatLng = LatLng(0.0, 0.0)
    private var lastLocation: Location? = null
    var isTeleported = false
    private var currentBearing: Float = 0f

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 100): Flow<Pair<LatLng, Float>> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(0.1f) // More frequent updates
            .setMaxUpdateDelayMillis(intervalMs)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { currentLocation ->
                    currentBearing = lastLocation?.bearingTo(currentLocation) ?: currentLocation.bearing


                    lastLocation = currentLocation

                    // If teleported, use offset. Otherwise, use real location
                    val baseLocation = LatLng(currentLocation.latitude, currentLocation.longitude)
                    val finalLocation = if (isTeleported) {
                        LatLng(
                            baseLocation.latitude + locationOffset.latitude,
                            baseLocation.longitude + locationOffset.longitude
                        )
                    } else {
                        baseLocation
                    }

                    trySend(Pair(finalLocation, currentBearing))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    fun setLocationOffset(newLocation: LatLng, currentRealLocation: LatLng) {
        locationOffset = LatLng(
            newLocation.latitude - currentRealLocation.latitude,
            newLocation.longitude - currentRealLocation.longitude
        )
        isTeleported = true
    }

    fun resetOffset() {
        locationOffset = LatLng(0.0, 0.0)
        isTeleported = false
    }

    fun getCurrentBearing(): Float = currentBearing

    companion object {
        fun hasLocationPermissions(context: Context): Boolean {
            return REQUIRED_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

@Composable
fun RequestLocationPermissions(
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(LocationManager.REQUIRED_PERMISSIONS)
    }
}