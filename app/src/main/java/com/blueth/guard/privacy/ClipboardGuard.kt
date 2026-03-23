package com.blueth.guard.privacy

import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ClipType { TEXT, URI, INTENT, HTML, UNKNOWN }

data class ClipboardEvent(
    val timestamp: Long,
    val clipType: ClipType,
    val sourcePackage: String?,
    val contentPreview: String,
    val isSensitive: Boolean
)

data class ClipboardStatus(
    val isMonitoring: Boolean,
    val eventCount: Int,
    val sensitiveCount: Int,
    val lastEventTime: Long?
)

@Singleton
class ClipboardGuard @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private val events = ArrayDeque<ClipboardEvent>(MAX_EVENTS)
    private var isMonitoring = false
    private var lastBackgroundTime: Long? = null

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        onClipChanged()
    }

    fun startMonitoring() {
        if (isMonitoring) return
        clipboardManager.addPrimaryClipChangedListener(listener)
        isMonitoring = true
        lastBackgroundTime = null
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        clipboardManager.removePrimaryClipChangedListener(listener)
        isMonitoring = false
    }

    fun getRecentClipboardEvents(): List<ClipboardEvent> = events.toList()

    fun clearHistory() {
        events.clear()
    }

    fun getClipboardStatus(): ClipboardStatus = ClipboardStatus(
        isMonitoring = isMonitoring,
        eventCount = events.size,
        sensitiveCount = events.count { it.isSensitive },
        lastEventTime = events.firstOrNull()?.timestamp
    )

    fun onAppBackgrounded() {
        lastBackgroundTime = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val bgTime = lastBackgroundTime
        if (bgTime != null && System.currentTimeMillis() - bgTime > BACKGROUND_CLEAR_MS) {
            events.clear()
        }
        lastBackgroundTime = null
    }

    private fun onClipChanged() {
        val clip = try {
            clipboardManager.primaryClip
        } catch (_: SecurityException) {
            return
        } ?: return

        val clipType: ClipType
        val preview: String

        if (clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            when {
                item.htmlText != null -> {
                    clipType = ClipType.HTML
                    val text = item.htmlText ?: ""
                    preview = buildPreview(text)
                }
                item.text != null -> {
                    clipType = ClipType.TEXT
                    val text = item.text.toString()
                    preview = buildPreview(text)
                }
                item.uri != null -> {
                    clipType = ClipType.URI
                    preview = item.uri.toString().take(50)
                }
                item.intent != null -> {
                    clipType = ClipType.INTENT
                    preview = item.intent?.action ?: "intent"
                }
                else -> {
                    clipType = ClipType.UNKNOWN
                    preview = "(unknown content)"
                }
            }
        } else {
            clipType = ClipType.UNKNOWN
            preview = "(empty)"
        }

        val rawText = when (clipType) {
            ClipType.TEXT, ClipType.HTML -> {
                if (clip.itemCount > 0) clip.getItemAt(0).text?.toString() ?: "" else ""
            }
            else -> ""
        }

        val sensitive = isSensitiveContent(rawText)
        val displayPreview = if (sensitive) maskContent(rawText) else preview

        val event = ClipboardEvent(
            timestamp = System.currentTimeMillis(),
            clipType = clipType,
            sourcePackage = null,
            contentPreview = displayPreview,
            isSensitive = sensitive
        )

        if (events.size >= MAX_EVENTS) {
            events.removeLast()
        }
        events.addFirst(event)
    }

    private fun buildPreview(text: String): String {
        return if (text.length <= 50) text else text.take(50) + "…"
    }

    companion object {
        private const val MAX_EVENTS = 50
        private const val BACKGROUND_CLEAR_MS = 5 * 60 * 1000L

        private val CREDIT_CARD_REGEX = Regex("""\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""")
        private val SSN_REGEX = Regex("""\b\d{3}-\d{2}-\d{4}\b""")
        private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
        private val API_KEY_REGEX = Regex("""\b[A-Za-z0-9_-]{32,}\b""")
        private val PASSWORD_REGEX = Regex("""(?i)\b(?:password|passwd|pwd)\s*[:=]\s*\S+""")

        fun isSensitiveContent(text: String): Boolean {
            if (text.isBlank()) return false
            return CREDIT_CARD_REGEX.containsMatchIn(text) ||
                SSN_REGEX.containsMatchIn(text) ||
                API_KEY_REGEX.containsMatchIn(text) ||
                PASSWORD_REGEX.containsMatchIn(text)
        }

        fun maskContent(text: String): String {
            if (text.length <= 8) return "*".repeat(text.length)
            val first = text.take(4)
            val last = text.takeLast(4)
            val middle = "*".repeat(minOf(text.length - 8, 20))
            return "$first$middle$last"
        }
    }
}
