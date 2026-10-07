package com.geostamp.app.location

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.geostamp.app.model.LocationData
import com.geostamp.app.permission.PermissionHandler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository that provides high-accuracy real-time location data using Google Play Services
 * [FusedLocationProviderClient] and monitors system location providers.
 */
class LocationRepository(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Flow that monitors whether the device's location services (GPS/network) are enabled in real time.
     * Uses Android's [LocationManager.PROVIDERS_CHANGED_ACTION] broadcast receiver.
     */
    fun locationServicesEnabledFlow(): Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                val isEnabled = PermissionHandler.isLocationEnabled(context)
                trySend(isEnabled)
            }
        }

        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            @Suppress("DEPRECATION")
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }

        context.registerReceiver(receiver, filter)

        // Emit current status immediately upon subscription
        trySend(PermissionHandler.isLocationEnabled(context))

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver was already unregistered
            }
        }
    }

    /**
     * Cold [Flow] that delivers real-time, high-accuracy [LocationData] updates.
     * Configured for maximum accuracy with fast updates suitable for geotagging.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationData> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3_000L
        )
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(0.5f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location.toLocationData())
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                super.onLocationAvailability(availability)
                // If location becomes unavailable at hardware level, callback flow remains active
                // and continues to listen as soon as GPS satellites reconnect
            }
        }

        fusedClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Returns the last known location, or `null` if no location is available.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): LocationData? {
        return try {
            fusedClient.lastLocation.await()?.toLocationData()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Requests a single, high-accuracy location fix directly from GPS hardware.
     */
    @SuppressLint("MissingPermission")
    fun requestSingleUpdate(callback: (LocationData?) -> Unit) {
        val cancellationTokenSource = CancellationTokenSource()

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            callback(location?.toLocationData())
        }.addOnFailureListener {
            callback(null)
        }
    }

    /**
     * Converts a platform [Location] into the app-level [LocationData] model.
     */
    private fun Location.toLocationData(): LocationData = LocationData(
        latitude = latitude,
        longitude = longitude,
        altitude = if (hasAltitude()) altitude else null,
        accuracy = if (hasAccuracy()) accuracy else null,
        bearing = if (hasBearing()) bearing else null,
        speed = if (hasSpeed()) speed else null,
        timestamp = time,
        provider = provider
    )
}
