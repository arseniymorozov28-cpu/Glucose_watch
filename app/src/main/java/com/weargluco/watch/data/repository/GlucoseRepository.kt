package com.weargluco.watch.data.repository

import com.weargluco.watch.data.api.LibreLinkUpApi
import com.weargluco.watch.data.api.LibreLinkUpClient
import com.weargluco.watch.data.api.models.GlucoseMeasurement
import com.weargluco.watch.data.api.models.LoginRequest
import com.weargluco.watch.data.settings.AppSettings

class GlucoseRepository(
    private val settings: AppSettings
) {
    private var api: LibreLinkUpApi? = null

    private suspend fun getApi(): LibreLinkUpApi {
        val cached = api
        if (cached != null) return cached

        val region = settings.region
        val token = settings.token
        val newApi = LibreLinkUpClient.create(
            region = region,
            token = token
        )
        api = newApi
        return newApi
    }

    private suspend fun refreshApi(token: String) {
        val region = settings.region
        api = LibreLinkUpClient.create(region = region, token = token)
    }

    suspend fun login(email: String, password: String, region: String = "eu"): Result<String> {
        return try {
            settings.saveRegion(region)
            val tempApi = LibreLinkUpClient.create(region = region)
            val response = tempApi.login(LoginRequest(email, password))

            if (response.status == 0) {
                val token = response.data.authTicket.token
                val expires = response.data.authTicket.expires

                settings.saveCredentials(email, password)
                settings.saveToken(token, expires)
                refreshApi(token)

                val connections = tempApi.getConnections()
                if (connections.status == 0 && connections.data.isNotEmpty()) {
                    val patient = connections.data.first()
                    settings.savePatientInfo(patient.patientId, "${patient.firstName} ${patient.lastName}")
                }

                Result.success(token)
            } else {
                Result.failure(Exception("Login failed with status: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentGlucose(): Result<GlucoseData> {
        return try {
            val api = getApi()

            val connections = api.getConnections()
            if (connections.status != 0 || connections.data.isEmpty()) {
                return Result.failure(Exception("No connections found"))
            }

            val patient = connections.data.first()
            val patientId = patient.patientId

            settings.savePatientInfo(patientId, "${patient.firstName} ${patient.lastName}")

            val graph = api.getGraph(patientId)
            if (graph.status != 0) {
                return Result.failure(Exception("Failed to get graph data"))
            }

            val latest = graph.data.connection.glucoseMeasurement
            val history = graph.data.graphData

            if (latest != null) {
                Result.success(
                    GlucoseData(
                        valueMgDl = latest.ValueInMgPerDl,
                        value = latest.Value,
                        trendArrow = latest.TrendArrow ?: 3,
                        trendMessage = latest.TrendMessage,
                        timestamp = latest.Timestamp,
                        isHigh = latest.isHigh,
                        isLow = latest.isLow,
                        patientName = "${patient.firstName} ${patient.lastName}",
                        history = history.map { it.Value },
                        targetLow = patient.targetLow,
                        targetHigh = patient.targetHigh
                    )
                )
            } else {
                Result.failure(Exception("No glucose measurement available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GlucoseData(
    val valueMgDl: Double,
    val value: Double,
    val trendArrow: Int,
    val trendMessage: String?,
    val timestamp: String,
    val isHigh: Boolean,
    val isLow: Boolean,
    val patientName: String,
    val history: List<Double>,
    val targetLow: Int,
    val targetHigh: Int
) {
    val trendSymbol: String
        get() = when (trendArrow) {
            1 -> "\u2B07\u2B07"
            2 -> "\u2B07"
            3 -> "\u2796"
            4 -> "\u2B06"
            5 -> "\u2B06\u2B06"
            else -> "\u2796"
        }

    val trendLabel: String
        get() = when (trendArrow) {
            1 -> "Rapidly falling"
            2 -> "Falling"
            3 -> "Stable"
            4 -> "Rising"
            5 -> "Rapidly rising"
            else -> "Unknown"
        }
}
