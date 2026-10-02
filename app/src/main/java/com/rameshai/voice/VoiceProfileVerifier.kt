package com.rameshai.voice

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Optional "Train my voice" feature.
 *
 * IMPORTANT / HONESTY NOTICE:
 * This stores a lightweight local acoustic fingerprint (average pitch/energy
 * features from a few enrollment phrases) purely as a CONVENIENCE check —
 * e.g. to reduce the chance a family member's voice triggers RAMESH AI by
 * accident. It is explicitly NOT secure biometric authentication:
 *   - It is not resistant to recordings, impersonation, or spoofing.
 *   - It has not been evaluated for false-accept/false-reject rates.
 *   - It must never gate anything sensitive on its own.
 *
 * For any risky/irreversible action (sending a message, calls, deleting data,
 * account changes — see [com.rameshai.tools.ToolDefinitions] requiresConfirmation
 * flags), the app requires an explicit in-app confirmation and, where the
 * action is sensitive enough, real Android device authentication
 * (BiometricPrompt / device credential), never this voice check alone.
 */
class VoiceProfileVerifier(context: Context) {

    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context, "voice_profile_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isEnrolled(): Boolean = prefs.getBoolean(KEY_ENROLLED, false)

    /** Store a coarse feature vector derived from enrollment phrases (not raw audio). */
    fun enroll(featureVector: FloatArray) {
        prefs.edit()
            .putBoolean(KEY_ENROLLED, true)
            .putString(KEY_FEATURES, featureVector.joinToString(","))
            .apply()
    }

    fun clearEnrollment() {
        prefs.edit().clear().apply()
    }

    /**
     * Convenience-only similarity check. Returns a confidence 0f..1f. Callers
     * MUST treat this as a hint, never as authentication proof — see class doc.
     */
    fun similarityScore(sampleFeatureVector: FloatArray): Float {
        val storedCsv = prefs.getString(KEY_FEATURES, null) ?: return 0f
        val stored = storedCsv.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
        if (stored.isEmpty() || stored.size != sampleFeatureVector.size) return 0f

        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in stored.indices) {
            dot += stored[i] * sampleFeatureVector[i]
            normA += stored[i] * stored[i]
            normB += sampleFeatureVector[i] * sampleFeatureVector[i]
        }
        if (normA == 0f || normB == 0f) return 0f
        val cosine = dot / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble())).toFloat()
        return cosine.coerceIn(0f, 1f)
    }

    companion object {
        private const val KEY_ENROLLED = "enrolled"
        private const val KEY_FEATURES = "features"
    }
}
