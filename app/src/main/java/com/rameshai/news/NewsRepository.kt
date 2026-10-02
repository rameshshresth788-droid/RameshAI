package com.rameshai.news

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.rameshai.config.RuntimeConfig

/**
 * Bridges [NewsProvider] output to a spoken-friendly summary, and enforces the
 * "internet unavailable" and "not configured" messaging required by the spec
 * instead of silently failing or fabricating headlines.
 */
class NewsRepository(private val context: Context, private val config: RuntimeConfig) {

    sealed class Result {
        data class Success(val summary: String) : Result()
        object NoInternet : Result()
        object NotConfigured : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun fetchNews(topic: String?): Result {
        if (!isOnline()) return Result.NoInternet
        if (config.newsProvider.equals("none", ignoreCase = true)) return Result.NotConfigured

        val provider: NewsProvider = when (config.newsProvider.lowercase()) {
            "newsapi" -> NewsApiProvider(config)
            else -> return Result.NotConfigured
        }

        return try {
            val headlines = provider.fetchHeadlines(topic)
            if (headlines.isEmpty()) Result.Error("Abhi koi news nahi mili.")
            else Result.Success(
                headlines.joinToString(" | ") { "${it.title} (${it.source}, ${it.publishedAt})" }
            )
        } catch (e: Exception) {
            Result.Error("News fetch karte waqt error aaya.")
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
