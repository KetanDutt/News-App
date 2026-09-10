package com.rtctek.newsapp.utils

import androidx.annotation.StringRes
import com.rtctek.newsapp.R

/**
 * App-wide constants: category configuration, intent extras and cache settings.
 */
object Constants {

    // ── NewsAPI query values ────────────────────────────────────────
    const val GENERAL = "general"
    const val BUSINESS = "business"
    const val ENTERTAINMENT = "entertainment"
    const val SCIENCE = "science"
    const val SPORTS = "sports"
    const val TECHNOLOGY = "technology"
    const val HEALTH = "health"
    const val SAVED_CATEGORY = "saved"
    const val SEARCH_CATEGORY = "search"

    /** Country passed to the top-headlines endpoint. */
    const val COUNTRY = "in"

    /** Categories shown as tabs on the home screen, in order. */
    val TAB_CATEGORIES: List<TabCategory> = listOf(
        TabCategory(R.string.tab_home, GENERAL),
        TabCategory(R.string.tab_business, BUSINESS),
        TabCategory(R.string.tab_entertainment, ENTERTAINMENT),
        TabCategory(R.string.tab_science, SCIENCE),
        TabCategory(R.string.tab_sports, SPORTS),
        TabCategory(R.string.tab_tech, TECHNOLOGY),
        TabCategory(R.string.tab_health, HEALTH),
    )

    // ── UI counts ───────────────────────────────────────────────────
    const val TOP_HEADLINES_COUNT = 5

    // ── Networking ──────────────────────────────────────────────────
    const val SEARCH_PAGE_SIZE = 20

    // ── Persistence ─────────────────────────────────────────────────
    const val DATABASE_NAME = "news_database"

    // ── Intent extras ───────────────────────────────────────────────
    const val EXTRA_NEWS_TITLE = "com.rtctek.newsapp.extra.NEWS_TITLE"
    const val EXTRA_NEWS_IMAGE_URL = "com.rtctek.newsapp.extra.NEWS_IMAGE_URL"
    const val EXTRA_NEWS_DESCRIPTION = "com.rtctek.newsapp.extra.NEWS_DESCRIPTION"
    const val EXTRA_NEWS_URL = "com.rtctek.newsapp.extra.NEWS_URL"
    const val EXTRA_NEWS_SOURCE = "com.rtctek.newsapp.extra.NEWS_SOURCE"
    const val EXTRA_NEWS_PUBLICATION_TIME = "com.rtctek.newsapp.extra.NEWS_PUBLICATION_TIME"
    const val EXTRA_NEWS_CONTENT = "com.rtctek.newsapp.extra.NEWS_CONTENT"
}

/** A home-screen tab: a user-facing [labelRes] plus the NewsAPI [query] value. */
data class TabCategory(
    @StringRes val labelRes: Int,
    val query: String,
)
