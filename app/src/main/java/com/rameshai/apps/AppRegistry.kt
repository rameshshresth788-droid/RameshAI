package com.rameshai.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(val label: String, val packageName: String)

/**
 * Discovers installed, launchable apps and lets [com.rameshai.tools.ToolExecutor]
 * resolve a spoken app name ("YouTube kholo") to a real package — instead of
 * hard-coding a handful of packages, every launchable app on the device is
 * indexed and matched by name.
 */
class AppRegistry(private val context: Context) {

    private var cache: List<InstalledApp> = emptyList()

    fun refresh(): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        cache = resolved.map {
            InstalledApp(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        return cache
    }

    fun all(): List<InstalledApp> = cache.ifEmpty { refresh() }

    /** Best-effort fuzzy match: exact label, then contains, then package-name contains. */
    fun findByName(spokenName: String): InstalledApp? {
        val apps = all()
        val query = spokenName.trim().lowercase()
        if (query.isBlank()) return null

        apps.firstOrNull { it.label.lowercase() == query }?.let { return it }
        apps.firstOrNull { it.label.lowercase().contains(query) }?.let { return it }
        apps.firstOrNull { it.packageName.lowercase().contains(query.replace(" ", "")) }?.let { return it }
        return null
    }
}
