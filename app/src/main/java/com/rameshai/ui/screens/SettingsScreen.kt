package com.rameshai.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rameshai.accessibility.RameshAccessibilityService
import com.rameshai.config.ConfigRepository
import com.rameshai.config.RuntimeConfig
import com.rameshai.notifications.RameshNotificationService
import com.rameshai.ui.theme.BackgroundDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    configRepository: ConfigRepository,
    onBack: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenRoutine: () -> Unit
) {
    val context = LocalContext.current
    val config by configRepository.config.collectAsState()
    var localConfig by remember(config) { mutableStateOf(config) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Assistant")
            OutlinedTextField(
                value = localConfig.assistantName, onValueChange = { localConfig = localConfig.copy(assistantName = it) },
                label = { Text("Assistant name") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = localConfig.wakePhrase, onValueChange = { localConfig = localConfig.copy(wakePhrase = it) },
                label = { Text("Wake phrase") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = localConfig.language, onValueChange = { localConfig = localConfig.copy(language = it) },
                label = { Text("Language (BCP-47, e.g. hi-IN, en-US)") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Speaking speed: ${"%.1f".format(localConfig.voiceSpeed)}x")
            Slider(
                value = localConfig.voiceSpeed, onValueChange = { localConfig = localConfig.copy(voiceSpeed = it) },
                valueRange = 0.5f..2f
            )
            ToggleRow("Female AI voice", localConfig.femaleVoice) {
                localConfig = localConfig.copy(femaleVoice = it)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("AI Provider")
            ProviderDropdown(localConfig.aiProvider) { provider ->
                localConfig = when (provider) {
                    "openrouter" -> localConfig.copy(
                        aiProvider = provider,
                        aiBaseUrl = "https://openrouter.ai/api/v1",
                        aiModel = if (localConfig.aiModel == "gpt-4o-mini" || localConfig.aiModel.isBlank()) "openai/gpt-4o-mini" else localConfig.aiModel
                    )
                    "openai" -> localConfig.copy(
                        aiProvider = provider,
                        aiBaseUrl = "https://api.openai.com/v1",
                        aiModel = if (localConfig.aiModel.isBlank()) "gpt-4o-mini" else localConfig.aiModel
                    )
                    "gemini" -> localConfig.copy(
                        aiProvider = provider,
                        aiBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
                        aiModel = if (localConfig.aiModel.isBlank() || localConfig.aiModel.startsWith("gpt")) "gemini-2.0-flash" else localConfig.aiModel
                    )
                    else -> localConfig.copy(aiProvider = provider)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = localConfig.aiBaseUrl, onValueChange = { localConfig = localConfig.copy(aiBaseUrl = it) },
                label = { Text("API Base URL") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = localConfig.aiApiKey, onValueChange = { localConfig = localConfig.copy(aiApiKey = it) },
                label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (apiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None
                else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) { Text(if (apiKeyVisible) "Hide" else "Show") }
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = localConfig.aiModel, onValueChange = { localConfig = localConfig.copy(aiModel = it) },
                label = { Text("Model") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Temperature: ${"%.1f".format(localConfig.aiTemperature)}")
            Slider(
                value = localConfig.aiTemperature, onValueChange = { localConfig = localConfig.copy(aiTemperature = it) },
                valueRange = 0f..1.5f
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = localConfig.systemPrompt, onValueChange = { localConfig = localConfig.copy(systemPrompt = it) },
                label = { Text("System prompt / personality") }, modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("Phone Control")
            StatusRow("Accessibility", RameshAccessibilityService.isEnabled(context)) {
                openAccessibilitySettings(context)
            }
            StatusRow("Notification Access", RameshNotificationService.isEnabled(context)) {
                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
            StatusRow("Modify System Settings", Settings.System.canWrite(context)) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("Navigate")
            Button(onClick = onOpenMemory, modifier = Modifier.fillMaxWidth()) { Text("Memory") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenReminders, modifier = Modifier.fillMaxWidth()) { Text("Reminders") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenRoutine, modifier = Modifier.fillMaxWidth()) { Text("Routine") }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("Feature Toggles")
            ToggleRow("Accessibility automation", localConfig.featureAccessibility) {
                localConfig = localConfig.copy(featureAccessibility = it)
            }
            ToggleRow("Read notifications", localConfig.featureNotificationsRead) {
                localConfig = localConfig.copy(featureNotificationsRead = it)
            }
            ToggleRow("Background continuous listening", localConfig.featureBackgroundListening) {
                localConfig = localConfig.copy(featureBackgroundListening = it)
            }
            ToggleRow("Web search", localConfig.featureWebSearch) {
                localConfig = localConfig.copy(featureWebSearch = it)
            }
            ToggleRow("Debug mode", localConfig.debugMode) {
                localConfig = localConfig.copy(debugMode = it)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("Privacy")
            Text(
                "Masked key: ${config.maskedApiKey()}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { configRepository.resetToDefaults(); localConfig = RuntimeConfig() }) {
                Text("Reset configuration to defaults")
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { configRepository.update { localConfig } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Settings") }

            Spacer(Modifier.height(32.dp))
            SectionTitle("About")
            Text("RAMESH AI v${com.rameshai.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
}

private fun openAccessibilitySettings(context: android.content.Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    } catch (_: Exception) {
        // Ignore if the device does not expose Accessibility Settings.
    }
}

@Composable
private fun StatusRow(label: String, granted: Boolean, onFix: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        if (granted) Text("Granted ✅") else TextButton(onClick = onFix) { Text("Enable") }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ProviderDropdown(current: String, onSelect: (String) -> Unit) {
    val options = listOf("openai", "gemini", "openrouter", "local")
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("Provider: $current") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
