package com.example.data.apps

import android.content.pm.ApplicationInfo
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppInfoModel(
    val packageName: String,
    val name: String,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val installTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val categoryName: String,
    val hasLaunchIntent: Boolean,
    val lastTimeUsedMillis: Long? = null,
    val foregroundTimeMillis24h: Long? = null
) {
    val appTypeLabel: String
        get() = when {
            isUpdatedSystemApp -> "System (Updated)"
            isSystemApp -> "System"
            else -> "User Installed"
        }

    fun formattedInstallDate(): String {
        return if (installTimeMillis > 0L) {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(installTimeMillis))
        } else {
            "Preinstalled"
        }
    }

    fun formattedUpdateDate(): String {
        return if (lastUpdateTimeMillis > 0L) {
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(lastUpdateTimeMillis))
        } else {
            "Unknown"
        }
    }

    fun formattedLastUsed(): String {
        return lastTimeUsedMillis?.let {
            if (it > 0L) {
                SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(it))
            } else {
                "Not recently used"
            }
        } ?: "Usage access required"
    }

    fun formattedForegroundTime(): String {
        return foregroundTimeMillis24h?.let { ms ->
            val minutes = ms / (1000 * 60)
            val hours = minutes / 60
            val remMinutes = minutes % 60
            if (hours > 0) "${hours}h ${remMinutes}m active today" else "${remMinutes}m active today"
        } ?: "Usage access required"
    }

    companion object {
        fun resolveCategory(category: Int): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return when (category) {
                    ApplicationInfo.CATEGORY_GAME -> "Games"
                    ApplicationInfo.CATEGORY_AUDIO -> "Audio & Music"
                    ApplicationInfo.CATEGORY_VIDEO -> "Video & Movies"
                    ApplicationInfo.CATEGORY_IMAGE -> "Photography"
                    ApplicationInfo.CATEGORY_SOCIAL -> "Social & Communication"
                    ApplicationInfo.CATEGORY_NEWS -> "News & Magazines"
                    ApplicationInfo.CATEGORY_MAPS -> "Maps & Navigation"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity & Tools"
                    ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Accessibility"
                    else -> "Tools & Utility"
                }
            }
            return "Application"
        }
    }
}
