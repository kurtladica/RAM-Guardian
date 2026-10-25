package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.automation.AccessibilityUtils
import com.example.automation.BatchAutomationEngine
import com.example.automation.BatchItemStatus
import com.example.automation.ProtectedAppManager
import com.example.data.apps.AppInfoModel
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase7AutomationTest {

    private lateinit var context: Context
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepo = SettingsRepository.getInstance(context)
        settingsRepo.resetToDefaults()
        BatchAutomationEngine.stopBatch()
    }

    @After
    fun tearDown() {
        BatchAutomationEngine.stopBatch()
    }

    @Test
    fun test_ram_guardian_is_always_protected() {
        val isSelfProtected = ProtectedAppManager.isProtected(context, context.packageName)
        assertTrue("RAM Guardian must always protect itself from being force-stopped", isSelfProtected)
        val reason = ProtectedAppManager.getProtectionReason(context, context.packageName)
        assertNotNull("Protection reason should be provided for RAM Guardian", reason)
    }

    @Test
    fun test_system_critical_apps_are_protected() {
        assertTrue(ProtectedAppManager.isProtected(context, "com.android.systemui"))
        assertTrue(ProtectedAppManager.isProtected(context, "com.android.settings"))
        assertTrue(ProtectedAppManager.isProtected(context, "com.google.android.gms"))
        assertTrue(ProtectedAppManager.isProtected(context, "com.android.phone"))
    }

    @Test
    fun test_user_whitelist_protects_custom_app() {
        val sampleApp = "com.spotify.music"
        assertFalse("Sample app should initially not be protected", ProtectedAppManager.isProtected(context, sampleApp))

        settingsRepo.addToWhitelist(sampleApp)
        assertTrue("Whitelisted app must be protected", ProtectedAppManager.isProtected(context, sampleApp))

        val reason = ProtectedAppManager.getProtectionReason(context, sampleApp)
        assertTrue("Reason should indicate custom whitelist", reason?.contains("whitelist") == true)

        settingsRepo.removeFromWhitelist(sampleApp)
        assertFalse("App removed from whitelist should no longer be protected", ProtectedAppManager.isProtected(context, sampleApp))
    }

    @Test
    fun test_normal_user_app_is_not_protected() {
        val normalUserApp = "com.rovio.angrybirds"
        assertFalse(ProtectedAppManager.isProtected(context, normalUserApp))
    }

    @Test
    fun test_batch_execution_on_protected_app_is_skipped() = runBlocking {
        val protectedApp = AppInfoModel(
            packageName = "com.android.systemui",
            name = "System UI",
            isSystemApp = true,
            isUpdatedSystemApp = false,
            versionName = "1.0",
            versionCode = 1L,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 0L,
            lastUpdateTimeMillis = 0L,
            categoryName = "System",
            hasLaunchIntent = false
        )

        val completed = CompletableDeferred<Unit>()

        // Starting batch should mark protected app as SKIPPED, never SUCCESS
        BatchAutomationEngine.startBatch(
            context = context,
            targetApps = listOf(protectedApp),
            delayBetweenAppsMillis = 10L,
            onComplete = { completed.complete(Unit) }
        )

        completed.await()

        val progress = BatchAutomationEngine.progress.value
        val itemResult = progress.items.firstOrNull { it.app.packageName == "com.android.systemui" }
        assertNotNull(itemResult)
        assertEquals(BatchItemStatus.SKIPPED, itemResult?.status)
        assertTrue(itemResult?.message?.contains("Protected") == true)
        assertEquals(0, progress.successCount)
    }

    @Test
    fun test_batch_cancellation_halts_processing() = runBlocking {
        val app1 = AppInfoModel(
            packageName = "com.example.dummy1",
            name = "Dummy 1",
            isSystemApp = false,
            isUpdatedSystemApp = false,
            versionName = "1.0",
            versionCode = 1L,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 1000L,
            lastUpdateTimeMillis = 1000L,
            categoryName = "Game",
            hasLaunchIntent = true
        )
        val app2 = AppInfoModel(
            packageName = "com.example.dummy2",
            name = "Dummy 2",
            isSystemApp = false,
            isUpdatedSystemApp = false,
            versionName = "1.0",
            versionCode = 1L,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 1000L,
            lastUpdateTimeMillis = 1000L,
            categoryName = "Game",
            hasLaunchIntent = true
        )

        BatchAutomationEngine.startBatch(
            context = context,
            targetApps = listOf(app1, app2),
            delayBetweenAppsMillis = 2000L
        )

        // Stop immediately
        BatchAutomationEngine.stopBatch()

        assertFalse(BatchAutomationEngine.isBatchActive.value)
        val progress = BatchAutomationEngine.progress.value
        assertFalse(progress.isRunning)
        assertTrue(progress.isFinished)
    }

    @Test
    fun test_service_disabled_detection() {
        val isEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
        // In Robolectric test environment without user interaction, service is not enabled
        assertFalse(isEnabled)
    }
}
