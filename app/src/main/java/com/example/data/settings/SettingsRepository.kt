package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System Default")
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _refreshIntervalMillis = MutableStateFlow(loadRefreshInterval())
    val refreshIntervalMillis: StateFlow<Long> = _refreshIntervalMillis.asStateFlow()

    private val _historyClearTrigger = MutableStateFlow(0L)
    val historyClearTrigger: StateFlow<Long> = _historyClearTrigger.asStateFlow()

    private val _automationDelayMillis = MutableStateFlow(loadAutomationDelay())
    val automationDelayMillis: StateFlow<Long> = _automationDelayMillis.asStateFlow()

    private val _userWhitelist = MutableStateFlow(loadUserWhitelist())
    val userWhitelist: StateFlow<Set<String>> = _userWhitelist.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setRefreshInterval(millis: Long) {
        val validMillis = when (millis) {
            1000L, 2000L, 5000L -> millis
            else -> DEFAULT_INTERVAL
        }
        prefs.edit().putLong(KEY_REFRESH_INTERVAL, validMillis).apply()
        _refreshIntervalMillis.value = validMillis
    }

    fun setAutomationDelay(millis: Long) {
        val validMillis = when (millis) {
            1000L, 1500L, 2000L, 3000L -> millis
            else -> DEFAULT_AUTOMATION_DELAY
        }
        prefs.edit().putLong(KEY_AUTOMATION_DELAY, validMillis).apply()
        _automationDelayMillis.value = validMillis
    }

    fun addToWhitelist(packageName: String) {
        val updated = _userWhitelist.value + packageName
        prefs.edit().putStringSet(KEY_USER_WHITELIST, updated).apply()
        _userWhitelist.value = updated
    }

    fun removeFromWhitelist(packageName: String) {
        val updated = _userWhitelist.value - packageName
        prefs.edit().putStringSet(KEY_USER_WHITELIST, updated).apply()
        _userWhitelist.value = updated
    }

    fun clearLocalHistory() {
        _historyClearTrigger.value = System.currentTimeMillis()
    }

    fun resetToDefaults() {
        prefs.edit()
            .remove(KEY_THEME_MODE)
            .remove(KEY_REFRESH_INTERVAL)
            .remove(KEY_AUTOMATION_DELAY)
            .remove(KEY_USER_WHITELIST)
            .apply()

        _themeMode.value = AppThemeMode.DARK
        _refreshIntervalMillis.value = DEFAULT_INTERVAL
        _automationDelayMillis.value = DEFAULT_AUTOMATION_DELAY
        _userWhitelist.value = emptySet()
        _historyClearTrigger.value = System.currentTimeMillis()
    }

    private fun loadThemeMode(): AppThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name)
        return try {
            AppThemeMode.valueOf(raw ?: AppThemeMode.DARK.name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    private fun loadRefreshInterval(): Long {
        val interval = prefs.getLong(KEY_REFRESH_INTERVAL, DEFAULT_INTERVAL)
        return if (interval in listOf(1000L, 2000L, 5000L)) interval else DEFAULT_INTERVAL
    }

    private fun loadAutomationDelay(): Long {
        val delay = prefs.getLong(KEY_AUTOMATION_DELAY, DEFAULT_AUTOMATION_DELAY)
        return if (delay in listOf(1000L, 1500L, 2000L, 3000L)) delay else DEFAULT_AUTOMATION_DELAY
    }

    private fun loadUserWhitelist(): Set<String> {
        return prefs.getStringSet(KEY_USER_WHITELIST, emptySet())?.toSet() ?: emptySet()
    }

    companion object {
        private const val PREFS_NAME = "ram_guardian_settings"
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_REFRESH_INTERVAL = "dashboard_refresh_interval_ms"
        private const val KEY_AUTOMATION_DELAY = "automation_delay_ms"
        private const val KEY_USER_WHITELIST = "automation_user_whitelist"
        const val DEFAULT_INTERVAL = 2000L
        const val DEFAULT_AUTOMATION_DELAY = 1500L

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
