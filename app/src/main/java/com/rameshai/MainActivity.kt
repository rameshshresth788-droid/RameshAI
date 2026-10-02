package com.rameshai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rameshai.core.AssistantOrchestrator
import com.rameshai.core.AssistantForegroundService
import com.rameshai.memory.MemoryRepository
import com.rameshai.news.NewsRepository
import com.rameshai.reminders.ReminderScheduler
import com.rameshai.routine.RoutineRepository
import com.rameshai.settings.OnboardingPrefs
import com.rameshai.ui.Screen
import com.rameshai.ui.screens.HomeScreen
import com.rameshai.ui.screens.MemoryScreen
import com.rameshai.ui.screens.OnboardingScreen
import com.rameshai.ui.screens.RemindersScreen
import com.rameshai.ui.screens.RoutineScreen
import com.rameshai.ui.screens.SettingsScreen
import com.rameshai.ui.theme.RameshAITheme
import com.rameshai.voice.AndroidSpeechRecognizerEngine
import com.rameshai.voice.AndroidTextToSpeechEngine

class MainActivity : ComponentActivity() {

    private lateinit var speechEngine: AndroidSpeechRecognizerEngine
    private lateinit var ttsEngine: AndroidTextToSpeechEngine
    private lateinit var orchestrator: AssistantOrchestrator

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results handled by onboarding screen re-checking hasRequiredPermissions() */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as RameshAIApplication
        speechEngine = AndroidSpeechRecognizerEngine(this)
        ttsEngine = AndroidTextToSpeechEngine(this)
        orchestrator = AssistantOrchestrator(
            context = this,
            configRepository = app.configRepository,
            memoryRepository = MemoryRepository(app.memoryDatabase.memoryDao()),
            reminderScheduler = ReminderScheduler(this, app.reminderDatabase.reminderDao()),
            routineRepository = RoutineRepository(this),
            newsRepository = NewsRepository(this, app.configRepository.current())
        )

        setContent {
            RameshAITheme {
                val navController = rememberNavController()
                val onboardingPrefs = remember { OnboardingPrefs(this) }
                val firstRunDone by onboardingPrefs.firstRunDone.collectAsState(initial = true)

                NavHost(
                    navController = navController,
                    startDestination = if (firstRunDone) Screen.Home.route else Screen.Onboarding.route
                ) {
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            hasRequiredPermissions = { hasRequiredPermissions() },
                            onRequestPermissions = { requestRuntimePermissions() },
                            onFinish = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            },
                            onboardingPrefs = onboardingPrefs
                        )
                    }
                    composable(Screen.Home.route) {
                        HomeScreen(
                            orchestrator = orchestrator,
                            speechEngine = speechEngine,
                            ttsEngine = ttsEngine,
                            config = app.configRepository.config,
                            onOpenSettings = { navController.navigate(Screen.Settings.route) },
                            hasMicPermission = { hasMicPermission() },
                            onRequestMicPermission = { requestMicPermission() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            configRepository = app.configRepository,
                            onBack = { navController.popBackStack() },
                            onOpenMemory = { navController.navigate(Screen.Memory.route) },
                            onOpenReminders = { navController.navigate(Screen.Reminders.route) },
                            onOpenRoutine = { navController.navigate(Screen.Routine.route) }
                        )
                    }
                    composable(Screen.Memory.route) {
                        MemoryScreen(
                            memoryRepository = MemoryRepository(app.memoryDatabase.memoryDao()),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Reminders.route) {
                        RemindersScreen(
                            dao = app.reminderDatabase.reminderDao(),
                            scheduler = ReminderScheduler(this@MainActivity, app.reminderDatabase.reminderDao()),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Routine.route) {
                        RoutineScreen(
                            routineRepository = RoutineRepository(this@MainActivity),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // The Home screen owns the microphone while visible. Background service takes over only after the activity leaves foreground.
        try { AssistantForegroundService.stop(this) } catch (_: Exception) { }
    }

    override fun onStop() {
        val app = application as RameshAIApplication
        if (!isChangingConfigurations && app.configRepository.current().featureBackgroundListening && hasMicPermission()) {
            try { AssistantForegroundService.start(this) } catch (_: Exception) { }
        }
        super.onStop()
    }

    override fun onDestroy() {
        speechEngine.release()
        ttsEngine.release()
        super.onDestroy()
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestMicPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    private fun hasRequiredPermissions(): Boolean {
        val mic = hasMicPermission()
        val notif = if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return mic && notif
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        permissionLauncher.launch(perms.toTypedArray())
    }
}
