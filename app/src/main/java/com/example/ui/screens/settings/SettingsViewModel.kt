package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.apps.AppRepository
import com.example.data.settings.AppThemeMode
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.automation.AccessibilityUtils
import com.example.automation.GuardianAccessibilityService

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val refreshIntervalMillis: Long = SettingsRepository.DEFAULT_INTERVAL,
    val automationDelayMillis: Long = SettingsRepository.DEFAULT_AUTOMATION_DELAY,
    val userWhitelist: Set<String> = emptySet(),
    val hasUsageAccess: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isAccessibilityConnected: Boolean = false,
    val showClearHistoryDialog: Boolean = false,
    val showResetDefaultsDialog: Boolean = false,
    val showConsentDialog: Boolean = false,
    val showProtectedAppsDialog: Boolean = false,
    val actionFeedbackMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val appRepository = AppRepository(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = settingsRepository.themeMode.value,
            refreshIntervalMillis = settingsRepository.refreshIntervalMillis.value,
            automationDelayMillis = settingsRepository.automationDelayMillis.value,
            userWhitelist = settingsRepository.userWhitelist.value,
            hasUsageAccess = appRepository.hasUsageAccessPermission(),
            isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(application),
            isAccessibilityConnected = AccessibilityUtils.isServiceConnected()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.refreshIntervalMillis.collect { interval ->
                _uiState.update { it.copy(refreshIntervalMillis = interval) }
            }
        }
        viewModelScope.launch {
            settingsRepository.automationDelayMillis.collect { delay ->
                _uiState.update { it.copy(automationDelayMillis = delay) }
            }
        }
        viewModelScope.launch {
            settingsRepository.userWhitelist.collect { whitelist ->
                _uiState.update { it.copy(userWhitelist = whitelist) }
            }
        }
        viewModelScope.launch {
            GuardianAccessibilityService.isConnected.collect { connected ->
                _uiState.update {
                    it.copy(
                        isAccessibilityConnected = connected,
                        isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
                    )
                }
            }
        }
    }

    fun refreshState() {
        val hasAccess = appRepository.hasUsageAccessPermission()
        val isA11yEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
        val isA11yConnected = AccessibilityUtils.isServiceConnected()
        _uiState.update {
            it.copy(
                hasUsageAccess = hasAccess,
                isAccessibilityEnabled = isA11yEnabled,
                isAccessibilityConnected = isA11yConnected
            )
        }
    }

    fun refreshUsagePermissionState() {
        refreshState()
    }

    fun setAutomationDelay(millis: Long) {
        settingsRepository.setAutomationDelay(millis)
    }

    fun removeFromWhitelist(packageName: String) {
        settingsRepository.removeFromWhitelist(packageName)
    }

    fun showConsentDialog() {
        _uiState.update { it.copy(showConsentDialog = true) }
    }

    fun dismissConsentDialog() {
        _uiState.update { it.copy(showConsentDialog = false) }
    }

    fun showProtectedAppsDialog() {
        _uiState.update { it.copy(showProtectedAppsDialog = true) }
    }

    fun dismissProtectedAppsDialog() {
        _uiState.update { it.copy(showProtectedAppsDialog = false) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setRefreshInterval(millis: Long) {
        settingsRepository.setRefreshInterval(millis)
    }

    fun promptClearHistory() {
        _uiState.update { it.copy(showClearHistoryDialog = true) }
    }

    fun dismissClearHistory() {
        _uiState.update { it.copy(showClearHistoryDialog = false) }
    }

    fun confirmClearHistory() {
        settingsRepository.clearLocalHistory()
        _uiState.update {
            it.copy(
                showClearHistoryDialog = false,
                actionFeedbackMessage = "RAM measurement graph history cleared."
            )
        }
    }

    fun promptResetDefaults() {
        _uiState.update { it.copy(showResetDefaultsDialog = true) }
    }

    fun dismissResetDefaults() {
        _uiState.update { it.copy(showResetDefaultsDialog = false) }
    }

    fun confirmResetDefaults() {
        settingsRepository.resetToDefaults()
        _uiState.update {
            it.copy(
                showResetDefaultsDialog = false,
                actionFeedbackMessage = "All preferences reset to factory defaults."
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(actionFeedbackMessage = null) }
    }
}
