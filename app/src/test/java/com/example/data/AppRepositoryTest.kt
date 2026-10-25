package com.example.data

import com.example.data.apps.AppFilterType
import com.example.data.apps.AppInfoModel
import com.example.data.apps.AppRepository
import com.example.data.apps.AppSortType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRepositoryTest {

    private val sampleApps = listOf(
        AppInfoModel(
            packageName = "com.google.android.youtube",
            name = "YouTube",
            isSystemApp = true,
            isUpdatedSystemApp = true,
            versionName = "19.01",
            versionCode = 100,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 1000L,
            lastUpdateTimeMillis = 5000L,
            categoryName = "Video & Movies",
            hasLaunchIntent = true,
            lastTimeUsedMillis = 9000L,
            foregroundTimeMillis24h = 3600000L
        ),
        AppInfoModel(
            packageName = "com.example.calculator",
            name = "Calculator",
            isSystemApp = false,
            isUpdatedSystemApp = false,
            versionName = "1.0",
            versionCode = 1,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 2000L,
            lastUpdateTimeMillis = 2000L,
            categoryName = "Tools & Utility",
            hasLaunchIntent = true,
            lastTimeUsedMillis = 1000L,
            foregroundTimeMillis24h = 60000L
        ),
        AppInfoModel(
            packageName = "com.android.systemui",
            name = "System UI",
            isSystemApp = true,
            isUpdatedSystemApp = false,
            versionName = "14",
            versionCode = 34,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            installTimeMillis = 500L,
            lastUpdateTimeMillis = 500L,
            categoryName = "System",
            hasLaunchIntent = false,
            lastTimeUsedMillis = null,
            foregroundTimeMillis24h = null
        )
    )

    @Test
    fun `filterApps by User filter includes user apps and updated system apps`() {
        val userApps = AppRepository.filterApps(sampleApps, "", AppFilterType.USER)
        assertEquals(2, userApps.size)
        assertTrue(userApps.any { it.name == "Calculator" })
        assertTrue(userApps.any { it.name == "YouTube" })
    }

    @Test
    fun `filterApps by System filter excludes pure user apps`() {
        val systemApps = AppRepository.filterApps(sampleApps, "", AppFilterType.SYSTEM)
        assertEquals(1, systemApps.size)
        assertEquals("System UI", systemApps.first().name)
    }

    @Test
    fun `filterApps by search query matches name and package case-insensitively`() {
        val matchName = AppRepository.filterApps(sampleApps, "calc", AppFilterType.ALL)
        assertEquals(1, matchName.size)
        assertEquals("Calculator", matchName.first().name)

        val matchPackage = AppRepository.filterApps(sampleApps, "youtube", AppFilterType.ALL)
        assertEquals(1, matchPackage.size)
        assertEquals("YouTube", matchPackage.first().name)
    }

    @Test
    fun `sortApps by Name A-Z orders correctly`() {
        val sorted = AppRepository.sortApps(sampleApps, AppSortType.NAME_ASC)
        assertEquals("Calculator", sorted[0].name)
        assertEquals("System UI", sorted[1].name)
        assertEquals("YouTube", sorted[2].name)
    }

    @Test
    fun `sortApps by Recently Used puts most recent foreground app first`() {
        val sorted = AppRepository.sortApps(sampleApps, AppSortType.RECENTLY_USED)
        assertEquals("YouTube", sorted.first().name)
    }
}
