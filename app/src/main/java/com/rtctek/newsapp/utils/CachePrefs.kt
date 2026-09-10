package com.rtctek.newsapp.utils

import android.content.Context

/**
 * Tiny SharedPreferences-backed record of when the remote news cache was last
 * successfully refreshed. Keeps us within NewsAPI's free-tier request limits
 * by only re-fetching after [CACHE_TTL_MS] has elapsed (or on explicit refresh).
 */
object CachePrefs {

    private const val PREFS_NAME = "news_cache_prefs"
    private const val KEY_LAST_REFRESH = "last_refresh_at"

    /** How long a fetched category is considered fresh (30 minutes). */
    const val CACHE_TTL_MS: Long = 30L * 60L * 1000L

    fun isStale(context: Context): Boolean {
        val last = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_REFRESH, 0L)
        return System.currentTimeMillis() - last >= CACHE_TTL_MS
    }

    fun markFresh(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_REFRESH, System.currentTimeMillis())
            .apply()
    }
}
