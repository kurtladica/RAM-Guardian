package com.example.data

import com.example.data.memory.MemoryInfoData
import com.example.data.memory.MemoryPressureLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryInfoDataTest {

    @Test
    fun `calculate used bytes and percentage correctly`() {
        val totalBytes = 8L * 1024L * 1024L * 1024L // 8 GB
        val availBytes = 2L * 1024L * 1024L * 1024L // 2 GB
        val data = MemoryInfoData(
            totalBytes = totalBytes,
            availBytes = availBytes,
            isLowMemory = false,
            lowMemThresholdBytes = 500L * 1024L * 1024L
        )

        assertEquals(6L * 1024L * 1024L * 1024L, data.usedBytes)
        assertEquals(75.0f, data.usagePercentage, 0.01f)
        assertEquals(MemoryPressureLevel.MODERATE, data.pressureLevel)
    }

    @Test
    fun `format bytes in GB and MB accurately`() {
        val bytes8GB = (8L * 1024L * 1024L * 1024L)
        val bytes512MB = (512L * 1024L * 1024L)

        assertEquals("8.00 GB", MemoryInfoData.formatBytes(bytes8GB))
        assertEquals("512.0 MB", MemoryInfoData.formatBytes(bytes512MB))
    }

    @Test
    fun `high pressure is triggered when isLowMemory is true or usage is 88 percent or higher`() {
        val totalBytes = 10L * 1024L * 1024L * 1024L
        val availBytes = 1L * 1024L * 1024L * 1024L // 90% used

        val data90Percent = MemoryInfoData(
            totalBytes = totalBytes,
            availBytes = availBytes,
            isLowMemory = false,
            lowMemThresholdBytes = 400L * 1024L * 1024L
        )
        assertEquals(MemoryPressureLevel.HIGH, data90Percent.pressureLevel)

        val dataLowMemFlag = MemoryInfoData(
            totalBytes = totalBytes,
            availBytes = 5L * 1024L * 1024L * 1024L, // 50% used but system flagged lowMemory
            isLowMemory = true,
            lowMemThresholdBytes = 400L * 1024L * 1024L
        )
        assertEquals(MemoryPressureLevel.HIGH, dataLowMemFlag.pressureLevel)
    }

    @Test
    fun `boundary check with zero total bytes does not throw or NaN`() {
        val zeroData = MemoryInfoData(
            totalBytes = 0L,
            availBytes = 0L,
            isLowMemory = false,
            lowMemThresholdBytes = 0L
        )

        assertEquals(0L, zeroData.usedBytes)
        assertEquals(0f, zeroData.usagePercentage, 0.01f)
        assertEquals(MemoryPressureLevel.NORMAL, zeroData.pressureLevel)
    }
}
