package com.blueth.guard.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class GuardAccessibilityService : AccessibilityService() {

    companion object {
        var instance: GuardAccessibilityService? = null
            private set

        fun isServiceEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(
                "${context.packageName}/${GuardAccessibilityService::class.java.canonicalName}"
            )
        }

        private const val TIMEOUT_PER_APP_MS = 5000L
        private const val DELAY_BETWEEN_CLICKS_MS = 300L
        private const val DELAY_BETWEEN_APPS_MS = 500L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val taskQueue = ConcurrentLinkedQueue<AutomationTask>()

    private var currentMode: AutomationMode = AutomationMode.IDLE
    private var currentPackageList: List<String> = emptyList()
    private var currentPackageIndex = 0
    private var currentStep: AutomationStep = AutomationStep.WAITING
    private var stepStartTime = 0L
    private var onProgressCallback: ((Int, Int) -> Unit)? = null
    private var processingJob: Job? = null

    private val _progress = MutableStateFlow(Pair(0, 0))
    val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()

    private enum class AutomationMode {
        IDLE, FORCE_STOP, CLEAR_CACHE
    }

    private enum class AutomationStep {
        WAITING,
        FIND_FORCE_STOP_BUTTON,
        CLICK_FORCE_STOP_CONFIRM,
        FIND_STORAGE_BUTTON,
        FIND_CLEAR_CACHE_BUTTON,
        DONE
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        scope.cancel()
    }

    fun queueTask(task: AutomationTask) {
        taskQueue.add(task)
        if (currentMode == AutomationMode.IDLE) {
            processNextTask()
        }
    }

    private fun processNextTask() {
        val task = taskQueue.poll() ?: run {
            currentMode = AutomationMode.IDLE
            return
        }

        when (task) {
            is AutomationTask.ForceStop -> {
                currentMode = AutomationMode.FORCE_STOP
                currentPackageList = task.packages.filter { it != packageName }
                currentPackageIndex = 0
                onProgressCallback = task.onProgress
                currentStep = AutomationStep.FIND_FORCE_STOP_BUTTON
                stepStartTime = System.currentTimeMillis()
                _progress.value = Pair(0, currentPackageList.size)
            }
            is AutomationTask.ClearCache -> {
                currentMode = AutomationMode.CLEAR_CACHE
                currentPackageList = task.packages.filter { it != packageName }
                currentPackageIndex = 0
                onProgressCallback = task.onProgress
                currentStep = AutomationStep.FIND_STORAGE_BUTTON
                stepStartTime = System.currentTimeMillis()
                _progress.value = Pair(0, currentPackageList.size)
            }
            is AutomationTask.ForceStopSingle -> {
                currentMode = AutomationMode.FORCE_STOP
                currentPackageList = if (task.packageName != packageName) listOf(task.packageName) else emptyList()
                currentPackageIndex = 0
                onProgressCallback = null
                currentStep = AutomationStep.FIND_FORCE_STOP_BUTTON
                stepStartTime = System.currentTimeMillis()
                _progress.value = Pair(0, currentPackageList.size)
            }
            is AutomationTask.ClearCacheSingle -> {
                currentMode = AutomationMode.CLEAR_CACHE
                currentPackageList = if (task.packageName != packageName) listOf(task.packageName) else emptyList()
                currentPackageIndex = 0
                onProgressCallback = null
                currentStep = AutomationStep.FIND_STORAGE_BUTTON
                stepStartTime = System.currentTimeMillis()
                _progress.value = Pair(0, currentPackageList.size)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (currentMode == AutomationMode.IDLE) return
        if (currentPackageList.isEmpty()) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        // Timeout check
        if (System.currentTimeMillis() - stepStartTime > TIMEOUT_PER_APP_MS) {
            advanceToNextApp()
            return
        }

        val rootNode = rootInActiveWindow ?: return

        when (currentMode) {
            AutomationMode.FORCE_STOP -> handleForceStop(rootNode)
            AutomationMode.CLEAR_CACHE -> handleClearCache(rootNode)
            AutomationMode.IDLE -> { /* no-op */ }
        }
    }

    private fun handleForceStop(rootNode: AccessibilityNodeInfo) {
        when (currentStep) {
            AutomationStep.FIND_FORCE_STOP_BUTTON -> {
                val button = findClickableByTexts(rootNode, listOf("Force stop", "Force Stop", "FORCE STOP"))
                    ?: findClickableByViewId(rootNode, "com.android.settings:id/right_button")
                if (button != null) {
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    currentStep = AutomationStep.CLICK_FORCE_STOP_CONFIRM
                    stepStartTime = System.currentTimeMillis()
                    processingJob = scope.launch {
                        delay(DELAY_BETWEEN_CLICKS_MS)
                    }
                }
            }
            AutomationStep.CLICK_FORCE_STOP_CONFIRM -> {
                val okButton = findClickableByTexts(rootNode, listOf("OK", "Ok", "Force stop", "Force Stop"))
                    ?: findClickableByViewId(rootNode, "android:id/button1")
                    ?: findClickableByViewId(rootNode, "com.android.settings:id/button1")
                if (okButton != null) {
                    okButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    advanceToNextApp()
                }
            }
            else -> { /* no-op */ }
        }
    }

    private fun handleClearCache(rootNode: AccessibilityNodeInfo) {
        when (currentStep) {
            AutomationStep.FIND_STORAGE_BUTTON -> {
                val storageButton = findClickableByTexts(
                    rootNode,
                    listOf("Storage", "Storage & cache", "Storage and cache")
                )
                if (storageButton != null) {
                    storageButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    currentStep = AutomationStep.FIND_CLEAR_CACHE_BUTTON
                    stepStartTime = System.currentTimeMillis()
                    processingJob = scope.launch {
                        delay(DELAY_BETWEEN_CLICKS_MS)
                    }
                }
            }
            AutomationStep.FIND_CLEAR_CACHE_BUTTON -> {
                val clearButton = findClickableByTexts(
                    rootNode,
                    listOf("Clear cache", "Clear Cache", "CLEAR CACHE")
                )
                if (clearButton != null) {
                    clearButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    advanceToNextApp()
                }
            }
            else -> { /* no-op */ }
        }
    }

    private fun advanceToNextApp() {
        currentPackageIndex++
        _progress.value = Pair(currentPackageIndex, currentPackageList.size)
        onProgressCallback?.invoke(currentPackageIndex, currentPackageList.size)

        if (currentPackageIndex >= currentPackageList.size) {
            currentMode = AutomationMode.IDLE
            currentStep = AutomationStep.DONE
            processingJob?.cancel()
            processNextTask()
            return
        }

        // Reset step for next app
        when (currentMode) {
            AutomationMode.FORCE_STOP -> currentStep = AutomationStep.FIND_FORCE_STOP_BUTTON
            AutomationMode.CLEAR_CACHE -> currentStep = AutomationStep.FIND_STORAGE_BUTTON
            AutomationMode.IDLE -> { /* no-op */ }
        }
        stepStartTime = System.currentTimeMillis()

        // Open next app's info screen
        scope.launch {
            delay(DELAY_BETWEEN_APPS_MS)
            val pkg = currentPackageList[currentPackageIndex]
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$pkg".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                advanceToNextApp()
            }
        }
    }

    private fun findClickableByTexts(
        rootNode: AccessibilityNodeInfo,
        texts: List<String>
    ): AccessibilityNodeInfo? {
        for (text in texts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            if (nodes != null) {
                for (node in nodes) {
                    if (node.isClickable && node.isEnabled) return node
                    // Walk up to find a clickable parent
                    var parent = node.parent
                    var depth = 0
                    while (parent != null && depth < 5) {
                        if (parent.isClickable && parent.isEnabled) return parent
                        parent = parent.parent
                        depth++
                    }
                }
            }
        }
        return null
    }

    private fun findClickableByViewId(
        rootNode: AccessibilityNodeInfo,
        viewId: String
    ): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        if (nodes != null) {
            for (node in nodes) {
                if (node.isClickable && node.isEnabled) return node
                var parent = node.parent
                var depth = 0
                while (parent != null && depth < 5) {
                    if (parent.isClickable && parent.isEnabled) return parent
                    parent = parent.parent
                    depth++
                }
            }
        }
        return null
    }

    override fun onInterrupt() {
        currentMode = AutomationMode.IDLE
        processingJob?.cancel()
    }
}
