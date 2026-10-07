package com.geostamp.app.model

/**
 * Represents raw location data obtained from the device's location providers.
 *
 * @property latitude The latitude in degrees.
 * @property longitude The longitude in degrees.
 * @property accuracy The estimated horizontal accuracy of the location in meters, or null if unavailable.
 * @property altitude The altitude in meters above the WGS 84 reference ellipsoid, or null if unavailable.
 * @property speed The speed at the time of the location fix in meters/second, or null if unavailable.
 * @property bearing The bearing (direction of travel) in degrees, or null if unavailable.
 * @property timestamp The UTC time of the fix in milliseconds since epoch (System.currentTimeMillis()).
 * @property provider The name of the provider that generated this fix (e.g., "gps", "fused"), or null.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String? = null,
) {
    /** Whether this fix has high accuracy (25 meters or better). */
    val isHighAccuracy: Boolean get() = accuracy != null && accuracy <= 25f

    /** Whether this location fix was received recently (within the last 30 seconds). */
    val isFresh: Boolean get() = (System.currentTimeMillis() - timestamp) < 30_000L
}

/**
 * Represents a reverse-geocoded address associated with a location.
 *
 * @property addressLine The full formatted address line.
 * @property city The city or locality name.
 * @property state The state or administrative area name.
 * @property country The country name.
 * @property postalCode The postal or ZIP code.
 * @property featureName The locality or feature name.
 * @property conciseAddress A clean, concise formatted address suitable for mobile display (e.g. "Civil Lines, Prayagraj, Uttar Pradesh").
 */
data class AddressData(
    val addressLine: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val featureName: String? = null,
    val conciseAddress: String? = null,
)

/**
 * Sealed interface representing all possible UI states for location acquisition.
 */
sealed interface LocationUiState {

    /** Location data is being actively acquired. */
    data object Loading : LocationUiState

    /**
     * Location data is available.
     *
     * @property data The current [LocationData].
     * @property address The reverse-geocoded [AddressData], or null if geocoding is unavailable.
     * @property isAcquiringBetterFix True if a preliminary fix is displayed while seeking higher accuracy.
     */
    data class Available(
        val data: LocationData,
        val address: AddressData? = null,
        val isAcquiringBetterFix: Boolean = false,
    ) : LocationUiState

    /**
     * The required location permission has been denied by the user.
     *
     * @property isPermanentlyDenied `true` if permission can only be granted from system settings.
     */
    data class PermissionDenied(
        val isPermanentlyDenied: Boolean = false,
    ) : LocationUiState

    /** The device's location services (GPS / network) are disabled. */
    data object ServiceDisabled : LocationUiState

    /**
     * Location is temporarily unavailable.
     *
     * @property message A human-readable explanation of why location is unavailable.
     */
    data class Unavailable(
        val message: String,
    ) : LocationUiState

    /**
     * An unexpected error occurred while acquiring location.
     *
     * @property message A human-readable error description.
     */
    data class Error(
        val message: String,
    ) : LocationUiState
}
