package com.geostamp.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.geostamp.app.model.AddressData
import com.geostamp.app.model.LocationData
import com.geostamp.app.util.LocationFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Utility object for burning professional GPS location metadata and
 * an integrated location mini-map directly onto captured photographs.
 */
object PhotoStamper {

    /**
     * Stamped bitmap overlay with location coordinates, concise address,
     * timestamp, and an embedded mini-map snapshot of the capture location.
     */
    suspend fun stampPhoto(
        context: Context,
        sourceBitmap: Bitmap,
        location: LocationData?,
        address: AddressData?
    ): Bitmap {
        val mutableBitmap = if (sourceBitmap.isMutable) {
            sourceBitmap
        } else {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val canvas = Canvas(mutableBitmap)
        val width = mutableBitmap.width.toFloat()
        val height = mutableBitmap.height.toFloat()

        // Responsive scaling based on image width (1080p reference)
        val scale = max(width / 1080f, 1f)

        // Generate mini-map snapshot at the exact captured location
        val miniMapDim = (180f * scale).toInt()
        val miniMapBitmap: Bitmap? = if (location != null) {
            try {
                MiniMapGenerator.generateMiniMap(
                    context = context,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    targetWidth = miniMapDim,
                    targetHeight = miniMapDim,
                    cornerRadius = 16f * scale
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        // Layout Dimensions
        val cardMargin = 24f * scale
        val cardPadding = 18f * scale
        val cornerRadius = 22f * scale
        val spacing = 16f * scale

        // Paint configurations
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(195, 12, 17, 29) // Deep translucent slate
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248) // GPS Cyan (#38BDF8)
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }

        val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 203, 213, 225)
            textSize = 16f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Text preparations
        val brandText = "GEO STAMP  |  GPS CAMERA"

        val addressText = address?.conciseAddress
            ?: address?.addressLine
            ?: listOfNotNull(address?.featureName, address?.city, address?.state).joinToString(", ").ifBlank {
                if (location != null) "Real-time GPS Fix" else "Location fix unavailable"
            }

        val coordsText = if (location != null) {
            LocationFormatter.formatCardinalCoordinates(location.latitude, location.longitude)
        } else {
            "GPS Coordinates: Unavailable"
        }

        val telemetryParts = mutableListOf<String>()
        location?.accuracy?.let { telemetryParts.add("Acc: ${LocationFormatter.formatAccuracy(it)}") }
        location?.altitude?.let { telemetryParts.add("Alt: ${LocationFormatter.formatAltitude(it)}") }
        location?.speed?.let { if (it > 0.5f) telemetryParts.add(LocationFormatter.formatSpeed(it)) }
        location?.bearing?.let { telemetryParts.add(LocationFormatter.formatBearing(it)) }
        val telemetryText = if (telemetryParts.isNotEmpty()) telemetryParts.joinToString("  •  ") else "High Accuracy GPS"

        val timestamp = location?.timestamp ?: System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date(timestamp))
        val timeFormatted = SimpleDateFormat("hh:mm:ss a (z)", Locale.US).format(Date(timestamp))
        val timeText = "$dateFormatted  •  $timeFormatted"

        // Line Heights
        val brandLineH = 28f * scale
        val addressLineH = 32f * scale
        val coordLineH = 26f * scale
        val metaLineH = 22f * scale
        val timeLineH = 22f * scale

        val textBlockHeight = brandLineH + addressLineH + coordLineH + metaLineH + timeLineH
        val contentHeight = max(textBlockHeight, miniMapDim.toFloat()) + (cardPadding * 2)

        val cardLeft = cardMargin
        val cardRight = width - cardMargin
        val cardBottom = height - cardMargin
        val cardTop = cardBottom - contentHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // Draw translucent card background & crisp border
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, backgroundPaint)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // Draw Mini-Map if available
        var textLeft = cardLeft + cardPadding
        if (miniMapBitmap != null) {
            val mapLeft = cardLeft + cardPadding
            val mapTop = cardTop + (contentHeight - miniMapDim) / 2f
            canvas.drawBitmap(miniMapBitmap, mapLeft, mapTop, null)
            textLeft = mapLeft + miniMapDim + spacing
            miniMapBitmap.recycle()
        }

        val maxTextWidth = (cardRight - cardPadding) - textLeft

        // Draw Text Elements
        var currentY = cardTop + cardPadding + (brandLineH * 0.72f)

        // 1. Branding
        canvas.drawText(brandText, textLeft, currentY, brandPaint)
        currentY += addressLineH

        // 2. Concise Address
        val truncatedAddress = truncateTextToWidth(addressText, addressPaint, maxTextWidth)
        canvas.drawText(truncatedAddress, textLeft, currentY, addressPaint)
        currentY += coordLineH

        // 3. Coordinates
        canvas.drawText(coordsText, textLeft, currentY, coordPaint)
        currentY += metaLineH

        // 4. Telemetry
        val truncatedTelemetry = truncateTextToWidth(telemetryText, metaPaint, maxTextWidth)
        canvas.drawText(truncatedTelemetry, textLeft, currentY, metaPaint)
        currentY += timeLineH

        // 5. Date & Time
        canvas.drawText(timeText, textLeft, currentY, metaPaint)

        return mutableBitmap
    }

    private fun truncateTextToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "..." else text
    }
}
