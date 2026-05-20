package com.weargluco.watch.ui.main

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weargluco.watch.data.ble.GlucoseBLEReceiverService
import com.weargluco.watch.data.repository.GlucoseRepository
import com.weargluco.watch.data.settings.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GlucoseUiState(
    val glucoseData: GlucoseUiData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bleConnected: Boolean = false
)

class GlucoseViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = AppSettings(application)
    private val repository = GlucoseRepository(settings)
    private var refreshJob: Job? = null
    private var bleRefreshJob: Job? = null

    private val _uiState = MutableStateFlow(GlucoseUiState())
    val uiState: StateFlow<GlucoseUiState> = _uiState.asStateFlow()

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val value = intent.getDoubleExtra(GlucoseBLEReceiverService.EXTRA_GLUCOSE_DATA, 0.0)
            val bleData = GlucoseBLEReceiverService.lastGlucoseData
            if (bleData != null) {
                _uiState.value = GlucoseUiState(
                    glucoseData = GlucoseUiData(
                        value = bleData.value,
                        trendArrow = bleData.trendArrow,
                        trendSymbol = trendSymbol(bleData.trendArrow),
                        trendLabel = bleData.trendLabel,
                        lastUpdate = bleData.timestamp,
                        patientName = bleData.patientName,
                        history = emptyList(),
                        targetLow = bleData.targetLow,
                        targetHigh = bleData.targetHigh
                    ),
                    isLoading = false,
                    bleConnected = true
                )
            }
        }
    }

    init {
        val context = getApplication<Application>()
        context.registerReceiver(bleReceiver, IntentFilter(GlucoseBLEReceiverService.ACTION_DATA_UPDATED), Context.RECEIVER_NOT_EXPORTED)
        startBLEService(context)
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val bleData = GlucoseBLEReceiverService.lastGlucoseData
        if (bleData != null) {
            _uiState.value = GlucoseUiState(
                glucoseData = GlucoseUiData(
                    value = bleData.value,
                    trendArrow = bleData.trendArrow,
                    trendSymbol = trendSymbol(bleData.trendArrow),
                    trendLabel = bleData.trendLabel,
                    lastUpdate = bleData.timestamp,
                    patientName = bleData.patientName,
                    history = emptyList(),
                    targetLow = bleData.targetLow,
                    targetHigh = bleData.targetHigh
                ),
                isLoading = false,
                bleConnected = true
            )
            return
        }

        viewModelScope.launch {
            val result = repository.getCurrentGlucose()
            result.fold(
                onSuccess = { data ->
                    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    dateFormat.timeZone = TimeZone.getDefault()
                    val timeStr = try {
                        val ts = data.timestamp
                        val sdf = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        val date = sdf.parse(ts)
                        date?.let { dateFormat.format(it) } ?: "?"
                    } catch (e: Exception) {
                        "?"
                    }

                    _uiState.value = GlucoseUiState(
                        glucoseData = GlucoseUiData(
                            value = data.value,
                            trendArrow = data.trendArrow,
                            trendSymbol = data.trendSymbol,
                            trendLabel = data.trendLabel,
                            lastUpdate = timeStr,
                            patientName = data.patientName,
                            history = data.history,
                            targetLow = data.targetLow,
                            targetHigh = data.targetHigh
                        ),
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to get glucose data"
                    )
                }
            )
        }
    }

    private fun startBLEService(context: Context) {
        val intent = Intent(context, GlucoseBLEReceiverService::class.java)
        context.startService(intent)
    }

    private fun startAutoRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                refresh()
            }
        }
    }

    private fun trendSymbol(arrow: Int): String = when (arrow) {
        1 -> "\u2B07\u2B07"
        2 -> "\u2B07"
        3 -> "\u2796"
        4 -> "\u2B06"
        5 -> "\u2B06\u2B06"
        else -> "\u2796"
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        try {
            getApplication<Application>().unregisterReceiver(bleReceiver)
        } catch (_: Exception) {}
    }
}
