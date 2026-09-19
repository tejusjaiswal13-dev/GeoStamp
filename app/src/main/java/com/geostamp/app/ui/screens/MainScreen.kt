package com.geostamp.app.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geostamp.app.model.LocationUiState
import com.geostamp.app.permission.PermissionHandler
import com.geostamp.app.ui.components.AboutDialog
import com.geostamp.app.ui.components.LocationPanel
import com.geostamp.app.ui.components.OsmMapView
import com.geostamp.app.ui.components.PermissionStateCard
import com.geostamp.app.ui.components.ServiceDisabledCard
import com.geostamp.app.ui.theme.AccuracyGood
import com.geostamp.app.util.ClipboardUtil
import com.geostamp.app.util.LocationFormatter
import com.geostamp.app.util.ShareUtil
import com.geostamp.app.viewmodel.LocationViewModel

enum class AppMode {
    CAMERA,
    MAP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var activeMode by remember { mutableStateOf(AppMode.CAMERA) }
    var hasCameraPermission by remember { mutableStateOf(PermissionHandler.hasCameraPermission(context)) }
    var hasLocationPermission by remember { mutableStateOf(PermissionHandler.hasLocationPermission(context)) }

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

        val allGranted = cameraGranted && locGranted

        val isPermanentlyDenied = if (!allGranted && context is Activity) {
            (!cameraGranted && !ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.CAMERA)) ||
            (!locGranted && !ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            false
        }

        viewModel.onPermissionResult(isGranted = locGranted, isPermanentlyDenied = isPermanentlyDenied)
    }

    // Refresh permissions and location on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = PermissionHandler.hasCameraPermission(context)
                hasLocationPermission = PermissionHandler.hasLocationPermission(context)

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

    // Check if permission is missing
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

    // Switch between Camera and Map modes
    when (activeMode) {
        AppMode.CAMERA -> {
            CameraScreen(
                locationData = currentLocation,
                addressData = currentAddress,
                onSwitchToMap = { activeMode = AppMode.MAP },
                onOpenAbout = { showAboutDialog = true }
            )
        }

        AppMode.MAP -> {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GeoStamp Map",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState is LocationUiState.Available) AccuracyGood
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        },
                        actions = {
                            // Button to open About / Credits
                            IconButton(onClick = { showAboutDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Button to switch back to Camera
                            Surface(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { activeMode = AppMode.CAMERA },
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Camera",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Map View
                    if (currentLocation != null) {
                        OsmMapView(
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "Acquiring GPS location...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Re-center FAB
                    if (currentLocation != null) {
                        FloatingActionButton(
                            onClick = { viewModel.refreshLocation() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Re-center Location"
                            )
                        }
                    }

                    // Bottom Location Panel
                    LocationPanel(
                        uiState = uiState,
                        onRefresh = { viewModel.refreshLocation() },
                        onCopy = {
                            currentLocation?.let { loc ->
                                ClipboardUtil.copyToClipboard(
                                    context = context,
                                    label = "GPS Coordinates",
                                    text = LocationFormatter.formatCoordinates(loc.latitude, loc.longitude)
                                )
                            }
                        },
                        onShare = {
                            currentLocation?.let { loc ->
                                val addressText = currentAddress?.let { addr ->
                                    addr.addressLine ?: listOfNotNull(addr.city, addr.state, addr.country).joinToString(", ")
                                }
                                ShareUtil.shareLocation(
                                    context = context,
                                    lat = loc.latitude,
                                    lng = loc.longitude,
                                    address = addressText
                                )
                            }
                        },
                        onOpenInMaps = {
                            currentLocation?.let { loc ->
                                ShareUtil.openInMaps(
                                    context = context,
                                    lat = loc.latitude,
                                    lng = loc.longitude
                                )
                            }
                        },
                        onRequestPermission = {
                            permissionLauncher.launch(PermissionHandler.APP_PERMISSIONS)
                        },
                        onOpenSettings = {
                            PermissionHandler.openAppSettings(context)
                        },
                        onEnableServices = {
                            PermissionHandler.openLocationSettings(context)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
