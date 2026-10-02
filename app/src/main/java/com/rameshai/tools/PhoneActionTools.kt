package com.rameshai.tools

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import com.rameshai.core.AssistantForegroundService

/**
 * Thin wrapper around real, official Android APIs. Everything here uses public,
 * documented mechanisms — no root, no hidden APIs, no accessibility-service
 * bypass of permission prompts. If an action needs a permission the user hasn't
 * granted, it returns a clear failure rather than silently doing nothing.
 */
class PhoneActionTools(private val context: Context) {

    fun launchPackage(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun openUrl(url: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolExecutionResult.Success("Browser khol raha hoon.")
        } catch (e: Exception) {
            ToolExecutionResult.Failure("URL open nahi ho paya.")
        }
    }

    fun openSettings(setting: String): ToolExecutionResult {
        val action = when (setting.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notification_listener" -> "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolExecutionResult.Success("Settings khol raha hoon.")
        } catch (e: Exception) {
            ToolExecutionResult.Failure("Ye settings page open nahi ho paya.")
        }
    }

    /** Requires WRITE_SETTINGS (Settings.System.canWrite); guides user if not granted. */
    fun setBrightness(percentStr: String): ToolExecutionResult {
        val percent = percentStr.toIntOrNull()?.coerceIn(0, 100)
            ?: return ToolExecutionResult.Failure("Brightness value samajh nahi aayi.")

        if (!Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return ToolExecutionResult.Failure(
                "Brightness change karne ke liye 'Modify System Settings' permission chahiye. Maine permission screen khol di hai."
            )
        }

        val value = (percent * 255) / 100
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        return ToolExecutionResult.Success("Brightness $percent% kar diya.")
    }

    fun mediaPlay(): ToolExecutionResult = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    fun mediaPause(): ToolExecutionResult = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)

    private fun sendMediaKey(keyCode: Int): ToolExecutionResult {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            ToolExecutionResult.Success("Ho gaya.")
        } catch (e: Exception) {
            ToolExecutionResult.Failure("Media control kaam nahi kiya.")
        }
    }

    fun dialNumber(contactOrNumber: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactOrNumber"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolExecutionResult.Success("Dialer khol diya, call karne ke liye confirm karo.")
        } catch (e: Exception) {
            ToolExecutionResult.Failure("Dialer open nahi ho paya.")
        }
    }

    fun stopAssistantService(): ToolExecutionResult {
        context.stopService(Intent(context, AssistantForegroundService::class.java))
        return ToolExecutionResult.Success("Assistant background service band kar diya.")
    }
}
