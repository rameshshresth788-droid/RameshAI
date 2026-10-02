package com.rameshai.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

/**
 * Lightweight, non-sensitive UI-state prefs (first-run completion, owner display
 * name for greetings). Deliberately separate from [com.rameshai.config.ConfigRepository],
 * which holds the encrypted, AI-relevant runtime config (API keys etc.) — this
 * keeps the two concerns from being conflated in one file.
 */
class OnboardingPrefs(private val context: Context) {
    private val keyFirstRunDone = booleanPreferencesKey("first_run_done")
    private val keyOwnerName = stringPreferencesKey("owner_name")

    val firstRunDone: Flow<Boolean> = context.onboardingDataStore.data.map { it[keyFirstRunDone] ?: false }
    val ownerName: Flow<String> = context.onboardingDataStore.data.map { it[keyOwnerName] ?: "RAMESH" }

    suspend fun markFirstRunDone() {
        context.onboardingDataStore.edit { it[keyFirstRunDone] = true }
    }

    suspend fun setOwnerName(name: String) {
        context.onboardingDataStore.edit { it[keyOwnerName] = name }
    }
}
