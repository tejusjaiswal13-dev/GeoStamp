package com.geostamp.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.geostamp.app.util.LocationFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * Generates a clean, static mini-map snapshot bitmap for captured photos.
 * Uses OpenStreetMap tiles when network is available, with an automatic
 * offline coordinate-grid fallback when network is unavailable.
 */
object MiniMapGenerator {

    private const val ZOOM = 15

    suspend fun generateMiniMap(
        context: Context,
        latitude: Double,
        longitude: Double,
        targetWidth: Int = 260,
        targetHeight: Int = 260,
        cornerRadius: Float = 20f
    ): Bitmap = withContext(Dispatchers.IO) {
        val baseBitmap = try {
            fetchOsmTile(context, latitude, longitude, targetWidth, targetHeight)
        } catch (e: Exception) {
            null
        } ?: generateOfflineMapGraphic(latitude, longitude, targetWidth, targetHeight)

        // Draw pin marker at exact center and clip rounded corners
        renderMarkerAndCrop(baseBitmap, targetWidth, targetHeight, cornerRadius)
    }

    private fun fetchOsmTile(
        context: Context,
        latitude: Double,
        longitude: Double,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val n = 1 shl ZOOM
        val xTileExact = (longitude + 180.0) / 360.0 * n
        val latRad = Math.toRadians(latitude)
        val yTileExact = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

        val xTile = xTileExact.toInt()
        val yTile = yTileExact.toInt()

        val tileUrl = "https://tile.openstreetmap.org/$ZOOM/$xTile/$yTile.png"
        val connection = (URL(tileUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2200
            readTimeout = 2200
            setRequestProperty("User-Agent", "GeoStamp-Android/${context.packageName}")
        }

        connection.inputStream.use { stream ->
            val tileBitmap = BitmapFactory.decodeStream(stream) ?: return null

            // Center around the exact sub-tile position
            val subX = ((xTileExact - xTile) * tileBitmap.width).toInt()
            val subY = ((yTileExact - yTile) * tileBitmap.height).toInt()

            val halfW = targetWidth / 2
            val halfH = targetHeight / 2

            val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            val srcLeft = max(0, subX - halfW)
            val srcTop = max(0, subY - halfH)
            val srcRight = min(tileBitmap.width, subX + halfW)
            val srcBottom = min(tileBitmap.height, subY + halfH)

            val srcRect = Rect(srcLeft, srcTop, srcRight, srcBottom)
            val dstRect = Rect(0, 0, targetWidth, targetHeight)

            canvas.drawBitmap(tileBitmap, srcRect, dstRect, null)
            tileBitmap.recycle()
            return result
        }
    }

    private fun generateOfflineMapGraphic(
        latitude: Double,
        longitude: Double,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Modern slate radar background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 34, 52) // Deep navy
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 56, 189, 248)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        val step = width / 5f
        for (i in 1..4) {
            canvas.drawLine(i * step, 0f, i * step, height.toFloat(), gridPaint)
            canvas.drawLine(0f, i * step, width.toFloat(), i * step, gridPaint)
        }

        // Radar circles
        val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 56, 189, 248)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawCircle(centerX, centerY, width * 0.22f, radarPaint)
        canvas.drawCircle(centerX, centerY, width * 0.40f, radarPaint)

        // Mini label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 203, 213, 225)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GPS RADAR", centerX, height - 16f, labelPaint)

        return bitmap
    }

    private fun renderMarkerAndCrop(
        source: Bitmap,
        width: Int,
        height: Int,
        cornerRadius: Float
    ): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Draw rounded mask
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }
        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, maskPaint)

        // 2. Transfer source inside mask
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, maskPaint)
        maskPaint.xfermode = null

        // 3. Draw border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 255, 255)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

        // 4. Draw prominent GPS Pin marker at center
        val centerX = width / 2f
        val centerY = height / 2f

        // Accuracy pulse circle
        val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 239, 68, 68) // Soft red pulse
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, 24f, pulsePaint)

        val pulseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 239, 68, 68)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(centerX, centerY, 24f, pulseRingPaint)

        // Pin shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 0, 0, 0)
        }
        canvas.drawCircle(centerX, centerY + 2f, 9f, shadowPaint)

        // Pin Head (Vibrant red with white center)
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(239, 68, 68)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY - 2f, 10f, pinPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY - 2f, 4f, corePaint)

        if (source != output) {
            source.recycle()
        }

        return output
    }
}
