package com.weargluco.watch.data.models

import kotlinx.serialization.Serializable

@Serializable
data class BleGlucoseData(
    val value: Double,
    val trendArrow: Int,
    val trendLabel: String,
    val timestamp: String,
    val patientName: String,
    val isHigh: Boolean,
    val isLow: Boolean,
    val targetLow: Double,
    val targetHigh: Double
)
