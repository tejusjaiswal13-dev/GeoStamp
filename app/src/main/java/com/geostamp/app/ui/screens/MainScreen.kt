package com.geostamp.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geostamp.app.model.LocationUiState
import com.geostamp.app.permission.PermissionHandler
import com.geostamp.app.ui.components.LocationPanel
import com.geostamp.app.ui.components.OsmMapView
import com.geostamp.app.ui.theme.AccuracyGood
import com.geostamp.app.util.ClipboardUtil
import com.geostamp.app.util.LocationFormatter
import com.geostamp.app.util.ShareUtil
import com.geostamp.app.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineGranted || coarseGranted

        val isPermanentlyDenied = if (!isGranted && context is Activity) {
            !ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) && !ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            false
        }

        viewModel.onPermissionResult(isGranted = isGranted, isPermanentlyDenied = isPermanentlyDenied)
    }

    // Check location permission and state on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!PermissionHandler.hasLocationPermission(context)) {
                    permissionLauncher.launch(PermissionHandler.LOCATION_PERMISSIONS)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GeoStamp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Real-time GPS status indicator dot
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            // MAP SECTION (Middle / Background)
            val currentLoc = (uiState as? LocationUiState.Available)?.data
            if (currentLoc != null) {
                OsmMapView(
                    latitude = currentLoc.latitude,
                    longitude = currentLoc.longitude,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder when map/location is not yet available
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
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "GPS Map View",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Quick Re-center FAB when location is available
            if (currentLoc != null) {
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

            // BOTTOM LOCATION PANEL (Prominent card with rounded top corners)
            LocationPanel(
                uiState = uiState,
                onRefresh = { viewModel.refreshLocation() },
                onCopy = {
                    currentLoc?.let { loc ->
                        ClipboardUtil.copyToClipboard(
                            context = context,
                            label = "GPS Coordinates",
                            text = LocationFormatter.formatCoordinates(loc.latitude, loc.longitude)
                        )
                    }
                },
                onShare = {
                    currentLoc?.let { loc ->
                        val addressText = (uiState as? LocationUiState.Available)?.address?.let { addr ->
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
                    currentLoc?.let { loc ->
                        ShareUtil.openInMaps(
                            context = context,
                            lat = loc.latitude,
                            lng = loc.longitude
                        )
                    }
                },
                onRequestPermission = {
                    permissionLauncher.launch(PermissionHandler.LOCATION_PERMISSIONS)
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
