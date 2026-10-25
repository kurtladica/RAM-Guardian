package com.example.ui.screens.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.apps.AppFilterType
import com.example.data.apps.AppInfoModel
import com.example.data.apps.AppRepository
import com.example.data.apps.AppSortType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppsUiState(
    val rawApps: List<AppInfoModel> = emptyList(),
    val displayedApps: List<AppInfoModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedFilter: AppFilterType = AppFilterType.ALL,
    val selectedSort: AppSortType = AppSortType.NAME_ASC,
    val selectedPackageNames: Set<String> = emptySet(),
    val selectedAppForDetails: AppInfoModel? = null,
    val hasUsageAccess: Boolean = false,
    val showUsageExplainer: Boolean = false,
    val totalCount: Int = 0,
    val userCount: Int = 0,
    val systemCount: Int = 0
)

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val hasUsage = repository.hasUsageAccessPermission()
            val result = repository.getInstalledApps()

            result.onSuccess { apps ->
                val userCount = apps.count { !it.isSystemApp || it.isUpdatedSystemApp }
                val systemCount = apps.count { it.isSystemApp && !it.isUpdatedSystemApp }

                _uiState.update { state ->
                    val filtered = applyFilterAndSort(
                        apps = apps,
                        query = state.searchQuery,
                        filter = state.selectedFilter,
                        sort = state.selectedSort
                    )
                    state.copy(
                        rawApps = apps,
                        displayedApps = filtered,
                        isLoading = false,
                        errorMessage = null,
                        hasUsageAccess = hasUsage,
                        totalCount = apps.size,
                        userCount = userCount,
                        systemCount = systemCount
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to inspect installed applications"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                apps = state.rawApps,
                query = query,
                filter = state.selectedFilter,
                sort = state.selectedSort
            )
            state.copy(
                searchQuery = query,
                displayedApps = filtered
            )
        }
    }

    fun onFilterChanged(filter: AppFilterType) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                apps = state.rawApps,
                query = state.searchQuery,
                filter = filter,
                sort = state.selectedSort
            )
            state.copy(
                selectedFilter = filter,
                displayedApps = filtered
            )
        }
    }

    fun onSortChanged(sort: AppSortType) {
        val hasUsage = repository.hasUsageAccessPermission()
        val showExplainer = (sort == AppSortType.RECENTLY_USED && !hasUsage)

        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                apps = state.rawApps,
                query = state.searchQuery,
                filter = state.selectedFilter,
                sort = sort
            )
            state.copy(
                selectedSort = sort,
                displayedApps = filtered,
                hasUsageAccess = hasUsage,
                showUsageExplainer = showExplainer
            )
        }
    }

    fun dismissUsageExplainer() {
        _uiState.update { it.copy(showUsageExplainer = false) }
    }

    fun refreshUsagePermission() {
        val hasUsage = repository.hasUsageAccessPermission()
        if (hasUsage != _uiState.value.hasUsageAccess) {
            loadInstalledApps()
        }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val current = state.selectedPackageNames
            val updated = if (current.contains(packageName)) {
                current - packageName
            } else {
                current + packageName
            }
            state.copy(selectedPackageNames = updated)
        }
    }

    fun selectAllVisible() {
        _uiState.update { state ->
            val allVisible = state.displayedApps.map { it.packageName }.toSet()
            state.copy(selectedPackageNames = allVisible)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPackageNames = emptySet()) }
    }

    fun selectAppForDetails(app: AppInfoModel) {
        _uiState.update { it.copy(selectedAppForDetails = app) }
    }

    fun dismissAppDetails() {
        _uiState.update { it.copy(selectedAppForDetails = null) }
    }

    private fun applyFilterAndSort(
        apps: List<AppInfoModel>,
        query: String,
        filter: AppFilterType,
        sort: AppSortType
    ): List<AppInfoModel> {
        val filtered = AppRepository.filterApps(apps, query, filter)
        return AppRepository.sortApps(filtered, sort)
    }
}
