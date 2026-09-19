package com.geostamp.app.camera

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
 * Utility object for adding GPS location, address, and timestamp stamps directly
 * onto captured camera bitmaps.
 */
object PhotoStamper {

    /**
     * Stamped bitmap overlay with location coordinates, address, and timestamp.
     */
    fun stampPhoto(
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

        // Dimensions
        val cardMargin = 28f * scale
        val cardPadding = 24f * scale
        val cornerRadius = 24f * scale

        // Paint configurations
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 15, 20, 28) // Deep translucent dark
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 181, 246) // GPS Primary Cyan/Light Blue
            textSize = 22f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }

        val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 200, 210, 225)
            textSize = 17f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Prepare text content
        val titleText = "GEO STAMP  |  GPS CAMERA"

        val addressText = address?.addressLine
            ?: listOfNotNull(address?.featureName, address?.city, address?.state, address?.country).joinToString(", ").ifBlank {
                "Location fix acquired"
            }

        val coordsText = if (location != null) {
            "Lat: ${LocationFormatter.formatLatitude(location.latitude)}°  |  Long: ${LocationFormatter.formatLongitude(location.longitude)}°"
        } else {
            "GPS Coordinates: Unavailable"
        }

        val telemetryItems = mutableListOf<String>()
        location?.accuracy?.let { telemetryItems.add("Acc: ${LocationFormatter.formatAccuracy(it)}") }
        location?.altitude?.let { telemetryItems.add("Alt: ${LocationFormatter.formatAltitude(it)}") }
        location?.speed?.let { if (it > 0.5f) telemetryItems.add("Speed: ${LocationFormatter.formatSpeed(it)}") }
        location?.bearing?.let { telemetryItems.add("Heading: ${LocationFormatter.formatBearing(it)}") }
        val telemetryText = if (telemetryItems.isNotEmpty()) telemetryItems.joinToString("  •  ") else "GPS Signal: Standard"

        val timestamp = location?.timestamp ?: System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(Date(timestamp))
        val timeFormatted = SimpleDateFormat("hh:mm:ss a (z)", Locale.US).format(Date(timestamp))
        val timeText = "$dateFormatted  •  $timeFormatted"

        // Calculate card height dynamically
        val titleLineHeight = 32f * scale
        val addressLineHeight = 36f * scale
        val coordLineHeight = 30f * scale
        val telemetryLineHeight = 26f * scale
        val timeLineHeight = 26f * scale

        val contentHeight = titleLineHeight + addressLineHeight + coordLineHeight + telemetryLineHeight + timeLineHeight + (cardPadding * 2)

        val cardLeft = cardMargin
        val cardRight = width - cardMargin
        val cardBottom = height - cardMargin
        val cardTop = cardBottom - contentHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // Draw card background & subtle border
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, backgroundPaint)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // Draw Text Lines
        var currentY = cardTop + cardPadding + (titleLineHeight * 0.75f)
        val textLeft = cardLeft + cardPadding

        // 1. Title / Brand
        canvas.drawText(titleText, textLeft, currentY, titlePaint)
        currentY += addressLineHeight

        // 2. Address (ellipsize if too long)
        val maxTextWidth = (cardRight - cardLeft) - (cardPadding * 2)
        val truncatedAddress = truncateTextToWidth(addressText, addressPaint, maxTextWidth)
        canvas.drawText(truncatedAddress, textLeft, currentY, addressPaint)
        currentY += coordLineHeight

        // 3. Coordinates
        canvas.drawText(coordsText, textLeft, currentY, coordPaint)
        currentY += telemetryLineHeight

        // 4. Telemetry (Accuracy, Altitude, Bearing)
        val truncatedTelemetry = truncateTextToWidth(telemetryText, metaPaint, maxTextWidth)
        canvas.drawText(truncatedTelemetry, textLeft, currentY, metaPaint)
        currentY += timeLineHeight

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
