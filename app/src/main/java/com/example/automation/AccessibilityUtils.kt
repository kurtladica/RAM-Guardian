package com.example.automation

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object AccessibilityUtils {

    const val SERVICE_CLASS_NAME = "com.example.automation.GuardianAccessibilityService"

    /**
     * Checks whether the user has toggled the RAM Guardian Accessibility Service ON in Android Settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedComponentName = "${context.packageName}/$SERVICE_CLASS_NAME"

        for (service in enabledServices) {
            val id = service.id
            if (id != null && (id == expectedComponentName || id.contains(SERVICE_CLASS_NAME))) {
                return true
            }
        }

        // Secondary check via Settings.Secure
        try {
            val enabledSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledSetting.contains(SERVICE_CLASS_NAME)
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Checks if the service instance is currently active and connected to the OS.
     */
    fun isServiceConnected(): Boolean {
        return GuardianAccessibilityService.isConnected.value
    }

    /**
     * Opens Android's system Accessibility Settings page so the user can review and enable the service.
     */
    fun openAccessibilitySettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Opens Android's native App Info settings page for a specific package.
     */
    fun openAppInfoSettings(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
