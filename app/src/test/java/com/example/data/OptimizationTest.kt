package com.example.data

import com.example.data.memory.MemoryInfoData
import com.example.ui.screens.optimize.MemoryComparisonData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizationTest {

    @Test
    fun `memory comparison delta computes freed memory with negative sign`() {
        val baseline = MemoryInfoData(
            totalBytes = 8L * 1024L * 1024L * 1024L,
            availBytes = 2L * 1024L * 1024L * 1024L, // 6 GB used (75%)
            isLowMemory = false,
            lowMemThresholdBytes = 400L * 1024L * 1024L
        )

        val current = MemoryInfoData(
            totalBytes = 8L * 1024L * 1024L * 1024L,
            availBytes = 3L * 1024L * 1024L * 1024L, // 5 GB used (62.5%) -> 1 GB freed
            isLowMemory = false,
            lowMemThresholdBytes = 400L * 1024L * 1024L
        )

        val usedDelta = current.usedBytes - baseline.usedBytes // -1 GB
        val percentDelta = current.usagePercentage - baseline.usagePercentage // -12.5%

        val comparison = MemoryComparisonData(
            baseline = baseline,
            current = current,
            usedBytesDelta = usedDelta,
            percentageDelta = percentDelta
        )

        assertEquals(-1L * 1024L * 1024L * 1024L, comparison.usedBytesDelta)
        val formatted = comparison.formattedDelta()
        assertTrue(formatted.startsWith("-1.00 GB"))
        assertTrue(formatted.contains("-12.5%"))
    }
}
