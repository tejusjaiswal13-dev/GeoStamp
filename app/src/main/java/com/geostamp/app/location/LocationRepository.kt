package com.geostamp.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.geostamp.app.model.LocationData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository that provides location data using Google Play Services
 * [FusedLocationProviderClient].
 *
 * Callers are responsible for ensuring location permissions have been granted
 * before invoking any of the public APIs exposed by this class.
 *
 * @param context Application or activity context used to obtain the fused
 *   location provider client.
 */
class LocationRepository(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Returns a [Flow] that emits [LocationData] updates at a balanced-power
     * interval.
     *
     * The flow uses [callbackFlow] internally and automatically removes the
     * location callback when the collector is cancelled.
     *
     * @return A cold [Flow] of [LocationData] representing the device's
     *   current location.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationData> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10_000L
        )
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location.toLocationData())
                }
            }
        }

        fusedClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    /**
     * Returns the last known location, or `null` if no location is available.
     *
     * @return The most recent [LocationData], or `null`.
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
     * Requests a single, high-accuracy location fix and delivers the result
     * via [callback].
     *
     * @param callback Invoked with the resulting [LocationData], or `null` if
     *   the location could not be determined.
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
        altitude = altitude,
        accuracy = accuracy,
        bearing = bearing,
        speed = speed,
        timestamp = time
    )
}
