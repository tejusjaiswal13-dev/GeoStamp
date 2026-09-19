package com.geostamp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geostamp.app.model.AddressData
import com.geostamp.app.model.LocationData
import com.geostamp.app.ui.theme.AccuracyGood
import com.geostamp.app.ui.theme.AccuracyMedium
import com.geostamp.app.ui.theme.AccuracyPoor
import com.geostamp.app.util.LocationFormatter

/**
 * Live GPS stamp card overlay displayed directly on top of the camera viewfinder.
 * Matches the aesthetic of the reference GPS Camera apps.
 */
@Composable
fun CameraLocationStampOverlay(
    location: LocationData?,
    address: AddressData?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xBB121620) // Translucent deep camera overlay
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header: GEO STAMP Branding + Live Accuracy Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GEO STAMP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF64B5F6)
                    )
                }

                if (location?.accuracy != null) {
                    val (badgeColor, text) = when {
                        location.accuracy <= 10f -> AccuracyGood to "±${location.accuracy.toInt()}m"
                        location.accuracy <= 30f -> AccuracyMedium to "±${location.accuracy.toInt()}m"
                        else -> AccuracyPoor to "±${location.accuracy.toInt()}m"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "Acquiring GPS...",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Address Line
            val displayAddress = address?.addressLine
                ?: listOfNotNull(address?.city, address?.state, address?.country).joinToString(", ").ifBlank {
                    if (location != null) "Real-time GPS Fix Acquired" else "Searching for satellites..."
                }

            Text(
                text = displayAddress,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Coordinates
            if (location != null) {
                Text(
                    text = "Lat: ${LocationFormatter.formatLatitude(location.latitude)}°   Long: ${LocationFormatter.formatLongitude(location.longitude)}°",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Telemetry row (Alt, Speed, Bearing, Timestamp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                location?.altitude?.let {
                    TelemetryChip(icon = Icons.Default.Height, text = LocationFormatter.formatAltitude(it))
                }
                location?.bearing?.let {
                    TelemetryChip(icon = Icons.Default.Explore, text = LocationFormatter.formatBearing(it))
                }
                location?.speed?.let {
                    if (it > 0.5f) {
                        TelemetryChip(icon = Icons.Default.Speed, text = LocationFormatter.formatSpeed(it))
                    }
                }
                val timeStr = LocationFormatter.formatTimestamp(location?.timestamp ?: System.currentTimeMillis())
                TelemetryChip(icon = Icons.Default.AccessTime, text = timeStr)
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
