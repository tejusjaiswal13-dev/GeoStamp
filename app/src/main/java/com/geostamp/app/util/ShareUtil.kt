package com.geostamp.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utility object for sharing location and launching map applications.
 */
object ShareUtil {

    /**
     * Shares the current location text and Google Maps link via Android's native share sheet.
     *
     * @param context Application or activity context.
     * @param lat Latitude in degrees.
     * @param lng Longitude in degrees.
     * @param address Formatted address string if available.
     */
    fun shareLocation(context: Context, lat: Double, lng: Double, address: String?) {
        val mapsUrl = LocationFormatter.getGoogleMapsUrl(lat, lng)
        val textToShare = buildString {
            append("My Current Location:\n")
            if (!address.isNullOrBlank()) {
                append("Address: $address\n")
            }
            append("Coordinates: ${LocationFormatter.formatCoordinates(lat, lng)}\n")
            append("Google Maps: $mapsUrl")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, textToShare)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Location via")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share location", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the coordinates in Google Maps or any installed map application.
     *
     * @param context Application or activity context.
     * @param lat Latitude in degrees.
     * @param lng Longitude in degrees.
     */
    fun openInMaps(context: Context, lat: Double, lng: Double) {
        val geoUri = Uri.parse(LocationFormatter.getGeoUri(lat, lng))
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to browser Google Maps
            val webUri = Uri.parse(LocationFormatter.getGoogleMapsUrl(lat, lng))
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No app available to open map", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
