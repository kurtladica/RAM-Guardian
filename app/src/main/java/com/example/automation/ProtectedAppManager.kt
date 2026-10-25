package com.example.automation

import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import com.example.data.settings.SettingsRepository

data class ProtectedPackageInfo(
    val packageName: String,
    val name: String,
    val reason: String
)

object ProtectedAppManager {

    private val HARD_PROTECTED_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.googlequicksearchbox",
        "com.google.android.gms",
        "com.android.phone",
        "com.google.android.dialer",
        "com.android.server.telecom",
        "com.android.emergency"
    )

    /**
     * Resolves currently active default Home Launcher package name.
     */
    fun getActiveLauncherPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * Resolves all currently enabled keyboard / input method packages.
     */
    fun getEnabledInputMethodPackages(context: Context): Set<String> {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return emptySet()
        return try {
            imm.enabledInputMethodList.map { it.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Evaluates whether a package is protected from force-stop automation.
     */
    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (HARD_PROTECTED_PACKAGES.contains(packageName)) return true
        if (packageName == getActiveLauncherPackage(context)) return true
        if (getEnabledInputMethodPackages(context).contains(packageName)) return true

        // User custom whitelist check
        val settingsRepo = SettingsRepository.getInstance(context)
        if (settingsRepo.userWhitelist.value.contains(packageName)) return true

        return false
    }

    /**
     * Explains why an application is protected from automation.
     */
    fun getProtectionReason(context: Context, packageName: String): String? {
        if (packageName == context.packageName) {
            return "RAM Guardian is currently running and cannot stop itself."
        }
        if (packageName == getActiveLauncherPackage(context)) {
            return "Active Home Launcher provides your home screen and app drawer."
        }
        if (getEnabledInputMethodPackages(context).contains(packageName)) {
            return "Active Keyboard / Input Method is required for text input."
        }
        if (packageName == "com.android.systemui") {
            return "System UI controls status bar, navigation bar, and notifications."
        }
        if (packageName == "com.android.settings") {
            return "Android Settings is required for system navigation and controls."
        }
        if (packageName in setOf("com.android.phone", "com.google.android.dialer", "com.android.server.telecom", "com.android.emergency")) {
            return "Critical telephony component required for emergency and voice calls."
        }
        if (packageName == "com.google.android.gms") {
            return "Google Play Services manages device integrity and system APIs."
        }

        val settingsRepo = SettingsRepository.getInstance(context)
        if (settingsRepo.userWhitelist.value.contains(packageName)) {
            return "Excluded by your custom whitelist in Settings."
        }

        return null
    }

    /**
     * Returns a curated list of system-protected packages detected on this device.
     */
    fun getSystemProtectedPackages(context: Context): List<ProtectedPackageInfo> {
        val list = mutableListOf<ProtectedPackageInfo>()

        list.add(ProtectedPackageInfo(context.packageName, "RAM Guardian", "Application utility itself"))

        val launcher = getActiveLauncherPackage(context)
        if (launcher != null) {
            list.add(ProtectedPackageInfo(launcher, "Home Launcher", "Active home screen environment"))
        }

        getEnabledInputMethodPackages(context).forEach { pkg ->
            list.add(ProtectedPackageInfo(pkg, "Active Keyboard", "Input method editor"))
        }

        list.add(ProtectedPackageInfo("com.android.systemui", "System UI", "Status bar & navigation"))
        list.add(ProtectedPackageInfo("com.android.settings", "Android Settings", "System preferences"))
        list.add(ProtectedPackageInfo("com.google.android.gms", "Play Services", "Core system runtime"))

        return list.distinctBy { it.packageName }
    }
}
