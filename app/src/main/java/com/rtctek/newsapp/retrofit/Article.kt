package com.rtctek.newsapp.retrofit

/**
 * NewsAPI article payload.
 *
 * All fields are nullable with defaults: Gson bypasses Kotlin constructors via
 * reflection, so the API can hand back `null` for any field even when the type
 * is declared non-null. Declaring nullability explicitly avoids hidden NPEs.
 */
data class Article(
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val urlToImage: String? = null,
    val publishedAt: String? = null,
    val content: String? = null,
    val source: Source? = null,
)
