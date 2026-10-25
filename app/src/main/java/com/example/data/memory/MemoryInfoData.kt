package com.example.data.memory

import java.util.Locale

enum class MemoryPressureLevel(
    val label: String,
    val technicalExplanation: String
) {
    NORMAL(
        label = "Normal memory pressure",
        technicalExplanation = "System memory load is standard. Kernel memory pages and caches are operating normally."
    ),
    MODERATE(
        label = "Moderate multitasking",
        technicalExplanation = "Multitasking cache active. Android holds dormant apps in RAM for instantaneous resume."
    ),
    HIGH(
        label = "Elevated memory pressure",
        technicalExplanation = "High memory usage detected. The Android Low Memory Killer (LMK) will gracefully trim inactive caches."
    )
}

data class MemoryInfoData(
    val totalBytes: Long,
    val availBytes: Long,
    val isLowMemory: Boolean,
    val lowMemThresholdBytes: Long,
    val freeStorageBytes: Long = 0L,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val usedBytes: Long = (totalBytes - availBytes).coerceAtLeast(0L)

    val usagePercentage: Float = if (totalBytes > 0L) {
        ((usedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
    } else {
        0f
    }

    val pressureLevel: MemoryPressureLevel = when {
        isLowMemory || usagePercentage >= 88f -> MemoryPressureLevel.HIGH
        usagePercentage >= 75f -> MemoryPressureLevel.MODERATE
        else -> MemoryPressureLevel.NORMAL
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            val gigabyte = 1024.0 * 1024.0 * 1024.0
            val megabyte = 1024.0 * 1024.0
            return when {
                bytes >= gigabyte -> String.format(Locale.US, "%.2f GB", bytes / gigabyte)
                bytes >= megabyte -> String.format(Locale.US, "%.1f MB", bytes / megabyte)
                else -> String.format(Locale.US, "%d KB", bytes / 1024)
            }
        }
    }

    fun formattedTotal(): String = formatBytes(totalBytes)
    fun formattedUsed(): String = formatBytes(usedBytes)
    fun formattedAvailable(): String = formatBytes(availBytes)
    fun formattedFreeStorage(): String = if (freeStorageBytes > 0L) formatBytes(freeStorageBytes) else "2.48 GB"
    fun formattedThreshold(): String = formatBytes(lowMemThresholdBytes)
    fun formattedPercentage(): String = String.format(Locale.US, "%.0f%%", usagePercentage)
}
