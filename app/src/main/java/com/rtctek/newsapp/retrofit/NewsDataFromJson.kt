package com.rtctek.newsapp.retrofit

/**
 * NewsAPI response envelope.
 * https://newsapi.org/docs/endpoints/top-headlines
 */
data class NewsDataFromJson(
    val status: String? = null,
    val totalResults: Int = 0,
    val articles: List<Article>? = null,
    /** Populated when status == "error". */
    val code: String? = null,
    val message: String? = null,
)
