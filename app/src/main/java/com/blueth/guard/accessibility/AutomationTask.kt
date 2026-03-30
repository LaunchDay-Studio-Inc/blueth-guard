package com.blueth.guard.accessibility

sealed class AutomationTask {
    data class ForceStop(val packages: List<String>, val onProgress: (Int, Int) -> Unit) : AutomationTask()
    data class ClearCache(val packages: List<String>, val onProgress: (Int, Int) -> Unit) : AutomationTask()
    data class ForceStopSingle(val packageName: String) : AutomationTask()
    data class ClearCacheSingle(val packageName: String) : AutomationTask()
}
