package com.geostamp.app.location

import android.content.Context
import android.location.Geocoder
import com.geostamp.app.model.AddressData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

/**
 * Repository that performs reverse-geocoding to convert latitude/longitude
 * coordinates into human-readable address information.
 *
 * Results are cached so that repeated calls with coordinates that have not
 * changed significantly (within ±0.0001 degrees) return the previously
 * resolved [AddressData] without hitting the [Geocoder] again.
 *
 * @param context Application or activity context used to create the
 *   [Geocoder] instance.
 */
class GeocodingRepository(private val context: Context) {

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    private var cachedLatitude: Double? = null
    private var cachedLongitude: Double? = null
    private var cachedAddress: AddressData? = null

    /**
     * Resolves the address for the given [latitude] and [longitude].
     *
     * If the coordinates are within 0.0001 degrees of the previously queried
     * location the cached result is returned immediately.
     *
     * @param latitude  Latitude in decimal degrees.
     * @param longitude Longitude in decimal degrees.
     * @return The resolved [AddressData], or `null` if geocoding fails or no
     *   results are available.
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
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses.isNullOrEmpty()) return@withContext null

                val address = addresses[0]
                val addressData = AddressData(
                    addressLine = address.getAddressLine(0),
                    city = address.locality,
                    state = address.adminArea,
                    country = address.countryName,
                    postalCode = address.postalCode,
                    featureName = address.featureName
                )

                // Update cache.
                cachedLatitude = latitude
                cachedLongitude = longitude
                cachedAddress = addressData

                addressData
            } catch (e: Exception) {
                null
            }
        }
    }

    private companion object {
        /** Coordinates within this threshold (degrees) are considered unchanged. */
        const val CACHE_THRESHOLD = 0.0001
    }
}
