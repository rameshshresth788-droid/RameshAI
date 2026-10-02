package com.rameshai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rameshai.settings.OnboardingPrefs
import com.rameshai.ui.theme.BackgroundDark
import kotlinx.coroutines.launch

/**
 * First-run flow per spec section 31: welcome -> name -> permissions (mic
 * required for voice; notification, accessibility, overlay, battery all
 * skippable) -> AI provider hint -> done. Nothing here forces a permission —
 * every optional step has a visible "Skip" action.
 */
@Composable
fun OnboardingScreen(
    hasRequiredPermissions: () -> Boolean,
    onRequestPermissions: () -> Unit,
    onFinish: () -> Unit,
    onboardingPrefs: OnboardingPrefs
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("RAMESH") }

    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundDark).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                0 -> {
                    Text("Welcome to RAMESH AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Aapka apna private voice assistant.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { step = 1 }) { Text("Shuru karein") }
                }
                1 -> {
                    Text("Aapka naam kya hai?", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        scope.launch { onboardingPrefs.setOwnerName(name) }
                        step = 2
                    }) { Text("Aage badhein") }
                }
                2 -> {
                    Text("Microphone Permission", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Voice se baat karne ke liye microphone permission zaroori hai.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onRequestPermissions) { Text("Permission do") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { step = 3 }) { Text("Aage badhein") }
                }
                3 -> {
                    Text("Aap taiyar hain!", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Accessibility aur Notification permissions baad me Settings se enable kar sakte hain, jab zaroorat ho.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "AI kaam karne ke liye Settings > AI me apna API key daalna na bhoolein.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        scope.launch {
                            onboardingPrefs.markFirstRunDone()
                            onFinish()
                        }
                    }) { Text("Shuru karein") }
                }
            }
        }
    }
}
