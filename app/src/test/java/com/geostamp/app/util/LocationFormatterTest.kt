package com.geostamp.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFormatterTest {

    @Test
    fun formatLatitude_formatsToSixDecimals() {
        val lat = 25.4358
        val result = LocationFormatter.formatLatitude(lat)
        assertEquals("25.435800", result)
    }

    @Test
    fun formatLongitude_formatsToSixDecimals() {
        val lng = 81.8463
        val result = LocationFormatter.formatLongitude(lng)
        assertEquals("81.846300", result)
    }

    @Test
    fun formatAccuracy_withNonNull_formatsWithUnit() {
        assertEquals("±5 m", LocationFormatter.formatAccuracy(5.2f))
    }

    @Test
    fun formatAccuracy_withNull_returnsNA() {
        assertEquals("N/A", LocationFormatter.formatAccuracy(null))
    }

    @Test
    fun formatAltitude_withNonNull_formatsWithUnit() {
        assertEquals("125.3 m", LocationFormatter.formatAltitude(125.34))
    }

    @Test
    fun formatSpeed_convertsMsToKmh() {
        // 10 m/s = 36 km/h
        assertEquals("36.0 km/h", LocationFormatter.formatSpeed(10f))
    }

    @Test
    fun formatBearing_withNorth_returnsCorrectDirection() {
        val result = LocationFormatter.formatBearing(10f)
        assertTrue(result.contains("N"))
    }

    @Test
    fun formatCoordinates_combinesLatAndLng() {
        val result = LocationFormatter.formatCoordinates(25.4358, 81.8463)
        assertEquals("25.435800, 81.846300", result)
    }

    @Test
    fun getGoogleMapsUrl_createsValidUrl() {
        val url = LocationFormatter.getGoogleMapsUrl(25.4358, 81.8463)
        assertTrue(url.startsWith("https://maps.google.com/?q=25.435800,81.846300"))
    }

    @Test
    fun formatCardinalCoordinates_formatsCorrectly() {
        val northEast = LocationFormatter.formatCardinalCoordinates(25.4358, 81.8463)
        assertEquals("25.435800° N, 81.846300° E", northEast)

        val southWest = LocationFormatter.formatCardinalCoordinates(-33.8688, -151.2093)
        assertEquals("33.868800° S, 151.209300° W", southWest)
    }
}
