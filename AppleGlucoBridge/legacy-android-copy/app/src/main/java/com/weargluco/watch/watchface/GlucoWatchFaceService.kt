package com.weargluco.watch.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.weargluco.watch.data.repository.GlucoseRepository
import com.weargluco.watch.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GlucoWatchFaceService : WatchFaceService() {

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val settings = AppSettings(applicationContext)
        val repository = GlucoseRepository(settings)

        return WatchFace(
            WatchFaceType.ANALOG,
            GlucoRenderer(
                surfaceHolder = surfaceHolder,
                currentUserStyleRepository = currentUserStyleRepository,
                watchState = watchState,
                settings = settings,
                repository = repository
            )
        )
    }

    private class GlucoRenderer(
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        settings: AppSettings,
        repository: GlucoseRepository
    ) : Renderer.CanvasRenderer2<GlucoSharedAssets>(
        surfaceHolder,
        currentUserStyleRepository,
        watchState,
        CanvasType.HARDWARE,
        interactiveDrawModeUpdateDelayMillis = 1_000L,
        clearWithBackgroundTintBeforeRenderingHighlightLayer = false
    ) {
        private val backgroundPaint = Paint().apply { color = Color.BLACK }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val copperColor = Color.rgb(184, 115, 51) // #B87333 Bronze/Copper
        private val goldColor = Color.rgb(212, 175, 55)   // #D4AF37 Metallic Gold
        
        private val sharedAssetsInstance = GlucoSharedAssets(settings, repository) {
            invalidate()
        }

        override suspend fun createSharedAssets(): GlucoSharedAssets = sharedAssetsInstance

        override fun render(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: GlucoSharedAssets
        ) {
            val snapshot = sharedAssets.snapshot.get()
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            val radius = min(bounds.width(), bounds.height()) * 0.46f
            val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT

            canvas.drawRect(bounds, backgroundPaint)
            
            // 1. Dial elements (Bezel, marks, numbers)
            drawGlucoseDial(canvas, centerX, centerY, radius, isAmbient)
            
            // 2. Letter "M" under 12
            drawInitialM(canvas, centerX, centerY, radius, isAmbient)

            // 3. Time Hands (Analog)
            drawMainTimeHands(canvas, centerX, centerY, radius, zonedDateTime, isAmbient)

            if (snapshot.value != null) {
                // 4. Date Window at 3
                drawDateWindow(canvas, centerX, centerY, radius, zonedDateTime, isAmbient)
                
                // 5. Glucose Value above 6
                drawSmallGlucoseValue(canvas, centerX, centerY, radius, snapshot, isAmbient)
                
                // 6. Victorian Steampunk Glucose Needle
                drawSteampunkNeedle(canvas, centerX, centerY, radius, snapshot.value, isAmbient)
            } else {
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textSize = radius * 0.16f
                textPaint.color = Color.rgb(79, 195, 247)
                canvas.drawText("GlucoWatch", centerX, centerY + radius * 0.05f, textPaint)
            }
        }

        private fun drawInitialM(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, isAmbient: Boolean) {
            textPaint.typeface = Typeface.create("serif", Typeface.ITALIC)
            textPaint.textSize = radius * 0.30f
            textPaint.color = if (isAmbient) Color.WHITE else goldColor
            textPaint.textAlign = Paint.Align.CENTER
            // Positioned even lower under 12
            canvas.drawText("M", centerX, centerY - radius * 0.35f, textPaint)
        }

        private fun drawDateWindow(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, zonedDateTime: ZonedDateTime, isAmbient: Boolean) {
            val rightX = centerX + radius * 0.40f // Moved further left (from 0.45f)
            val rectWidth = radius * 0.45f
            val rectHeight = radius * 0.18f
            val rect = RectF(rightX - rectWidth/2, centerY - rectHeight/2, rightX + rectWidth/2, centerY + rectHeight/2)
            
            strokePaint.color = Color.argb(100, 255, 255, 255)
            strokePaint.strokeWidth = radius * 0.01f
            canvas.drawRect(rect, strokePaint)
            
            val day = zonedDateTime.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).uppercase()
            val number = zonedDateTime.format(DateTimeFormatter.ofPattern("d", Locale.US))
            
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = radius * 0.10f
            textPaint.color = Color.WHITE
            canvas.drawText("$day | $number", rightX, centerY + textPaint.textSize * 0.35f, textPaint)
        }

        private fun drawSmallGlucoseValue(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, snapshot: FaceSnapshot, isAmbient: Boolean) {
            val value = snapshot.value ?: return
            val color = if (isAmbient) Color.WHITE else glucoseZoneColor(value)
            
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = radius * 0.25f // Increased size (from 0.22f)
            textPaint.color = color
            // Moved higher (from 0.45f)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", value), centerX, centerY + radius * 0.42f, textPaint)
            
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = radius * 0.08f
            textPaint.color = Color.argb(150, 255, 255, 255)
            // Moved higher accordingly
            canvas.drawText("mmol/L", centerX, centerY + radius * 0.52f, textPaint)
        }

        private fun drawSteampunkNeedle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, value: Double, isAmbient: Boolean) {
            val needleValue = repeatedDialValue(value)
            val angle = Math.toRadians((needleValue / 12.0 * 360.0) - 90.0)
            
            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(Math.toDegrees(angle).toFloat() + 90f)
            
            val len = radius * 0.82f
            val cColor = if (isAmbient) Color.WHITE else copperColor
            
            strokePaint.color = cColor
            fillPaint.color = cColor
            
            // --- Elaborate Victorian Steampunk Hand ---
            val path = Path()
            // Main shaft base (narrow)
            path.moveTo(-radius * 0.015f, 0f)
            path.lineTo(-radius * 0.015f, -len * 0.2f)
            
            // Decorative widening (diamond shape)
            path.lineTo(-radius * 0.04f, -len * 0.35f)
            path.lineTo(0f, -len * 0.5f)
            path.lineTo(radius * 0.04f, -len * 0.35f)
            path.lineTo(radius * 0.015f, -len * 0.2f)
            path.lineTo(radius * 0.015f, 0f)
            path.close()
            canvas.drawPath(path, fillPaint)
            
            // Long thin needle section
            strokePaint.strokeWidth = radius * 0.015f
            canvas.drawLine(0f, -len * 0.5f, 0f, -len * 0.95f, strokePaint)
            
            // Sharp sharp tip
            strokePaint.strokeWidth = radius * 0.005f
            canvas.drawLine(0f, -len * 0.95f, 0f, -len, strokePaint)
            
            // Decorative "engraving" look lines
            strokePaint.color = Color.argb(80, 0, 0, 0)
            strokePaint.strokeWidth = radius * 0.005f
            canvas.drawLine(0f, -len * 0.1f, 0f, -len * 0.45f, strokePaint)
            
            // Hub
            fillPaint.color = cColor
            canvas.drawCircle(0f, 0f, radius * 0.05f, fillPaint)
            strokePaint.color = Color.argb(100, 0, 0, 0)
            canvas.drawCircle(0f, 0f, radius * 0.03f, strokePaint)
            
            canvas.restore()
        }

        private fun drawMainTimeHands(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, zonedDateTime: ZonedDateTime, isAmbient: Boolean) {
            val hour = (zonedDateTime.hour % 12) + zonedDateTime.minute / 60f
            val minute = zonedDateTime.minute + zonedDateTime.second / 60f
            
            // Vintage style hands (Gold/Light Copper)
            val handColor = if (isAmbient) Color.WHITE else Color.rgb(230, 200, 100)
            
            drawVintageHand(canvas, centerX, centerY, radius * 0.55f, hour / 12f, handColor, radius * 0.04f)
            drawVintageHand(canvas, centerX, centerY, radius * 0.78f, minute / 60f, handColor, radius * 0.025f)
            
            // Small cap in center
            fillPaint.color = handColor
            canvas.drawCircle(centerX, centerY, radius * 0.025f, fillPaint)
        }

        private fun drawVintageHand(canvas: Canvas, centerX: Float, centerY: Float, length: Float, turn: Float, color: Int, width: Float) {
            val angle = Math.toRadians((turn * 360.0) - 90.0)
            strokePaint.color = color
            strokePaint.strokeWidth = width
            
            val endX = centerX + cos(angle).toFloat() * length
            val endY = centerY + sin(angle).toFloat() * length
            
            // Draw shaft with a small diamond shape near the end for vintage look
            val midX = centerX + cos(angle).toFloat() * length * 0.8f
            val midY = centerY + sin(angle).toFloat() * length * 0.8f
            
            canvas.drawLine(centerX, centerY, endX, endY, strokePaint)
            
            // Add a small ornate diamond
            fillPaint.color = color
            val pX = centerX + cos(angle).toFloat() * length * 0.7f
            val pY = centerY + sin(angle).toFloat() * length * 0.7f
            val perpAngle = angle + Math.PI / 2
            val dPath = Path()
            val dw = width * 1.5f
            dPath.moveTo(pX + cos(perpAngle).toFloat() * dw, pY + sin(perpAngle).toFloat() * dw)
            dPath.lineTo(midX, midY)
            dPath.lineTo(pX - cos(perpAngle).toFloat() * dw, pY - sin(perpAngle).toFloat() * dw)
            dPath.lineTo(centerX + cos(angle).toFloat() * length * 0.6f, centerY + sin(angle).toFloat() * length * 0.6f)
            dPath.close()
            canvas.drawPath(dPath, fillPaint)
        }

        private fun drawGlucoseDial(
            canvas: Canvas,
            centerX: Float,
            centerY: Float,
            radius: Float,
            isAmbient: Boolean
        ) {
            val ring = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            strokePaint.strokeWidth = radius * 0.07f
            if (isAmbient) {
                strokePaint.color = Color.DKGRAY
                canvas.drawCircle(centerX, centerY, radius, strokePaint)
            } else {
                drawGlucoseArc(canvas, ring, 0.0, 3.0, Color.rgb(220, 30, 40))
                drawGlucoseArc(canvas, ring, 3.0, 4.0, Color.rgb(255, 190, 40))
                drawGlucoseArc(canvas, ring, 4.0, 8.5, Color.rgb(55, 190, 95))
                drawGlucoseArc(canvas, ring, 8.5, 12.0, Color.rgb(255, 132, 32))
            }

            for (tick in 0 until 60) {
                val isHour = tick % 5 == 0
                val angle = Math.toRadians((tick * 6.0) - 90.0)
                val outer = radius * 0.94f
                val inner = if (isHour) radius * 0.82f else radius * 0.89f
                strokePaint.strokeWidth = if (isHour) radius * 0.018f else radius * 0.007f
                strokePaint.color = if (isHour) Color.WHITE else Color.rgb(95, 95, 95)
                canvas.drawLine(
                    centerX + cos(angle).toFloat() * inner,
                    centerY + sin(angle).toFloat() * inner,
                    centerX + cos(angle).toFloat() * outer,
                    centerY + sin(angle).toFloat() * outer,
                    strokePaint
                )
            }

            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = radius * 0.11f
            textPaint.color = Color.WHITE
            textPaint.textAlign = Paint.Align.CENTER
            for (hour in 1..12) {
                val angle = Math.toRadians((hourToDialValue(hour) / 12.0 * 360.0) - 90.0)
                val labelRadius = radius * 0.68f
                val x = centerX + cos(angle).toFloat() * labelRadius
                val y = centerY + sin(angle).toFloat() * labelRadius + textPaint.textSize * 0.35f
                canvas.drawText(hour.toString(), x, y, textPaint)
            }
        }

        private fun drawGlucoseArc(canvas: Canvas, ring: RectF, startValue: Double, endValue: Double, color: Int) {
            strokePaint.color = color
            canvas.drawArc(
                ring,
                valueToAngle(startValue),
                ((endValue - startValue) / 12.0 * 360.0).toFloat(),
                false,
                strokePaint
            )
        }

        override fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: GlucoSharedAssets
        ) {
            renderParameters.highlightLayer?.let {
                canvas.drawColor(it.backgroundTint)
            }
        }
    }

    private class GlucoSharedAssets(
        private val settings: AppSettings,
        private val repository: GlucoseRepository,
        private val invalidate: () -> Unit
    ) : Renderer.SharedAssets {
        val snapshot = AtomicReference(FaceSnapshot())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        init {
            scope.launch {
                while (isActive) {
                    loadStoredSnapshot()
                    if (settings.isLoggedIn()) {
                        repository.getCurrentGlucose()
                        loadStoredSnapshot()
                    }
                    withContext(Dispatchers.Main.immediate) {
                        invalidate()
                    }
                    delay(5 * 60 * 1_000L)
                }
            }
        }

        private suspend fun loadStoredSnapshot() {
            val value = settings.latestGlucose.first().toDoubleOrNull()
            if (value == null) {
                snapshot.set(FaceSnapshot(message = "Open app to connect"))
                return
            }

            snapshot.set(
                FaceSnapshot(
                    value = value,
                    trendSymbol = settings.latestTrend.first(),
                    trendLabel = settings.latestTrendLabel.first(),
                    lastUpdate = formatLibreTime(settings.latestTimestamp.first()),
                    targetLow = settings.latestTargetLow.first().toDoubleOrNull() ?: 4.0,
                    targetHigh = settings.latestTargetHigh.first().toDoubleOrNull() ?: 8.5
                )
            )
        }

        override fun onDestroy() {
            scope.cancel()
        }

        private fun formatLibreTime(timestamp: String): String {
            return try {
                val input = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val output = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
                input.parse(timestamp)?.let(output::format) ?: "--:--"
            } catch (_: Exception) {
                "--:--"
            }
        }
    }

    private data class FaceSnapshot(
        val value: Double? = null,
        val trendSymbol: String = "",
        val trendLabel: String = "",
        val lastUpdate: String = "--:--",
        val targetLow: Double = 4.0,
        val targetHigh: Double = 8.5,
        val message: String = "No glucose data"
    )
}

private fun valueToAngle(value: Double): Float = ((value / 12.0 * 360.0) - 90.0).toFloat()

private fun repeatedDialValue(value: Double): Double {
    if (value <= 0.0) return 0.0
    val repeated = value % 12.0
    return if (repeated == 0.0) 12.0 else repeated
}

private fun hourToDialValue(hour: Int): Double = if (hour == 12) 0.0 else hour.toDouble()

private fun glucoseZoneColor(value: Double): Int {
    return when {
        value < 3.0 -> Color.rgb(220, 30, 40)
        value < 4.0 -> Color.rgb(255, 190, 40)
        value <= 8.5 -> Color.rgb(55, 190, 95)
        else -> Color.rgb(255, 132, 32)
    }
}
