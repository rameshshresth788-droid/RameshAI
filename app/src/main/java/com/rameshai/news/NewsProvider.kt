package com.rameshai.news

/** Abstraction over any news backend so a new source can be added without touching callers. */
interface NewsProvider {
    suspend fun fetchHeadlines(topic: String?): List<NewsHeadline>
}

data class NewsHeadline(val title: String, val source: String, val publishedAt: String, val url: String)
