package com.weargluco.watch.ui.main

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.weargluco.watch.ui.theme.glucoseColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Copper = Color(0xFFB87333)

@Composable
fun GlucoseScreen(
    viewModel: GlucoseViewModel,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colors.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry", fontSize = 12.sp)
                        }

                        Button(onClick = onLogout) {
                            Text("Logout", fontSize = 12.sp)
                        }
                    }
                }

                state.glucoseData != null -> {
                    GlucoseContent(
                        data = state.glucoseData!!,
                        onRefresh = { viewModel.refresh() },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun GlucoseContent(
    data: GlucoseUiData,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val color = glucoseColor(data.value, data.targetLow, data.targetHigh)
    val onSurface = MaterialTheme.colors.onSurface

    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1000)
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.US)
    val dateFormat = SimpleDateFormat("MMM d", Locale.US)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = data.patientName,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Text(
                text = data.lastUpdate,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "M",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        Text(
            text = "${data.value.toInt()}",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OldStyleTrendArrow(
                trendArrow = data.trendArrow,
                modifier = Modifier.size(width = 48.dp, height = 24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = data.trendLabel,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "mmol/L",
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )

        if (data.history.size >= 2) {
            Spacer(modifier = Modifier.height(6.dp))
            MiniGlucoseChart(
                values = data.history,
                targetLow = data.targetLow,
                targetHigh = data.targetHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = onSurface.copy(alpha = 0.1f),
                        radius = size.minDimension / 2
                    )
                    drawCircle(
                        color = onSurface.copy(alpha = 0.3f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1.5f)
                    )
                }
                Text(
                    text = timeFormat.format(now.time),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
            }

            Box(
                modifier = Modifier.size(width = 64.dp, height = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = 6f
                    drawRoundRect(
                        color = onSurface.copy(alpha = 0.1f),
                        cornerRadius = CornerRadius(r, r)
                    )
                    drawRoundRect(
                        color = onSurface.copy(alpha = 0.3f),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = 1.5f)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayFormat.format(now.time).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = dateFormat.format(now.time),
                        fontSize = 9.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onRefresh
            ) {
                Text("↻", fontSize = 14.sp)
            }
            Button(
                onClick = onLogout
            ) {
                Text("✕", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun OldStyleTrendArrow(
    trendArrow: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cw = size.width
        val ch = size.height
        val cx = cw / 2f
        val cy = ch / 2f

        if (trendArrow == 1 || trendArrow == 2 || trendArrow == 4 || trendArrow == 5) {
            val isDouble = trendArrow == 1 || trendArrow == 5
            val dir = if (trendArrow == 1 || trendArrow == 2) -1f else 1f
            val shaftLen = ch * 0.5f
            val headSize = ch * 0.5f

            val shaftTop = cy - dir * (shaftLen / 2f)
            val shaftBottom = cy + dir * (shaftLen / 2f)
            val headBase = shaftBottom - dir * headSize * 0.6f

            val path = Path().apply {
                moveTo(cx, shaftTop)
                lineTo(cx, headBase)
                lineTo(cx - headSize * 0.5f, headBase)
                lineTo(cx, shaftBottom)
                lineTo(cx + headSize * 0.5f, headBase)
                lineTo(cx, headBase)
            }

            drawPath(
                path = path,
                color = Copper,
                style = Stroke(
                    width = 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            if (isDouble) {
                val secondShaftTop = shaftTop - dir * headSize * 0.7f
                val secondShaftBottom = shaftTop
                val secondHeadBase = secondShaftBottom - dir * headSize * 0.6f

                val path2 = Path().apply {
                    moveTo(cx, secondShaftTop)
                    lineTo(cx, secondHeadBase)
                    lineTo(cx - headSize * 0.4f, secondHeadBase)
                    lineTo(cx, secondShaftBottom)
                    lineTo(cx + headSize * 0.4f, secondHeadBase)
                    lineTo(cx, secondHeadBase)
                }

                drawPath(
                    path = path2,
                    color = Copper,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        } else {
            val shaftLen = cw * 0.4f
            val headSize = cw * 0.35f
            val left = cx - shaftLen / 2f
            val headBase = left + shaftLen - headSize * 0.5f

            val path = Path().apply {
                moveTo(left, cy)
                lineTo(headBase, cy)
                lineTo(headBase, cy - headSize * 0.45f)
                lineTo(left + shaftLen, cy)
                lineTo(headBase, cy + headSize * 0.45f)
                lineTo(headBase, cy)
            }

            drawPath(
                path = path,
                color = Copper,
                style = Stroke(
                    width = 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
private fun MiniGlucoseChart(
    values: List<Double>,
    targetLow: Double,
    targetHigh: Double,
    modifier: Modifier = Modifier
) {
    val lineColor = Color(0xFF4FC3F7)
    val lowColor = Color(0x30FF9800)
    val highColor = Color(0x30FF4444)

    Canvas(modifier = modifier) {
        val maxVal = values.maxOrNull()?.toFloat()?.coerceAtLeast(12f) ?: 12f
        val minVal = values.minOrNull()?.toFloat()?.coerceAtMost(0f) ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = size.width / (values.size - 1).coerceAtLeast(1)

        val yLow = size.height * (1f - (targetLow.toFloat() - minVal) / range)
        val yHigh = size.height * (1f - (targetHigh.toFloat() - minVal) / range)

        drawRect(
            color = lowColor,
            topLeft = Offset(0f, yLow),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - yLow)
        )
        drawRect(
            color = highColor,
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width, yHigh)
        )

        if (values.size >= 2) {
            val path = Path()
            for (i in values.indices) {
                val x = i * stepX
                val y = size.height * (1f - ((values[i].toFloat() - minVal) / range))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }
    }
}

data class GlucoseUiData(
    val value: Double,
    val trendArrow: Int,
    val trendSymbol: String,
    val trendLabel: String,
    val lastUpdate: String,
    val patientName: String,
    val history: List<Double>,
    val targetLow: Double,
    val targetHigh: Double
)
