package com.rameshai.news

import com.rameshai.config.RuntimeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Example concrete [NewsProvider] using NewsAPI.org-style REST endpoint. Fully
 * driven by [RuntimeConfig.newsApiKey] / newsProvider — never hard-codes
 * today's news, and always surfaces publish dates/sources so the user can judge
 * freshness themselves.
 */
class NewsApiProvider(private val config: RuntimeConfig) : NewsProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    override suspend fun fetchHeadlines(topic: String?): List<NewsHeadline> = withContext(Dispatchers.IO) {
        if (config.newsApiKey.isBlank()) return@withContext emptyList()

        val query = if (topic.isNullOrBlank()) "" else "&q=${topic.trim()}"
        val url = "https://newsapi.org/v2/top-headlines?country=in$query&apiKey=${config.newsApiKey}"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val json = JSONObject(response.body?.string().orEmpty())
            val articles = json.optJSONArray("articles") ?: return@withContext emptyList()
            (0 until minOf(articles.length(), 5)).map { i ->
                val a = articles.getJSONObject(i)
                NewsHeadline(
                    title = a.optString("title"),
                    source = a.optJSONObject("source")?.optString("name").orEmpty(),
                    publishedAt = a.optString("publishedAt"),
                    url = a.optString("url")
                )
            }
        }
    }
}
