package com.weargluco.watch.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionsResponse(
    val status: Int,
    val data: List<ConnectionData>
)

@Serializable
data class ConnectionData(
    val id: String,
    val patientId: String,
    val country: String,
    val status: Int,
    val firstName: String,
    val lastName: String,
    val targetLow: Int,
    val targetHigh: Int,
    val uom: Int,
    val glucoseMeasurement: GlucoseMeasurement?,
    val sensor: SensorInfo?,
    val alarmRules: AlarmRules?
)

@Serializable
data class SensorInfo(
    val deviceId: String,
    val sn: String,
    val a: Long,
    val w: Int,
    val pt: Int
)

@Serializable
data class AlarmRules(
    val c: Boolean,
    val h: AlarmThreshold,
    val l: AlarmThreshold,
    val f: FastDropAlarm
)

@Serializable
data class AlarmThreshold(
    val on: Boolean? = null,
    val th: Int,
    val thmm: Double,
    val d: Int
)

@Serializable
data class FastDropAlarm(
    val th: Int,
    val thmm: Double,
    val d: Int,
    val tl: Int,
    val tlmm: Double
)
