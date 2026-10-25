package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.data.apps.AppInfoModel
import com.example.data.memory.MemoryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BatchAutomationEngine {

    private const val TAG = "BatchAutomationEngine"

    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentJob: Job? = null

    private val _isBatchActive = MutableStateFlow(false)
    val isBatchActive: StateFlow<Boolean> = _isBatchActive.asStateFlow()

    private val _progress = MutableStateFlow(BatchProgress())
    val progress: StateFlow<BatchProgress> = _progress.asStateFlow()

    private val _windowUpdateSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val windowUpdateSignal: SharedFlow<Unit> = _windowUpdateSignal.asSharedFlow()

    fun notifyWindowUpdated() {
        _windowUpdateSignal.tryEmit(Unit)
    }

    /**
     * Explicitly starts a user-triggered batch force-stop operation.
     */
    fun startBatch(
        context: Context,
        targetApps: List<AppInfoModel>,
        delayBetweenAppsMillis: Long = 1500L,
        onComplete: () -> Unit = {}
    ) {
        if (_isBatchActive.value) {
            Log.w(TAG, "Batch already running; ignoring new request.")
            return
        }

        currentJob?.cancel()

        _isBatchActive.value = true
        _progress.value = BatchProgress(
            isRunning = true,
            isFinished = false,
            currentIndex = 0,
            totalCount = targetApps.size,
            currentApp = targetApps.firstOrNull(),
            processedCount = 0,
            successCount = 0,
            skippedCount = 0,
            failedCount = 0,
            unverifiedCount = 0,
            items = targetApps.map { BatchItemResult(app = it, status = BatchItemStatus.PENDING) },
            summary = "Starting batch operation..."
        )

        currentJob = scope.launch {
            try {
                executeBatchLoop(context.applicationContext, targetApps, delayBetweenAppsMillis)
            } finally {
                withContext(Dispatchers.Main) {
                    _isBatchActive.value = false
                    _progress.update { it.copy(isRunning = false, isFinished = true) }
                    // Refresh hardware memory measurements after batch completes
                    try {
                        MemoryMonitor(context).getMemorySnapshot()
                    } catch (_: Exception) {}
                    onComplete()
                }
            }
        }
    }

    /**
     * Halts an active batch operation immediately.
     */
    fun stopBatch() {
        if (!_isBatchActive.value) return
        Log.i(TAG, "User requested to halt batch operation immediately.")
        currentJob?.cancel()
        currentJob = null
        _isBatchActive.value = false
        _progress.update { state ->
            val remainingUpdated = state.items.map { item ->
                if (item.status == BatchItemStatus.PENDING || item.status == BatchItemStatus.PROCESSING) {
                    item.copy(status = BatchItemStatus.SKIPPED, message = "Cancelled by user")
                } else {
                    item
                }
            }
            state.copy(
                isRunning = false,
                isFinished = true,
                items = remainingUpdated,
                summary = "Batch stopped by user."
            )
        }
    }

    /**
     * Deterministic execution loop processing one application at a time.
     */
    private suspend fun executeBatchLoop(
        context: Context,
        apps: List<AppInfoModel>,
        delayMillis: Long
    ) {
        for ((index, app) in apps.withIndex()) {
            if (!_isBatchActive.value) break

            _progress.update { state ->
                val updatedItems = state.items.toMutableList()
                if (index < updatedItems.size) {
                    updatedItems[index] = updatedItems[index].copy(status = BatchItemStatus.PROCESSING)
                }
                state.copy(
                    currentIndex = index,
                    currentApp = app,
                    items = updatedItems,
                    summary = "Processing ${app.name} (${index + 1}/${apps.size})..."
                )
            }

            // Safety Check 1: Protected Package / User Whitelist Verification
            if (ProtectedAppManager.isProtected(context, app.packageName)) {
                val reason = ProtectedAppManager.getProtectionReason(context, app.packageName)
                    ?: "Protected application excluded by safety rules"
                recordItemResult(index, app, BatchItemStatus.SKIPPED, reason)
                delay(200)
                continue
            }

            // Safety Check 2: Accessibility Service Connection Verification
            val service = GuardianAccessibilityService.instance
            if (service == null || !GuardianAccessibilityService.isConnected.value) {
                recordItemResult(
                    index,
                    app,
                    BatchItemStatus.FAILED,
                    "Accessibility Service disconnected. Operation aborted."
                )
                break
            }

            // Step 1: Open native Android App Info screen
            val opened = AccessibilityUtils.openAppInfoSettings(context, app.packageName)
            if (!opened) {
                recordItemResult(index, app, BatchItemStatus.FAILED, "Unable to launch system App Info settings")
                delay(delayMillis)
                continue
            }

            // Step 2: Await Settings screen rendering (bounded wait)
            var attempts = 0
            var stepResult: AutomationStepResult = AutomationStepResult.Failed("Timeout opening settings")
            while (attempts < 8 && _isBatchActive.value) {
                delay(300)
                attempts++
                val res = service.attemptForceStopOnActiveScreen(app.packageName)
                if (res !is AutomationStepResult.Failed || attempts >= 8) {
                    stepResult = res
                    break
                }
            }

            // Step 3: Record outcome
            when (stepResult) {
                is AutomationStepResult.Success -> {
                    recordItemResult(index, app, BatchItemStatus.SUCCESS, "Force stop activated and verified")
                }
                is AutomationStepResult.AlreadyStopped -> {
                    recordItemResult(index, app, BatchItemStatus.SKIPPED, stepResult.reason)
                }
                is AutomationStepResult.Unverified -> {
                    recordItemResult(index, app, BatchItemStatus.UNVERIFIED, stepResult.reason)
                }
                is AutomationStepResult.Failed -> {
                    recordItemResult(index, app, BatchItemStatus.FAILED, stepResult.reason)
                }
            }

            // Step 4: Deterministic navigation return
            try {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            } catch (_: Exception) {}

            delay(250)

            // Re-assert RAM Guardian to foreground if needed
            bringRamGuardianToForeground(context)

            // Step 5: Configurable safety inter-app delay
            delay(delayMillis.coerceAtLeast(600L))
        }

        // Final summary calculation
        _progress.update { state ->
            val sCount = state.items.count { it.status == BatchItemStatus.SUCCESS }
            val kCount = state.items.count { it.status == BatchItemStatus.SKIPPED }
            val fCount = state.items.count { it.status == BatchItemStatus.FAILED }
            val uCount = state.items.count { it.status == BatchItemStatus.UNVERIFIED }
            state.copy(
                isFinished = true,
                isRunning = false,
                successCount = sCount,
                skippedCount = kCount,
                failedCount = fCount,
                unverifiedCount = uCount,
                summary = "Completed: $sCount stopped, $kCount skipped, $fCount failed."
            )
        }
    }

    private fun bringRamGuardianToForeground(context: Context) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun recordItemResult(
        index: Int,
        app: AppInfoModel,
        status: BatchItemStatus,
        message: String
    ) {
        _progress.update { state ->
            val updatedList = state.items.toMutableList()
            if (index in updatedList.indices) {
                updatedList[index] = BatchItemResult(app = app, status = status, message = message)
            }
            val processed = updatedList.count { it.status != BatchItemStatus.PENDING && it.status != BatchItemStatus.PROCESSING }
            val sCount = updatedList.count { it.status == BatchItemStatus.SUCCESS }
            val kCount = updatedList.count { it.status == BatchItemStatus.SKIPPED }
            val fCount = updatedList.count { it.status == BatchItemStatus.FAILED }
            val uCount = updatedList.count { it.status == BatchItemStatus.UNVERIFIED }

            state.copy(
                processedCount = processed,
                successCount = sCount,
                skippedCount = kCount,
                failedCount = fCount,
                unverifiedCount = uCount,
                items = updatedList
            )
        }
    }
}
