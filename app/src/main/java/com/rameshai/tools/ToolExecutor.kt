package com.rameshai.tools

import android.content.Context
import com.rameshai.accessibility.RameshAccessibilityService
import com.rameshai.ai.ToolCall
import com.rameshai.apps.AppRegistry
import com.rameshai.news.NewsRepository
import com.rameshai.notifications.RameshNotificationService
import com.rameshai.reminders.ReminderScheduler
import com.rameshai.routine.RoutineRepository

/** Outcome of executing a validated tool call — always spoken back to the user. */
sealed class ToolExecutionResult {
    data class Success(val spokenMessage: String) : ToolExecutionResult()
    data class NeedsConfirmation(val spokenPrompt: String, val pendingCall: ToolCall) : ToolExecutionResult()
    data class Failure(val spokenMessage: String) : ToolExecutionResult()
}

/**
 * The single choke point between "the AI wants to do X" and "X actually happens
 * on the phone". Every call here is:
 *   1. Checked against [ToolDefinitions.ALL] — unknown tool names are rejected.
 *   2. Checked for required arguments.
 *   3. Routed to a risky-action confirmation step if [ToolDefinitions.ToolSpec.requiresConfirmation].
 *   4. Only then dispatched to the real Android API.
 *
 * Nothing here ever claims success before the underlying Android call actually
 * confirms it worked (per the "never claim to have performed an action" rule).
 */
