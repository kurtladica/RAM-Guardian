package com.example.data.apps

enum class WarningSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class AppWarning(
    val severity: WarningSeverity,
    val title: String,
    val message: String
)

object AppWarningClassifier {

    fun classify(app: AppInfoModel): AppWarning? {
        val pkg = app.packageName.lowercase()
        val category = app.categoryName.lowercase()

        // 1. Critical System Components
        if (app.isSystemApp && !app.isUpdatedSystemApp &&
            (pkg.contains("systemui") || pkg.contains(".android.settings") ||
                pkg == "android" || pkg.contains("google.android.gms") ||
                pkg.contains("qualcomm") || pkg.contains("telephony"))
        ) {
            return AppWarning(
                severity = WarningSeverity.CRITICAL,
                title = "Protected System Component",
                message = "Android typically disables the Force stop button for core OS packages. Force-stopping system components can destabilize the operating system."
            )
        }

        // 2. Launchers & Home Screens
        if (pkg.contains("launcher") || pkg.contains("home") || pkg.contains("trebuchet") || pkg.contains("nexuslauncher")) {
            return AppWarning(
                severity = WarningSeverity.WARNING,
                title = "Home Screen Launcher",
                message = "Stopping your active home launcher will cause your wallpaper, widgets, and recent tasks to immediately crash and reload."
            )
        }

        // 3. Accessibility Services
        if (category.contains("accessibility") || pkg.contains("accessibility") || pkg.contains("talkback")) {
            return AppWarning(
                severity = WarningSeverity.WARNING,
                title = "Accessibility Provider",
                message = "Force-stopping this application may deactivate screen readers, gesture shortcuts, or assistive interaction tools."
            )
        }

        // 4. Communication / Messaging Apps
        if (category.contains("social") || category.contains("communication") ||
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
            pkg.contains("messenger") || pkg.contains("discord") || pkg.contains("viber") ||
            pkg.contains("dialer") || pkg.contains("contacts") || pkg.contains("mms") || pkg.contains("sms")
        ) {
            return AppWarning(
                severity = WarningSeverity.INFO,
                title = "Messaging & Notifications",
                message = "Force-stopping this app will terminate background push listeners. You will NOT receive incoming messages or call alerts until you manually reopen it."
            )
        }

        // 5. Navigation & GPS Guidance
        if (category.contains("maps") || category.contains("navigation") ||
            pkg.contains("maps") || pkg.contains("waze") || pkg.contains("here") || pkg.contains("gps")
        ) {
            return AppWarning(
                severity = WarningSeverity.INFO,
                title = "Navigation & Location Guidance",
                message = "Force-stopping will cancel active GPS directions and turn-by-turn background audio prompts."
            )
        }

        // 6. Media / Music Players
        if (category.contains("audio") || category.contains("music") ||
            pkg.contains("spotify") || pkg.contains("music") || pkg.contains("audio") ||
            pkg.contains("podcast") || pkg.contains("deezer") || pkg.contains("soundcloud")
        ) {
            return AppWarning(
                severity = WarningSeverity.INFO,
                title = "Audio Playback",
                message = "Force-stopping this media player will immediately terminate any active background audio streaming or playback."
            )
        }

        // 7. General System App Notice
        if (app.isSystemApp && !app.isUpdatedSystemApp) {
            return AppWarning(
                severity = WarningSeverity.INFO,
                title = "Preinstalled System App",
                message = "Android or your device manufacturer may grey out or disable the Force stop button for this package."
            )
        }

        return null
    }
}
