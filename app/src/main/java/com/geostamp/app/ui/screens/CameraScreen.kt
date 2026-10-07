package com.geostamp.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geostamp.app.camera.CameraPreview
import com.geostamp.app.camera.MediaStorageUtil
import com.geostamp.app.camera.PhotoStamper
import com.geostamp.app.camera.captureAndProcessPhoto
import com.geostamp.app.model.AddressData
import com.geostamp.app.model.LocationData
import com.geostamp.app.ui.components.CameraLocationStampOverlay
import com.geostamp.app.ui.components.PhotoPreviewDialog
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    locationData: LocationData?,
    addressData: AddressData?,
    isLocationEnabled: Boolean,
    hasLocationPermission: Boolean,
    onSwitchToMap: () -> Unit,
    onRequestPermission: () -> Unit,
    onEnableLocation: () -> Unit,
    onOpenAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    var isCapturing by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastSavedUri by remember { mutableStateOf<Uri?>(null) }
    var lastSavedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. Full-screen Camera Viewfinder
        CameraPreview(
            cameraSelector = cameraSelector,
            flashMode = flashMode,
            imageCapture = imageCapture,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Controls Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash",
                    tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White else Color(0xFFFFD54F)
                )
            }

            // Mode Selector Pill (Camera / Map)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x77000000),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Camera",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSwitchToMap() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Map",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Right Controls: Info Button + Camera Flip Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenAbout,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // 3. Bottom Section: Compact Stamp Overlay + Shutter Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Clean GPS Location Stamp Card
            CameraLocationStampOverlay(
                location = locationData,
                address = addressData,
                isLocationEnabled = isLocationEnabled,
                hasLocationPermission = hasLocationPermission,
                onRequestPermission = onRequestPermission,
                onEnableLocation = onEnableLocation
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Shutter Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Gallery Thumbnail / Preview Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            if (lastSavedBitmap != null) {
                                capturedBitmap = lastSavedBitmap
                                showPreviewDialog = true
                            } else {
                                Toast.makeText(context, "No photos taken yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (lastSavedBitmap != null) {
                        Image(
                            bitmap = lastSavedBitmap!!.asImageBitmap(),
                            contentDescription = "Last Stamped Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Center: Large Camera Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            captureAndProcessPhoto(
                                imageCapture = imageCapture,
                                context = context,
                                onSuccess = { rawBitmap ->
                                    coroutineScope.launch {
                                        val stampedBitmap = PhotoStamper.stampPhoto(
                                            context = context,
                                            sourceBitmap = rawBitmap,
                                            location = locationData,
                                            address = addressData
                                        )

                                        val savedUri = MediaStorageUtil.savePhotoToGallery(context, stampedBitmap)

                                        lastSavedBitmap = stampedBitmap
                                        lastSavedUri = savedUri
                                        capturedBitmap = stampedBitmap
                                        showPreviewDialog = true
                                        isCapturing = false

                                        Toast.makeText(
                                            context,
                                            "GeoStamped photo saved to Pictures/GeoStamp!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onError = { errorMsg ->
                                    isCapturing = false
                                    Toast.makeText(context, "Capture failed: $errorMsg", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Right: Switch to Map View Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .clickable { onSwitchToMap() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Switch to Map",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 4. Stamped Photo Preview Dialog
        if (showPreviewDialog && capturedBitmap != null) {
            PhotoPreviewDialog(
                bitmap = capturedBitmap!!,
                savedUri = lastSavedUri,
                onDismiss = {
                    showPreviewDialog = false
                    capturedBitmap = null
                }
            )
        }
    }
}
