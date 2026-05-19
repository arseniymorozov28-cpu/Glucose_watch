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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

        Spacer(modifier = Modifier.height(4.dp))

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
            Text(
                text = data.trendSymbol,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.padding(4.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
            MiniGlucoseChart(
                values = data.history,
                targetLow = data.targetLow,
                targetHigh = data.targetHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

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
private fun MiniGlucoseChart(
    values: List<Double>,
    targetLow: Int,
    targetHigh: Int,
    modifier: Modifier = Modifier
) {
    val lineColor = Color(0xFF4FC3F7)
    val lowColor = Color(0x30FF9800)
    val highColor = Color(0x30FF4444)

    Canvas(modifier = modifier) {
        val maxVal = values.maxOrNull()?.toFloat()?.coerceAtLeast(200f) ?: 200f
        val minVal = values.minOrNull()?.toFloat()?.coerceAtMost(50f) ?: 50f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val centerY = size.height / 2

        val yLow = size.height * (1f - (targetLow - minVal) / range)
        val yHigh = size.height * (1f - (targetHigh - minVal) / range)

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
            points@ for (i in values.indices) {
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
    val trendSymbol: String,
    val trendLabel: String,
    val lastUpdate: String,
    val patientName: String,
    val history: List<Double>,
    val targetLow: Int,
    val targetHigh: Int
)
