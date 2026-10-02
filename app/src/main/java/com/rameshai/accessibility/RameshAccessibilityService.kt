package com.rameshai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Legitimate, user-requested automation only. This service:
 *  - Never enables itself — the user must turn it on in Android Settings.
 *  - Never runs an action the user didn't ask the assistant to perform.
 *  - Is fully visible in Settings > Accessibility, with a clear description
 *    (see res/xml/accessibility_service_config.xml + strings.xml).
 *
 * [ToolExecutor] is the only caller of the `requestX` functions below, and it
 * only calls them after the AI has produced a validated, whitelisted tool call
 * (and after user confirmation for risky actions like sending a message).
 */
class RameshAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Re-apply the service configuration explicitly. Some OEM Android builds
        // are picky about accessibility-service defaults loaded from XML.
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            // Do not set isAccessibilityTool here; it is configured by XML/manifest.
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally minimal: this app does not passively harvest screen content.
        // Window-change events are only consulted synchronously while executing
        // a specific, user-requested automation (see performClick/performType below).
    }

    override fun onInterrupt() {}

    // --- Low-level automation primitives, used by requestSendMessage() etc. ---

    fun clickNodeByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun typeIntoFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val arguments = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val matches = root.findAccessibilityNodeInfosByText(text)
        return matches?.firstOrNull()
    }

    companion object {
        private var instance: RameshAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expected = "${context.packageName}/${RameshAccessibilityService::class.java.name}"
            return enabledServices.split(':').any {
                it.equals(expected, ignoreCase = true) ||
                    it.endsWith("/${RameshAccessibilityService::class.java.name}", ignoreCase = true)
            }
        }

        /**
         * High-level "open app, tap the contact, type the message" flow. Real apps'
         * UI trees vary a lot, so this is a best-effort automation built on the
         * generic primitives above; it reports failure clearly rather than
         * pretending to have sent something it couldn't confirm.
         */
        fun requestSendMessage(appName: String, contact: String, message: String): Boolean {
            val service = instance ?: return false
            // Best-effort generic flow: find contact node, click, find input, type, find send.
            val foundContact = service.clickNodeByText(contact)
            if (!foundContact) return false
            val typed = service.typeIntoFocusedField(message)
            if (!typed) return false
            return service.clickNodeByText("Send")
        }
    }
}
