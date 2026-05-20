package com.weargluco.watch.data.repository

import com.weargluco.watch.data.api.LibreLinkUpApi
import com.weargluco.watch.data.api.LibreLinkUpClient
import com.weargluco.watch.data.api.models.GlucoseMeasurement
import com.weargluco.watch.data.api.models.LoginRequest
import com.weargluco.watch.data.settings.AppSettings
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.security.MessageDigest

class GlucoseRepository(
    private val settings: AppSettings
) {
    private var api: LibreLinkUpApi? = null

    private suspend fun getApi(): LibreLinkUpApi {
        val cached = api
        if (cached != null) return cached

        val region = settings.region.first()
        val token = settings.token.first()
        val accountId = settings.accountId.first().ifEmpty { null }
        val newApi = LibreLinkUpClient.create(
            region = region,
            token = token,
            accountId = accountId
        )
        api = newApi
        return newApi
    }

    private suspend fun refreshApi(token: String, accountId: String? = null) {
        val region = settings.region.first()
        api = LibreLinkUpClient.create(region = region, token = token, accountId = accountId)
    }

    suspend fun login(email: String, password: String, region: String = "eu"): Result<String> {
        return try {
            settings.saveRegion(region)
            val tempApi = LibreLinkUpClient.create(region = region)
            val response = tempApi.login(LoginRequest(email, password))

            if (response.status == 0 && response.data != null) {
                val token = response.data.authTicket.token
                val expires = response.data.authTicket.expires
                val userId = response.data.user.id

                val accountId = computeAccountId(userId)
                settings.saveAccountId(accountId)
                settings.saveCredentials(email, password)
                settings.saveToken(token, expires)
                refreshApi(token, accountId)

                val connections = getApi().getConnections()
                if (connections.status == 0 && connections.data.isNotEmpty()) {
                    val patient = connections.data.first()
                    settings.savePatientInfo(patient.patientId, "${patient.firstName} ${patient.lastName}")
                }

                Result.success(token)
            } else {
                val serverError = response.error?.message
                val errorMsg = serverError ?: "Login failed (status=${response.status})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is HttpException -> {
                    val body = e.response()?.errorBody()?.string()
                    "HTTP ${e.code()}: ${body ?: e.message()}"
                }
                else -> e.message ?: "Login failed"
            }
            Result.failure(Exception(errorMsg))
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
                val data = GlucoseData(
                    valueMgDl = latest.ValueInMgPerDl,
                    value = latest.ValueInMgPerDl.toMmolL(),
                    trendArrow = latest.TrendArrow ?: 3,
                    trendMessage = latest.TrendMessage,
                    timestamp = latest.Timestamp,
                    isHigh = latest.isHigh,
                    isLow = latest.isLow,
                    patientName = "${patient.firstName} ${patient.lastName}",
                    history = history.map { it.ValueInMgPerDl.toMmolL() },
                    targetLow = patient.targetLow.toDouble().toMmolL(),
                    targetHigh = patient.targetHigh.toDouble().toMmolL()
                )
                settings.saveLatestGlucose(
                    value = data.value,
                    trendSymbol = data.trendSymbol,
                    trendLabel = data.trendLabel,
                    timestamp = data.timestamp,
                    targetLow = data.targetLow,
                    targetHigh = data.targetHigh
                )
                Result.success(data)
            } else {
                Result.failure(Exception("No glucose measurement available"))
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is HttpException -> {
                    val body = e.response()?.errorBody()?.string()
                    "HTTP ${e.code()}: ${body ?: e.message()}"
                }
                else -> e.message ?: "Unknown error"
            }
            Result.failure(Exception(errorMsg))
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
    val targetLow: Double,
    val targetHigh: Double
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

private fun Double.toMmolL(): Double = this / 18.0182

private fun computeAccountId(userId: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
