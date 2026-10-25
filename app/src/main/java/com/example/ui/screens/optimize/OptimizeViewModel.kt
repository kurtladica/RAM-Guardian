package com.example.ui.screens.optimize

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.apps.AppFilterType
import com.example.data.apps.AppInfoModel
import com.example.data.apps.AppRepository
import com.example.data.apps.AppSortType
import com.example.data.memory.MemoryInfoData
import com.example.data.memory.MemoryMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoryComparisonData(
    val baseline: MemoryInfoData,
    val current: MemoryInfoData,
    val usedBytesDelta: Long, // negative = freed memory, positive = increased memory
    val percentageDelta: Float
) {
    fun formattedDelta(): String {
        val sign = if (usedBytesDelta <= 0) "-" else "+"
        val absBytes = kotlin.math.abs(usedBytesDelta)
        return "$sign${MemoryInfoData.formatBytes(absBytes)} (${String.format(java.util.Locale.US, "%+.1f%%", percentageDelta)})"
    }
}

data class ChecklistItem(
    val id: String,
    val title: String,
    val description: String,
    val isChecked: Boolean = false
)

data class OptimizeUiState(
    val currentMemory: MemoryInfoData? = null,
    val historyPercentages: List<Float> = emptyList(),
    val baselineMemory: MemoryInfoData? = null,
    val comparison: MemoryComparisonData? = null,
    val recentApps: List<AppInfoModel> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val isLoading: Boolean = true,
    val checklist: List<ChecklistItem> = defaultChecklist()
) {
    companion object {
        fun defaultChecklist(): List<ChecklistItem> = listOf(
            ChecklistItem(
                id = "save_work",
                title = "Save ongoing work in apps & browsers",
                description = "Ensure open document drafts, unsaved forms, and active browser tabs are saved before running demanding games."
            ),
            ChecklistItem(
                id = "close_recents",
                title = "Swipe away heavy tasks from Recent Apps",
                description = "Dismiss background video editors, maps, or camera sessions using the system Recents overview."
            ),
            ChecklistItem(
                id = "check_temperature",
                title = "Verify phone is not hot or charging fast",
                description = "Thermal throttling lowers CPU and GPU clock speeds regardless of available RAM capacity."
            ),
            ChecklistItem(
                id = "disable_power_save",
                title = "Turn off Extreme Battery Saver for gaming",
                description = "Aggressive battery saver modes deliberately restrict CPU cores and drop frame rates."
            )
        )
    }
}

class OptimizeViewModel(application: Application) : AndroidViewModel(application) {

    private val memoryMonitor = MemoryMonitor(application)
    private val appRepository = AppRepository(application)

    private val _uiState = MutableStateFlow(OptimizeUiState())
    val uiState: StateFlow<OptimizeUiState> = _uiState.asStateFlow()

    init {
        loadOptimizationData()
    }

    fun loadOptimizationData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val memoryResult = memoryMonitor.getMemorySnapshot()
            val hasUsage = appRepository.hasUsageAccessPermission()
            val appsResult = appRepository.getInstalledApps()

            val topRecentApps = if (hasUsage) {
                val allApps = appsResult.getOrDefault(emptyList())
                val sorted = AppRepository.sortApps(allApps, AppSortType.RECENTLY_USED)
                sorted.filter { (it.lastTimeUsedMillis ?: 0L) > 0L }.take(5)
            } else {
                emptyList()
            }

            _uiState.update { state ->
                val mem = memoryResult.getOrNull() ?: state.currentMemory
                val updatedHistory = if (mem != null) {
                    (state.historyPercentages + mem.usagePercentage).takeLast(20)
                } else {
                    state.historyPercentages
                }
                state.copy(
                    currentMemory = mem,
                    historyPercentages = updatedHistory,
                    recentApps = topRecentApps,
                    hasUsageAccess = hasUsage,
                    isLoading = false
                )
            }
        }
    }

    fun captureBaseline() {
        viewModelScope.launch {
            val result = memoryMonitor.getMemorySnapshot()
            result.onSuccess { mem ->
                _uiState.update {
                    it.copy(
                        baselineMemory = mem,
                        comparison = null
                    )
                }
            }
        }
    }

    fun captureAfterActions() {
        val baseline = _uiState.value.baselineMemory ?: return
        viewModelScope.launch {
            val result = memoryMonitor.getMemorySnapshot()
            result.onSuccess { current ->
                val usedDelta = current.usedBytes - baseline.usedBytes
                val percentDelta = current.usagePercentage - baseline.usagePercentage
                val comp = MemoryComparisonData(
                    baseline = baseline,
                    current = current,
                    usedBytesDelta = usedDelta,
                    percentageDelta = percentDelta
                )
                _uiState.update {
                    it.copy(
                        currentMemory = current,
                        comparison = comp
                    )
                }
            }
        }
    }

    fun resetComparison() {
        _uiState.update {
            it.copy(
                baselineMemory = null,
                comparison = null
            )
        }
    }

    fun toggleChecklistItem(id: String) {
        _uiState.update { state ->
            val updated = state.checklist.map { item ->
                if (item.id == id) item.copy(isChecked = !item.isChecked) else item
            }
            state.copy(checklist = updated)
        }
    }
}
