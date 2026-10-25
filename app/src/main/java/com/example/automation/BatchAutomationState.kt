package com.example.automation

import com.example.data.apps.AppInfoModel

enum class BatchItemStatus(val label: String) {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SUCCESS("Force Stopped"),
    SKIPPED("Skipped"),
    FAILED("Failed"),
    UNVERIFIED("Unverified")
}

data class BatchItemResult(
    val app: AppInfoModel,
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val message: String = ""
)

data class BatchProgress(
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentApp: AppInfoModel? = null,
    val processedCount: Int = 0,
    val successCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val unverifiedCount: Int = 0,
    val items: List<BatchItemResult> = emptyList(),
    val summary: String = ""
) {
    val progressFraction: Float
        get() = if (totalCount > 0) currentIndex.toFloat() / totalCount.toFloat() else 0f
}
