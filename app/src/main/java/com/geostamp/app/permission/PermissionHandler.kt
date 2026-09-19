package com.geostamp.app.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Centralized handler for location-related permissions and settings navigation.
 *
 * Provides utility functions to check whether location permissions have been granted,
 * whether location services are enabled on the device, and to navigate the user to
 * the relevant system settings screens.
 */
object PermissionHandler {

    /** The set of location permissions required by the app. */
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Checks whether the app has been granted either fine or coarse location permission.
     *
     * @param context The [Context] used to check permission status.
     * @return `true` if at least one of [Manifest.permission.ACCESS_FINE_LOCATION] or
     *         [Manifest.permission.ACCESS_COARSE_LOCATION] is granted, `false` otherwise.
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks whether location services (GPS or network provider) are enabled on the device.
     *
     * @param context The [Context] used to access [LocationManager].
     * @return `true` if either [LocationManager.GPS_PROVIDER] or
     *         [LocationManager.NETWORK_PROVIDER] is enabled, `false` otherwise.
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Opens the system Location Settings screen so the user can enable location services.
     *
     * @param context The [Context] used to launch the settings activity.
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Opens the application detail settings screen for this app, where the user can
     * manage permissions, storage, notifications, and other per-app settings.
     *
     * @param context The [Context] used to launch the settings activity.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
