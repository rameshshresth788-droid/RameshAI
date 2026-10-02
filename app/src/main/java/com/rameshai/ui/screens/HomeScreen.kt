package com.rameshai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rameshai.config.RuntimeConfig
import com.rameshai.core.AssistantOrchestrator
import com.rameshai.ui.components.AiOrb
import com.rameshai.ui.theme.BackgroundDark
import com.rameshai.voice.AndroidSpeechRecognizerEngine
import com.rameshai.voice.AndroidTextToSpeechEngine
import com.rameshai.voice.AssistantState
import com.rameshai.voice.VoiceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * The entire "normal" app experience: a big orb, minimal chrome, tap-or-say to
 * talk. All actual work (AI calls, tool execution) happens in [AssistantOrchestrator];
 * this screen only wires voice I/O to it and reflects [AssistantState] visually.
 */
@Composable
fun HomeScreen(
    orchestrator: AssistantOrchestrator,
    speechEngine: AndroidSpeechRecognizerEngine,
    ttsEngine: AndroidTextToSpeechEngine,
    config: StateFlow<RuntimeConfig>,
    onOpenSettings: () -> Unit,
    hasMicPermission: () -> Boolean,
    onRequestMicPermission: () -> Unit
) {
    val state by orchestrator.state.collectAsState()
    val lastText by orchestrator.lastSpokenText.collectAsState()
    val currentConfig by config.collectAsState()
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("Mic ready — bolo...") }
    var autoListen by remember { mutableStateOf(true) }
    var starting by remember { mutableStateOf(false) }
    var liveTranscript by remember { mutableStateOf("") }
    var greeted by remember { mutableStateOf(false) }

    fun startListening() {
        if (starting || state == AssistantState.LISTENING) return
        if (!hasMicPermission()) {
            onRequestMicPermission()
            return
        }
        starting = true
        orchestrator.onListeningStarted()
        statusText = "Sun raha hoon..."
        speechEngine.startListening(currentConfig.language) { event ->
            when (event) {
                is VoiceEvent.FinalResult -> {
                    starting = false
                    liveTranscript = event.text
                    statusText = "Soch raha hoon..."
                    scope.launch {
                        val reply = orchestrator.handleUserUtterance(event.text)
                        statusText = "Bol raha hoon..."
                        ttsEngine.speak(reply, currentConfig.language) {
                            orchestrator.onSpeakingFinished()
                            statusText = if (autoListen) "Mic ready — bolo..." else "Tap ya bolo \"${currentConfig.wakePhrase}\""
                            if (autoListen) {
                                scope.launch {
                                    delay(350)
                                    startListening()
                                }
                            }
                        }
                    }
                }
                is VoiceEvent.PartialResult -> {
                    liveTranscript = event.text
                    statusText = event.text
                }
                is VoiceEvent.Error -> {
                    starting = false
                    orchestrator.onError(event.message)
                    statusText = event.message
                    if (autoListen) {
                        scope.launch { delay(700); startListening() }
                    }
                }
                is VoiceEvent.TimedOut -> {
                    starting = false
                    orchestrator.resetToIdle()
                    statusText = if (autoListen) "Mic ready — bolo..." else "Tap ya bolo \"${currentConfig.wakePhrase}\""
                    if (autoListen) {
                        scope.launch { delay(300); startListening() }
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(currentConfig.language) {
        ttsEngine.setSpeechRate(currentConfig.voiceSpeed)
        ttsEngine.setFemaleVoice(currentConfig.femaleVoice)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF18233A), BackgroundDark, Color.Black),
                    radius = 1100f
                )
            )
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.clickable { if (state != AssistantState.LISTENING) startListening() },
                contentAlignment = Alignment.Center
            ) {
                AiOrb(state = state)
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = if (state == AssistantState.LISTENING) "Listening" else "Microphone",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (liveTranscript.isNotBlank()) liveTranscript else statusText.ifBlank { lastText },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (state == AssistantState.LISTENING) "🎙️ Sun rahi hoon..." else "RAMESH AI • Female Voice • Ready",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
