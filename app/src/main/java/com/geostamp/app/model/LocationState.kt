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
 * @property provider The name of the provider that generated this fix (e.g., "gps", "network"), or null.
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
)

/**
 * Represents a reverse-geocoded address associated with a location.
 *
 * @property addressLine The full formatted address line (e.g., "123 Main St, Springfield, IL 62704").
 * @property city The city or locality name.
 * @property state The state or administrative area name.
 * @property country The country name.
 * @property postalCode The postal or ZIP code.
 * @property featureName The locality or feature name (e.g., a landmark or neighborhood).
 */
data class AddressData(
    val addressLine: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val featureName: String? = null,
)

/**
 * Sealed interface representing all possible UI states for location acquisition.
 *
 * Use this with a `when` expression to exhaustively handle every state in the UI layer.
 */
sealed interface LocationUiState {

    /** Location data is being acquired. */
    data object Loading : LocationUiState

    /**
     * Location data is available.
     *
     * @property data The current [LocationData].
     * @property address The reverse-geocoded [AddressData], or null if geocoding is unavailable.
     */
    data class Available(
        val data: LocationData,
        val address: AddressData? = null,
    ) : LocationUiState

    /**
     * The required location permission has been denied by the user.
     *
     * @property isPermanentlyDenied `true` if the user selected "Don't ask again",
     *   meaning the permission can only be granted from system settings.
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