class ToolExecutor(
    private val context: Context,
    private val appRegistry: AppRegistry,
    private val phoneActions: PhoneActionTools,
    private val reminderScheduler: ReminderScheduler,
    private val routineRepository: RoutineRepository,
    private val newsRepository: NewsRepository
) {

    suspend fun execute(call: ToolCall, confirmed: Boolean = false): ToolExecutionResult {
        val spec = ToolDefinitions.byName(call.tool)
            ?: return ToolExecutionResult.Failure("Ye action supported nahi hai.")

        val missing = spec.params.filter { it.required && call.arguments[it.name].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            return ToolExecutionResult.Failure(
                "Is action ke liye ${missing.joinToString { it.name }} chahiye tha, mila nahi."
            )
        }

        if (spec.requiresConfirmation && !confirmed) {
            return ToolExecutionResult.NeedsConfirmation(buildConfirmationPrompt(call), call)
        }

        return try {
            when (call.tool) {
                ToolDefinitions.OPEN_APP.name -> openApp(call)
                ToolDefinitions.OPEN_SETTINGS.name -> phoneActions.openSettings(call.arguments["setting"].orEmpty())
                ToolDefinitions.OPEN_URL.name -> phoneActions.openUrl(call.arguments["url"].orEmpty())
                ToolDefinitions.SET_BRIGHTNESS.name -> phoneActions.setBrightness(call.arguments["percent"].orEmpty())
                ToolDefinitions.MEDIA_PLAY.name -> phoneActions.mediaPlay()
                ToolDefinitions.MEDIA_PAUSE.name -> phoneActions.mediaPause()
                ToolDefinitions.READ_NOTIFICATIONS.name -> readNotifications(call)
                ToolDefinitions.CREATE_REMINDER.name -> createReminder(call)
                ToolDefinitions.GET_TODAY_ROUTINE.name -> getTodayRoutine()
                ToolDefinitions.GET_NEWS.name -> getNews(call)
                ToolDefinitions.SEARCH_WEB.name -> ToolExecutionResult.Failure(
                    "Web search abhi is response me handle hota hai AI provider ke through; direct tool call yahan support nahi hai."
                )
                ToolDefinitions.SEND_MESSAGE.name -> sendMessage(call)
                ToolDefinitions.MAKE_CALL.name -> phoneActions.dialNumber(call.arguments["contact_or_number"].orEmpty())
                ToolDefinitions.STOP_ASSISTANT.name -> phoneActions.stopAssistantService()
                else -> ToolExecutionResult.Failure("Ye action abhi implement nahi hua.")
            }
        } catch (e: SecurityException) {
            ToolExecutionResult.Failure("Is action ke liye permission missing hai: ${e.message}")
        } catch (e: Exception) {
            ToolExecutionResult.Failure("Action perform karte waqt error aaya: ${e.message}")
        }
    }

    private fun buildConfirmationPrompt(call: ToolCall): String = when (call.tool) {
        ToolDefinitions.SEND_MESSAGE.name -> {
            val contact = call.arguments["contact"].orEmpty()
            val msg = call.arguments["message"].orEmpty()
            "Aap $contact ko message bhejna chahte hain: '$msg'. Confirm karein?"
        }
        ToolDefinitions.MAKE_CALL.name -> {
            "Aap ${call.arguments["contact_or_number"]} ko call karna chahte hain. Confirm karein?"
        }
        else -> "Ye action confirm karein?"
    }

    private fun openApp(call: ToolCall): ToolExecutionResult {
        val appName = call.arguments["app_name"].orEmpty()
        val match = appRegistry.findByName(appName)
            ?: return ToolExecutionResult.Failure("$appName installed nahi hai.")
        val launched = phoneActions.launchPackage(match.packageName)
        return if (launched) ToolExecutionResult.Success("${match.label} khol raha hoon.")
        else ToolExecutionResult.Failure("${match.label} open nahi ho paya.")
    }

    private fun readNotifications(call: ToolCall): ToolExecutionResult {
        if (!RameshNotificationService.isEnabled(context)) {
            return ToolExecutionResult.Failure(
                "Notifications padhne ke liye Notification Access permission chahiye. Settings me enable karo."
            )
        }
        val filterApp = call.arguments["app_name"]
        val notifications = RameshNotificationService.lastNotifications(filterApp)
        if (notifications.isEmpty()) return ToolExecutionResult.Success("Koi recent notification nahi mili.")
        return ToolExecutionResult.Success(notifications.joinToString("; "))
    }

    private suspend fun createReminder(call: ToolCall): ToolExecutionResult {
        val message = call.arguments["message"].orEmpty()
        val time = call.arguments["time"].orEmpty()
        val recurrence = call.arguments["recurrence"] ?: "none"
        val scheduled = reminderScheduler.schedule(message, time, recurrence)
        return if (scheduled) ToolExecutionResult.Success("Reminder set kar diya: '$message' ($time).")
        else ToolExecutionResult.Failure("Reminder time samajh nahi aaya, format check karo.")
    }

    private suspend fun getTodayRoutine(): ToolExecutionResult {
        val routine = routineRepository.getTodayRoutine()
        if (routine.isEmpty()) return ToolExecutionResult.Success("Aapka routine abhi set nahi hai. Settings > Routine me set karo.")
        return ToolExecutionResult.Success(routine.joinToString("; ") { "${it.time} - ${it.activity}" })
    }

    private suspend fun getNews(call: ToolCall): ToolExecutionResult {
        val topic = call.arguments["topic"]
        return when (val result = newsRepository.fetchNews(topic)) {
            is NewsRepository.Result.Success -> ToolExecutionResult.Success(result.summary)
            is NewsRepository.Result.NoInternet -> ToolExecutionResult.Failure(
                "Internet connection nahi hai, isliye latest news verify nahi kar pa raha hoon."
            )
            is NewsRepository.Result.NotConfigured -> ToolExecutionResult.Failure(
                "News provider configure nahi hai. Settings > AI > News Provider me set karo."
            )
            is NewsRepository.Result.Error -> ToolExecutionResult.Failure(result.message)
        }
    }

    private fun sendMessage(call: ToolCall): ToolExecutionResult {
        if (!RameshAccessibilityService.isEnabled(context)) {
            return ToolExecutionResult.Failure(
                "Message bhejne ke liye Accessibility permission chahiye. Settings me permission enable karo."
            )
        }
        val ok = RameshAccessibilityService.requestSendMessage(
            appName = call.arguments["app_name"].orEmpty(),
            contact = call.arguments["contact"].orEmpty(),
            message = call.arguments["message"].orEmpty()
        )
        return if (ok) ToolExecutionResult.Success("Message bhej diya.")
        else ToolExecutionResult.Failure("Message bhejne me problem aayi, app manually check karo.")
    }
}
