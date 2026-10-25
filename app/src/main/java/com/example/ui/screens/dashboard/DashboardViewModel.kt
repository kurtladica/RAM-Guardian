package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.memory.MemoryInfoData
import com.example.data.memory.MemoryMonitor
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val memoryData: MemoryInfoData? = null,
    val historyPercentages: List<Float> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val lastUpdatedFormatted: String = "Connecting...",
    val isPollingActive: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val memoryMonitor = MemoryMonitor(application)
    private val settingsRepository = SettingsRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private val maxHistoryPoints = 20

    init {
        // Observe history clear events from Settings
        viewModelScope.launch {
            settingsRepository.historyClearTrigger.collect {
                if (it > 0L) {
                    _uiState.update { state -> state.copy(historyPercentages = emptyList()) }
                }
            }
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        _uiState.update { it.copy(isPollingActive = true) }

        val interval = settingsRepository.refreshIntervalMillis.value

        pollingJob = viewModelScope.launch {
            memoryMonitor.observeMemory(pollIntervalMillis = interval)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Failed reading memory stats"
                        )
                    }
                }
                .collect { memoryInfo ->
                    recordMeasurement(memoryInfo)
                }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.update { it.copy(isPollingActive = false) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.memoryData == null) }
            val result = memoryMonitor.getMemorySnapshot()
            result.onSuccess { memoryInfo ->
                recordMeasurement(memoryInfo)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Error refreshing memory telemetry"
                    )
                }
            }
        }
    }

    private fun recordMeasurement(memoryInfo: MemoryInfoData) {
        val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date(memoryInfo.timestampMillis))

        _uiState.update { state ->
            val updatedHistory = (state.historyPercentages + memoryInfo.usagePercentage)
                .takeLast(maxHistoryPoints)

            state.copy(
                memoryData = memoryInfo,
                historyPercentages = updatedHistory,
                isLoading = false,
                errorMessage = null,
                lastUpdatedFormatted = "Updated $timeString"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
