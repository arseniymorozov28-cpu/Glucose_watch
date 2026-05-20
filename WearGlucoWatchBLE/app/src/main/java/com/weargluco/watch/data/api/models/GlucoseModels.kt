package com.weargluco.watch.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GraphResponse(
    val status: Int,
    val data: GraphData
)

@Serializable
data class GraphData(
    val connection: ConnectionData,
    val graphData: List<GlucoseMeasurement>,
    val activeSensors: List<ActiveSensor>?
)

@Serializable
data class GlucoseMeasurement(
    val FactoryTimestamp: String,
    val Timestamp: String,
    val type: Int,
    val ValueInMgPerDl: Double,
    val TrendArrow: Int? = null,
    val TrendMessage: String? = null,
    val MeasurementColor: Int,
    val GlucoseUnits: Int,
    val Value: Double,
    val isHigh: Boolean,
    val isLow: Boolean
)

@Serializable
data class ActiveSensor(
    val sensor: SensorInfo,
    val device: DeviceInfo
)

@Serializable
data class DeviceInfo(
    val did: String,
    val dtid: Int,
    val v: String,
    val ll: Int,
    val hl: Int,
    val u: Long,
    val alarms: Boolean
)
