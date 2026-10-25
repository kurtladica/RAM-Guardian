package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AutomationStepResult {
    data object Success : AutomationStepResult
    data class AlreadyStopped(val reason: String = "Force stop control is disabled (app not running)") : AutomationStepResult
    data class Failed(val reason: String) : AutomationStepResult
    data class Unverified(val reason: String) : AutomationStepResult
}

class GuardianAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        Log.i(TAG, "GuardianAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Zero-overhead guard: ignore all events when no user-initiated batch is active
        if (!BatchAutomationEngine.isBatchActive.value) {
            return
        }

        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                BatchAutomationEngine.notifyWindowUpdated()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "GuardianAccessibilityService interrupted")
        _isConnected.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        _isConnected.value = false
        Log.i(TAG, "GuardianAccessibilityService destroyed")
    }

    /**
     * Inspects the active settings window, locates the Force stop control, and clicks it if enabled.
     */
    fun attemptForceStopOnActiveScreen(targetPackage: String): AutomationStepResult {
        val rootNode = findActiveRootNode() ?: return AutomationStepResult.Failed("Could not access active window content")

        try {
            // Verify screen package belongs to Android Settings
            val currentPkg = rootNode.packageName?.toString() ?: ""
            if (!isSettingsPackage(currentPkg)) {
                return AutomationStepResult.Failed("Active window is '$currentPkg', not Android Settings")
            }

            // Search for Force Stop action node
            val forceStopNode = findForceStopNode(rootNode)
                ?: return AutomationStepResult.Failed("Force stop control not found on visible screen")

            try {
                // If the control is disabled, Android indicates the app has no active processes running
                if (!forceStopNode.isEnabled) {
                    return AutomationStepResult.AlreadyStopped("Force stop button is disabled (app already dormant)")
                }

                // Node is enabled: perform the click action
                val clickSuccess = forceStopNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clickSuccess) {
                    return AutomationStepResult.Failed("Accessibility ACTION_CLICK rejected by system control")
                }

                // Short sleep to allow the confirmation dialog to appear
                Thread.sleep(350)

                // Check for confirmation dialog (e.g. "If you force stop an app...")
                val dialogRoot = findActiveRootNode() ?: rootNode
                val confirmNode = findConfirmDialogButton(dialogRoot)
                if (confirmNode != null) {
                    try {
                        confirmNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Thread.sleep(300)
                    } finally {
                        if (confirmNode != dialogRoot && confirmNode != rootNode) {
                            @Suppress("DEPRECATION")
                            confirmNode.recycle()
                        }
                    }
                }

                // Verify the resulting state: inspect if Force Stop button is now disabled
                val afterRoot = findActiveRootNode()
                if (afterRoot != null) {
                    try {
                        val afterNode = findForceStopNode(afterRoot)
                        if (afterNode != null) {
                            val isNowDisabled = !afterNode.isEnabled
                            @Suppress("DEPRECATION")
                            afterNode.recycle()
                            return if (isNowDisabled) {
                                AutomationStepResult.Success
                            } else {
                                AutomationStepResult.Unverified("Force stop control remains active after click")
                            }
                        }
                    } finally {
                        if (afterRoot != rootNode) {
                            @Suppress("DEPRECATION")
                            afterRoot.recycle()
                        }
                    }
                }

                return AutomationStepResult.Success
            } finally {
                @Suppress("DEPRECATION")
                forceStopNode.recycle()
            }
        } finally {
            @Suppress("DEPRECATION")
            rootNode.recycle()
        }
    }

    /**
     * Finds active root node from active window or window list.
     */
    private fun findActiveRootNode(): AccessibilityNodeInfo? {
        rootInActiveWindow?.let { return it }
        val currentWindows = windows
        for (win in currentWindows) {
            val root = win.root
            if (root != null) {
                return root
            }
        }
        return null
    }

    private fun isSettingsPackage(pkg: String): Boolean {
        return pkg.contains("settings", ignoreCase = true)
    }

    /**
     * Recursively searches for the Force stop button node.
     */
    private fun findForceStopNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val targetTexts = listOf(
            "force stop",
            "force close",
            "forzar detención",
            "forcer l'arrêt",
            "beenden erzwingen",
            "arresto forzato",
            "forçar parada",
            "forçar interrupção",
            "强制停止",
            "強行停止",
            "강제 종료",
            "остановить",
            "paksa berhenti",
            "buộc dừng"
        )

        val targetViewIds = listOf(
            "com.android.settings:id/button1_negative",
            "com.android.settings:id/force_stop_button",
            "com.android.settings:id/right_button",
            "com.android.settings:id/button2",
            "com.android.settings:id/button3",
            "com.android.settings:id/btn_force_stop"
        )

        // Try view IDs first
        for (id in targetViewIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val match = nodes.firstOrNull { it.isClickable } ?: nodes.first()
                return match
            }
        }

        // Try text matching
        for (targetText in targetTexts) {
            val nodes = root.findAccessibilityNodeInfosByText(targetText)
            if (!nodes.isNullOrEmpty()) {
                val match = nodes.firstOrNull { node ->
                    val text = node.text?.toString()?.trim() ?: ""
                    text.equals(targetText, ignoreCase = true) || text.contains(targetText, ignoreCase = true)
                }
                if (match != null) {
                    return match
                }
            }
        }

        return null
    }

    /**
     * Searches for positive confirmation button on the "Force stop?" dialog.
     */
    private fun findConfirmDialogButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Standard Android dialog positive button ID
        val positiveButton = root.findAccessibilityNodeInfosByViewId("android:id/button1")
        if (!positiveButton.isNullOrEmpty()) {
            return positiveButton.first()
        }

        // Text matches for confirmation
        val confirmTexts = listOf(
            "ok",
            "force stop",
            "forzar detención",
            "forcer l'arrêt",
            "beenden erzwingen",
            "conferma",
            "确定",
            "確認",
            "확인"
        )

        for (ct in confirmTexts) {
            val nodes = root.findAccessibilityNodeInfosByText(ct)
            if (!nodes.isNullOrEmpty()) {
                val match = nodes.firstOrNull { it.isClickable }
                if (match != null) return match
            }
        }

        return null
    }

    companion object {
        private const val TAG = "GuardianA11yService"

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        @Volatile
        var instance: GuardianAccessibilityService? = null
            private set
    }
}
