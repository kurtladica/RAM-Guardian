package com.example.data

import com.example.data.apps.AppInfoModel
import com.example.data.apps.AppWarningClassifier
import com.example.data.apps.WarningSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppWarningClassifierTest {

    @Test
    fun `critical system component triggers critical warning`() {
        val systemApp = AppInfoModel(
            packageName = "com.android.systemui",
            name = "System UI",
            isSystemApp = true,
            isUpdatedSystemApp = false,
            versionName = "14",
            versionCode = 1,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 0L,
            lastUpdateTimeMillis = 0L,
            categoryName = "System",
            hasLaunchIntent = false
        )

        val warning = AppWarningClassifier.classify(systemApp)
        assertNotNull(warning)
        assertEquals(WarningSeverity.CRITICAL, warning?.severity)
    }

    @Test
    fun `communication app triggers messaging notification warning`() {
        val messagingApp = AppInfoModel(
            packageName = "org.thoughtcrime.securesms",
            name = "Signal",
            isSystemApp = false,
            isUpdatedSystemApp = false,
            versionName = "7.0",
            versionCode = 1,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 1000L,
            lastUpdateTimeMillis = 1000L,
            categoryName = "Social & Communication",
            hasLaunchIntent = true
        )

        val warning = AppWarningClassifier.classify(messagingApp)
        assertNotNull(warning)
        assertEquals("Messaging & Notifications", warning?.title)
    }

    @Test
    fun `launcher app triggers home screen warning`() {
        val launcherApp = AppInfoModel(
            packageName = "com.google.android.apps.nexuslauncher",
            name = "Pixel Launcher",
            isSystemApp = true,
            isUpdatedSystemApp = true,
            versionName = "14",
            versionCode = 1,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 1000L,
            lastUpdateTimeMillis = 1000L,
            categoryName = "Productivity & Tools",
            hasLaunchIntent = true
        )

        val warning = AppWarningClassifier.classify(launcherApp)
        assertNotNull(warning)
        assertEquals("Home Screen Launcher", warning?.title)
    }

    @Test
    fun `normal tool app without sensitive category triggers no warning`() {
        val calculatorApp = AppInfoModel(
            packageName = "com.example.calculator",
            name = "Calculator",
            isSystemApp = false,
            isUpdatedSystemApp = false,
            versionName = "1.0",
            versionCode = 1,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 1000L,
            lastUpdateTimeMillis = 1000L,
            categoryName = "Tools & Utility",
            hasLaunchIntent = true
        )

        val warning = AppWarningClassifier.classify(calculatorApp)
        assertNull(warning)
    }
}
