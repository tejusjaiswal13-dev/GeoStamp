package com.geostamp.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Utility object for formatting location-related data into clean, human-readable strings.
 */
object LocationFormatter {

    /**
     * Formats a latitude value to 6 decimal places.
     */
    fun formatLatitude(lat: Double): String =
        String.format(Locale.US, "%.6f", lat)

    /**
     * Formats a longitude value to 6 decimal places.
     */
    fun formatLongitude(lng: Double): String =
        String.format(Locale.US, "%.6f", lng)

    /**
     * Formats latitude and longitude with cardinal directions (e.g. "25.4358° N, 81.8463° E").
     */
    fun formatCardinalCoordinates(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.4f° %s, %.4f° %s", abs(lat), latDir, abs(lng), lngDir)
    }

    /**
     * Formats location accuracy in meters.
     */
    fun formatAccuracy(accuracy: Float?): String =
        accuracy?.let { "±${it.roundToInt()} m" } ?: "N/A"

    /**
     * Formats altitude in meters.
     */
    fun formatAltitude(altitude: Double?): String =
        altitude?.let { String.format(Locale.US, "%.1f m", it) } ?: "N/A"

    /**
     * Formats speed, converting from m/s to km/h.
     */
    fun formatSpeed(speed: Float?): String =
        speed?.let {
            val kmh = it * 3.6f
            String.format(Locale.US, "%.1f km/h", kmh)
        } ?: "N/A"

    /**
     * Formats bearing in degrees with a compass direction (e.g., "45° NE").
     */
    fun formatBearing(bearing: Float?): String =
        bearing?.let {
            val normalized = ((it % 360) + 360) % 360
            val direction = when {
                normalized < 22.5f -> "N"
                normalized < 67.5f -> "NE"
                normalized < 112.5f -> "E"
                normalized < 157.5f -> "SE"
                normalized < 202.5f -> "S"
                normalized < 247.5f -> "SW"
                normalized < 292.5f -> "W"
                normalized < 337.5f -> "NW"
                else -> "N"
            }
            "${normalized.roundToInt()}° $direction"
        } ?: "N/A"

    /**
     * Formats a timestamp as a short time string (e.g., "03:15 PM").
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats a timestamp as a full date-time string (e.g., "Sep 19, 2026 03:15:30 PM").
     */
    fun formatFullTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats latitude and longitude into a combined coordinate string.
     */
    fun formatCoordinates(lat: Double, lng: Double): String =
        "${formatLatitude(lat)}, ${formatLongitude(lng)}"

    /**
     * Generates a Google Maps URL for the given coordinates.
     */
    fun getGoogleMapsUrl(lat: Double, lng: Double): String =
        "https://maps.google.com/?q=${formatLatitude(lat)},${formatLongitude(lng)}"

    /**
     * Generates a geo: URI for the given coordinates.
     */
    fun getGeoUri(lat: Double, lng: Double): String {
        val latStr = formatLatitude(lat)
        val lngStr = formatLongitude(lng)
        return "geo:$latStr,$lngStr?q=$latStr,$lngStr(My+Location)"
    }
}
