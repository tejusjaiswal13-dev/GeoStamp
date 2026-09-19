package com.geostamp.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Utility object for formatting location-related data into human-readable strings.
 */
object LocationFormatter {

    /**
     * Formats a latitude value to 6 decimal places.
     *
     * @param lat The latitude in degrees.
     * @return A string representation of the latitude (e.g., "25.435800").
     */
    fun formatLatitude(lat: Double): String =
        String.format(Locale.US, "%.6f", lat)

    /**
     * Formats a longitude value to 6 decimal places.
     *
     * @param lng The longitude in degrees.
     * @return A string representation of the longitude (e.g., "81.846300").
     */
    fun formatLongitude(lng: Double): String =
        String.format(Locale.US, "%.6f", lng)

    /**
     * Formats location accuracy in meters.
     *
     * @param accuracy The accuracy in meters, or null if unavailable.
     * @return A formatted string (e.g., "±5 m") or "N/A" if null.
     */
    fun formatAccuracy(accuracy: Float?): String =
        accuracy?.let { "±${it.roundToInt()} m" } ?: "N/A"

    /**
     * Formats altitude in meters.
     *
     * @param altitude The altitude in meters, or null if unavailable.
     * @return A formatted string (e.g., "125.3 m") or "N/A" if null.
     */
    fun formatAltitude(altitude: Double?): String =
        altitude?.let { String.format(Locale.US, "%.1f m", it) } ?: "N/A"

    /**
     * Formats speed, converting from m/s to km/h.
     *
     * @param speed The speed in meters per second, or null if unavailable.
     * @return A formatted string (e.g., "12.5 km/h") or "N/A" if null.
     */
    fun formatSpeed(speed: Float?): String =
        speed?.let {
            val kmh = it * 3.6f
            String.format(Locale.US, "%.1f km/h", kmh)
        } ?: "N/A"

    /**
     * Formats bearing in degrees with a compass direction.
     *
     * @param bearing The bearing in degrees (0–360), or null if unavailable.
     * @return A formatted string (e.g., "45° NE") or "N/A" if null.
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
     * Formats a timestamp as a short time string.
     *
     * @param timestamp The Unix timestamp in milliseconds.
     * @return A formatted time string (e.g., "03:15 PM").
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats a timestamp as a full date-time string.
     *
     * @param timestamp The Unix timestamp in milliseconds.
     * @return A formatted date-time string (e.g., "Sep 19, 2026 03:15:30 PM").
     */
    fun formatFullTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats latitude and longitude into a combined coordinate string.
     *
     * @param lat The latitude in degrees.
     * @param lng The longitude in degrees.
     * @return A formatted string (e.g., "25.435800, 81.846300").
     */
    fun formatCoordinates(lat: Double, lng: Double): String =
        "${formatLatitude(lat)}, ${formatLongitude(lng)}"

    /**
     * Generates a Google Maps URL for the given coordinates.
     *
     * @param lat The latitude in degrees.
     * @param lng The longitude in degrees.
     * @return A Google Maps URL string.
     */
    fun getGoogleMapsUrl(lat: Double, lng: Double): String =
        "https://maps.google.com/?q=${formatLatitude(lat)},${formatLongitude(lng)}"

    /**
     * Generates a geo: URI for the given coordinates.
     *
     * @param lat The latitude in degrees.
     * @param lng The longitude in degrees.
     * @return A geo URI string with a query marker.
     */
    fun getGeoUri(lat: Double, lng: Double): String {
        val latStr = formatLatitude(lat)
        val lngStr = formatLongitude(lng)
        return "geo:$latStr,$lngStr?q=$latStr,$lngStr(My+Location)"
    }
}
