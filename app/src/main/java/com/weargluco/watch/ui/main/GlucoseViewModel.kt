package com.weargluco.watch.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null
)

class GlucoseViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = AppSettings(application)
    private val repository = GlucoseRepository(settings)
    private var refreshJob: Job? = null

    private val _uiState = MutableStateFlow(GlucoseUiState())
    val uiState: StateFlow<GlucoseUiState> = _uiState.asStateFlow()

    init {
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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

    private fun startAutoRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                refresh()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
