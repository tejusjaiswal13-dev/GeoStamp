package com.geostamp.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geostamp.app.location.GeocodingRepository
import com.geostamp.app.location.LocationRepository
import com.geostamp.app.model.AddressData
import com.geostamp.app.model.LocationData
import com.geostamp.app.model.LocationUiState
import com.geostamp.app.permission.PermissionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel managing the location state and actions for the GeoStamp application.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)
    private val geocodingRepository = GeocodingRepository(application)

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Loading)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var locationUpdatesJob: Job? = null
    private var lastLocationData: LocationData? = null
    private var lastAddressData: AddressData? = null

    /**
     * Called when the screen appears or when permission status changes.
     */
    fun checkAndStartLocation() {
        val context = getApplication<Application>()

        if (!PermissionHandler.hasLocationPermission(context)) {
            _uiState.value = LocationUiState.PermissionDenied(isPermanentlyDenied = false)
            return
        }

        if (!PermissionHandler.isLocationEnabled(context)) {
            _uiState.value = LocationUiState.ServiceDisabled
            return
        }

        startLocationTracking()
    }

    /**
     * Handles the result of the runtime permission request dialog.
     */
    fun onPermissionResult(isGranted: Boolean, isPermanentlyDenied: Boolean = false) {
        if (isGranted) {
            checkAndStartLocation()
        } else {
            _uiState.value = LocationUiState.PermissionDenied(isPermanentlyDenied = isPermanentlyDenied)
        }
    }

    /**
     * Starts continuous location updates and immediately tries to fetch the last known location.
     */
    private fun startLocationTracking() {
        _uiState.value = LocationUiState.Loading

        // Attempt immediate last known location for quick response
        viewModelScope.launch {
            val lastLoc = locationRepository.getLastLocation()
            if (lastLoc != null && _uiState.value is LocationUiState.Loading) {
                lastLocationData = lastLoc
                val address = geocodingRepository.getAddress(lastLoc.latitude, lastLoc.longitude)
                lastAddressData = address
                _uiState.value = LocationUiState.Available(lastLoc, address)
            }
        }

        // Start continuous updates
        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            locationRepository.locationUpdates()
                .catch { e ->
                    _uiState.value = LocationUiState.Error(
                        e.localizedMessage ?: "Failed to get location updates"
                    )
                }
                .collect { locationData ->
                    lastLocationData = locationData
                    val address = geocodingRepository.getAddress(
                        locationData.latitude,
                        locationData.longitude
                    ) ?: lastAddressData
                    lastAddressData = address
                    _uiState.value = LocationUiState.Available(locationData, address)
                }
        }
    }

    /**
     * Manually triggers a high-accuracy single location refresh.
     */
    fun refreshLocation() {
        val context = getApplication<Application>()
        if (!PermissionHandler.hasLocationPermission(context)) {
            _uiState.value = LocationUiState.PermissionDenied(isPermanentlyDenied = false)
            return
        }
        if (!PermissionHandler.isLocationEnabled(context)) {
            _uiState.value = LocationUiState.ServiceDisabled
            return
        }

        // If we don't have a location yet, show loading
        if (_uiState.value !is LocationUiState.Available) {
            _uiState.value = LocationUiState.Loading
        }

        locationRepository.requestSingleUpdate { freshLocation ->
            if (freshLocation != null) {
                viewModelScope.launch {
                    lastLocationData = freshLocation
                    val address = geocodingRepository.getAddress(
                        freshLocation.latitude,
                        freshLocation.longitude
                    ) ?: lastAddressData
                    lastAddressData = address
                    _uiState.value = LocationUiState.Available(freshLocation, address)
                }
            } else if (_uiState.value !is LocationUiState.Available) {
                _uiState.value = LocationUiState.Unavailable("Unable to determine current location. Try moving outdoors.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationUpdatesJob?.cancel()
    }
}
