package com.geostamp.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import android.graphics.Color as AndroidColor

/**
 * Modern, responsive OpenStreetMap view wrapped for Jetpack Compose.
 * Supports smooth free-hand panning, pinching, zoom, and controlled recentering
 * without fighting the user's touch gestures when new GPS updates arrive.
 */
@Composable
fun OsmMapView(
    latitude: Double?,
    longitude: Double?,
    accuracy: Float? = null,
    zoomLevel: Double = 17.0,
    recenterTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER) // Clean, no ugly +/- overlay
            controller.setZoom(zoomLevel)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
        }
    }

    val marker = remember {
        Marker(mapView).apply {
            title = "Current Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }

    val accuracyCircle = remember {
        Polygon(mapView).apply {
            fillPaint.color = AndroidColor.argb(45, 56, 189, 248) // Translucent light cyan
            outlinePaint.color = AndroidColor.argb(160, 56, 189, 248)
            outlinePaint.strokeWidth = 2.5f
        }
    }

    var hasInitialCentered by remember { mutableStateOf(false) }

    // Update marker and accuracy ring on location changes without pulling the map view
    LaunchedEffect(latitude, longitude, accuracy) {
        if (latitude != null && longitude != null) {
            val geoPoint = GeoPoint(latitude, longitude)

            if (!mapView.overlays.contains(accuracyCircle)) {
                mapView.overlays.add(accuracyCircle)
            }
            if (!mapView.overlays.contains(marker)) {
                mapView.overlays.add(marker)
            }

            marker.position = geoPoint
            marker.snippet = String.format("%.6f, %.6f", latitude, longitude)

            // Draw accuracy circle
            val radiusMeters = (accuracy ?: 15f).toDouble().coerceAtLeast(8.0)
            val circlePoints = Polygon.pointsAsCircle(geoPoint, radiusMeters)
            accuracyCircle.points = circlePoints

            // Center only on the very first coordinate fix
            if (!hasInitialCentered) {
                mapView.controller.setCenter(geoPoint)
                mapView.controller.setZoom(zoomLevel)
                hasInitialCentered = true
            }

            mapView.invalidate()
        }
    }

    // Explicit recenter trigger (e.g. when user clicks "Locate Me")
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && latitude != null && longitude != null) {
            val geoPoint = GeoPoint(latitude, longitude)
            mapView.controller.animateTo(geoPoint, zoomLevel, 800L)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}
