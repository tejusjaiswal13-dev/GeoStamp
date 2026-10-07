package com.geostamp.app.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geostamp.app.model.LocationUiState
import com.geostamp.app.permission.PermissionHandler
import com.geostamp.app.ui.components.AboutDialog
import com.geostamp.app.ui.components.ContactUsDialog
import com.geostamp.app.ui.components.PermissionStateCard
import com.geostamp.app.viewmodel.LocationViewModel

enum class AppMode {
    CAMERA,
    MAP
}

@Composable
fun MainScreen(
    viewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showContactUsDialog by remember { mutableStateOf(false) }

    var activeMode by remember { mutableStateOf(AppMode.CAMERA) }
    var hasCameraPermission by remember { mutableStateOf(PermissionHandler.hasCameraPermission(context)) }
    var hasLocationPermission by remember { mutableStateOf(PermissionHandler.hasLocationPermission(context)) }
    var isLocationEnabled by remember { mutableStateOf(PermissionHandler.isLocationEnabled(context)) }

    // Intercept back button when in Map mode to return directly to Camera
    BackHandler(enabled = activeMode == AppMode.MAP) {
        activeMode = AppMode.CAMERA
    }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    // Multi-permission launcher (Camera + Fine/Coarse Location)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true || PermissionHandler.hasCameraPermission(context)
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val locGranted = fineGranted || coarseGranted || PermissionHandler.hasLocationPermission(context)

        hasCameraPermission = cameraGranted
        hasLocationPermission = locGranted
        isLocationEnabled = PermissionHandler.isLocationEnabled(context)

        val allGranted = cameraGranted && locGranted

        val isPermanentlyDenied = if (!allGranted && context is Activity) {
            (!cameraGranted && !ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.CAMERA)) ||
            (!locGranted && !ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            false
        }

        viewModel.onPermissionResult(isGranted = locGranted, isPermanentlyDenied = isPermanentlyDenied)
    }

    // Refresh permissions and location status on resume / foregrounding
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = PermissionHandler.hasCameraPermission(context)
                hasLocationPermission = PermissionHandler.hasLocationPermission(context)
                isLocationEnabled = PermissionHandler.isLocationEnabled(context)

                if (!hasCameraPermission || !hasLocationPermission) {
                    permissionLauncher.launch(PermissionHandler.APP_PERMISSIONS)
                } else {
                    viewModel.checkAndStartLocation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Check if permissions are missing
    if (!hasCameraPermission || !hasLocationPermission) {
        val isPermanentlyDenied = if (context is Activity) {
            (!hasCameraPermission && !ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.CAMERA)) ||
            (!hasLocationPermission && !ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            false
        }

        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                PermissionStateCard(
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRequestPermission = {
                        permissionLauncher.launch(PermissionHandler.APP_PERMISSIONS)
                    },
                    onOpenSettings = {
                        PermissionHandler.openAppSettings(context)
                    }
                )
            }
        }
        return
    }

    val availableState = uiState as? LocationUiState.Available
    val currentLocation = availableState?.data
    val currentAddress = availableState?.address

    // Main App Navigation: Camera Mode vs Redesigned Map Mode
    when (activeMode) {
        AppMode.CAMERA -> {
            CameraScreen(
                locationData = currentLocation,
                addressData = currentAddress,
                isLocationEnabled = isLocationEnabled && uiState !is LocationUiState.ServiceDisabled,
                hasLocationPermission = hasLocationPermission,
                onSwitchToMap = { activeMode = AppMode.MAP },
                onRequestPermission = { permissionLauncher.launch(PermissionHandler.APP_PERMISSIONS) },
                onEnableLocation = { PermissionHandler.openLocationSettings(context) },
                onOpenAbout = { showAboutDialog = true }
            )
        }

        AppMode.MAP -> {
            MapScreen(
                locationData = currentLocation,
                addressData = currentAddress,
                uiState = uiState,
                onBackToCamera = { activeMode = AppMode.CAMERA },
                onRefreshLocation = { viewModel.refreshLocation() },
                onOpenAbout = { showAboutDialog = true }
            )
        }
    }

    // Secondary Dialogs
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onOpenContactUs = {
                showAboutDialog = false
                showContactUsDialog = true
            }
        )
    }

    if (showContactUsDialog) {
        ContactUsDialog(
            onDismiss = { showContactUsDialog = false }
        )
    }
}
