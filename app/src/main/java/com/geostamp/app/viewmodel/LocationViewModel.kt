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
 * ViewModel managing the location lifecycle, permissions, real-time GPS fixes,
 * and automatic recovery when system location providers are toggled.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)
    private val geocodingRepository = GeocodingRepository(application)

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Loading)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var locationUpdatesJob: Job? = null
    private var lastLocationData: LocationData? = null
    private var lastAddressData: AddressData? = null

    init {
        // Automatically monitor location provider status changes in real-time
        viewModelScope.launch {
            locationRepository.locationServicesEnabledFlow().collect { isEnabled ->
                val context = getApplication<Application>()
                if (!isEnabled) {
                    locationUpdatesJob?.cancel()
                    locationUpdatesJob = null
                    _uiState.value = LocationUiState.ServiceDisabled
                } else {
                    // System location services are enabled.
                    // Automatically reacquire location if permissions are granted.
                    if (PermissionHandler.hasLocationPermission(context)) {
                        if (_uiState.value is LocationUiState.ServiceDisabled ||
                            locationUpdatesJob == null ||
                            locationUpdatesJob?.isActive == false
                        ) {
                            startLocationTracking()
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks permissions and location services status, starting tracking if available.
     * Called on screen entry and on Activity lifecycle ON_RESUME.
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

        // Only start if not already actively receiving updates
        if (locationUpdatesJob == null || locationUpdatesJob?.isActive == false) {
            startLocationTracking()
        }
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
     * Starts continuous high-accuracy location tracking and immediately loads
     * any recent cached fix to minimize initial wait time.
     */
    private fun startLocationTracking() {
        if (_uiState.value !is LocationUiState.Available) {
            _uiState.value = LocationUiState.Loading
        }

        // Quick bootstrap: Fetch last known location immediately
        viewModelScope.launch {
            val lastLoc = locationRepository.getLastLocation()
            if (lastLoc != null && _uiState.value is LocationUiState.Loading) {
                lastLocationData = lastLoc
                val address = geocodingRepository.getAddress(lastLoc.latitude, lastLoc.longitude)
                lastAddressData = address
                _uiState.value = LocationUiState.Available(
                    data = lastLoc,
                    address = address,
                    isAcquiringBetterFix = !lastLoc.isHighAccuracy
                )
            }
        }

        // Start continuous high-accuracy updates
        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            locationRepository.locationUpdates()
                .catch { e ->
                    _uiState.value = LocationUiState.Error(
                        e.localizedMessage ?: "Failed to get GPS location updates"
                    )
                }
                .collect { locationData ->
                    lastLocationData = locationData
                    val address = geocodingRepository.getAddress(
                        locationData.latitude,
                        locationData.longitude
                    ) ?: lastAddressData
                    lastAddressData = address

                    _uiState.value = LocationUiState.Available(
                        data = locationData,
                        address = address,
                        isAcquiringBetterFix = !locationData.isHighAccuracy
                    )
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
                    _uiState.value = LocationUiState.Available(
                        data = freshLocation,
                        address = address,
                        isAcquiringBetterFix = !freshLocation.isHighAccuracy
                    )
                }
            } else if (_uiState.value !is LocationUiState.Available) {
                _uiState.value = LocationUiState.Unavailable(
                    "Unable to determine current location. Please verify GPS signal."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationUpdatesJob?.cancel()
    }
}
