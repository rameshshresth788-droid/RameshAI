package com.rameshai.config

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/**
 * Loads and persists [RuntimeConfig].
 *
 * Storage location: app-private files dir, `config/assistant.json`, encrypted at rest
 * using Jetpack Security (AES256-GCM) so the API key isn't sitting in plaintext on
 * disk even on a rooted device. This file is:
 *   - Never committed to git (see .gitignore)
 *   - Editable from the Settings screen in-app
 *   - Editable from Termux via `adb shell run-as com.rameshai` + `tools/config.sh`,
 *     OR simply through the in-app Settings UI, which is the recommended path
 *     since `run-as` requires a debuggable build.
 *
 * Changing any value here takes effect on the next AI/voice/news call — no APK
 * rebuild required, which is the core "no-rebuild configuration" requirement.
 */
class ConfigRepository(private val context: Context) {

    private val configDir = File(context.filesDir, "config").apply { mkdirs() }
    private val configFile = File(configDir, "assistant.json")

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val _config = MutableStateFlow(RuntimeConfig())
    val config: StateFlow<RuntimeConfig> = _config.asStateFlow()

    fun loadOrCreateDefault(): RuntimeConfig {
        val loaded = readEncrypted()
        _config.value = loaded ?: RuntimeConfig().also { writeEncrypted(it) }
        return _config.value
    }

    fun update(transform: (RuntimeConfig) -> RuntimeConfig) {
        val updated = transform(_config.value)
        _config.value = updated
        writeEncrypted(updated)
    }

    fun current(): RuntimeConfig = _config.value

    private fun readEncrypted(): RuntimeConfig? {
        if (!configFile.exists()) return null
        return try {
            // Encrypted files can't be opened for read if they don't exist yet with this API,
            // and can't be overwritten in place, so we read via a temp decrypt each time.
            val bytes = decryptFile(configFile) ?: return null
            RuntimeConfig.fromJson(JSONObject(String(bytes)))
        } catch (e: Exception) {
            null // corrupted config -> fall back to defaults rather than crash
        }
    }

    private fun writeEncrypted(config: RuntimeConfig) {
        // EncryptedFile refuses to overwrite; write to temp then swap.
        val tempFile = File(configDir, "assistant.json.tmp")
        if (tempFile.exists()) tempFile.delete()

        val encryptedFile = EncryptedFile.Builder(
            context, tempFile, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { out ->
            out.write(config.toJson().toString().toByteArray())
        }

        if (configFile.exists()) configFile.delete()
        tempFile.renameTo(configFile)
    }

    private fun decryptFile(file: File): ByteArray? {
        return try {
            val encryptedFile = EncryptedFile.Builder(
                context, file, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileInput().use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    /** True when the assistant has enough config to actually call an AI provider. */
    fun isAiConfigured(): Boolean = current().aiApiKey.isNotBlank() && current().aiBaseUrl.isNotBlank()

    fun resetToDefaults() {
        val defaults = RuntimeConfig()
        _config.value = defaults
        writeEncrypted(defaults)
    }
}
