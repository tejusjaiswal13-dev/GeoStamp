package com.geostamp.app.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.geostamp.app.model.AddressData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.abs

/**
 * Repository that performs reverse-geocoding to convert latitude/longitude
 * coordinates into clean, concise human-readable address information.
 *
 * Results are cached so that repeated calls with coordinates that have not
 * changed significantly (within ±0.0001 degrees) return the previously
 * resolved [AddressData] without unnecessary Geocoder invocations.
 */
class GeocodingRepository(private val context: Context) {

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    private var cachedLatitude: Double? = null
    private var cachedLongitude: Double? = null
    private var cachedAddress: AddressData? = null

    /**
     * Resolves the address for the given [latitude] and [longitude].
     *
     * @param latitude  Latitude in decimal degrees.
     * @param longitude Longitude in decimal degrees.
     * @return The resolved [AddressData] with both full and concise formatting,
     *   or `null` if geocoding fails or is unavailable.
     */
    @Suppress("DEPRECATION")
    suspend fun getAddress(latitude: Double, longitude: Double): AddressData? {
        // Return cached result when coordinates haven't changed significantly.
        cachedLatitude?.let { cachedLat ->
            cachedLongitude?.let { cachedLng ->
                if (abs(cachedLat - latitude) < CACHE_THRESHOLD &&
                    abs(cachedLng - longitude) < CACHE_THRESHOLD
                ) {
                    return cachedAddress
                }
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                // Safeguard with timeout so slow network never hangs the app or camera
                withTimeoutOrNull(2500L) {
                    if (!Geocoder.isPresent()) return@withTimeoutOrNull null

                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (addresses.isNullOrEmpty()) return@withTimeoutOrNull null

                    val address = addresses[0]
                    val concise = buildConciseAddress(address)

                    val addressData = AddressData(
                        addressLine = address.getAddressLine(0),
                        city = address.locality,
                        state = address.adminArea,
                        country = address.countryName,
                        postalCode = address.postalCode,
                        featureName = address.featureName,
                        conciseAddress = concise
                    )

                    // Update cache
                    cachedLatitude = latitude
                    cachedLongitude = longitude
                    cachedAddress = addressData

                    addressData
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Creates a clean, concise, human-readable address string
     * (e.g. "Civil Lines, Prayagraj, Uttar Pradesh").
     */
    private fun buildConciseAddress(address: Address): String {
        val neighborhood = address.subLocality
            ?: (if (address.featureName != null && !address.featureName.matches(Regex("^[0-9+\\s-]+$"))) address.featureName else null)
            ?: address.thoroughfare

        val city = address.locality ?: address.subAdminArea
        val state = address.adminArea

        return when {
            neighborhood != null && city != null && state != null -> "$neighborhood, $city, $state"
            neighborhood != null && city != null -> "$neighborhood, $city"
            city != null && state != null -> "$city, $state"
            city != null -> city
            neighborhood != null -> neighborhood
            else -> {
                val raw = address.getAddressLine(0)
                if (!raw.isNullOrBlank()) {
                    raw.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.matches(Regex("^[0-9+\\s-]+$")) }
                        .take(3)
                        .joinToString(", ")
                } else {
                    listOfNotNull(address.adminArea, address.countryName).joinToString(", ")
                }
            }
        }
    }

    private companion object {
        /** Coordinates within this threshold (degrees) are considered unchanged (~11 meters). */
        const val CACHE_THRESHOLD = 0.0001
    }
}
