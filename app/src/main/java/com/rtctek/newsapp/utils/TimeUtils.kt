package com.rtctek.newsapp.utils

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Date/time helpers for the ISO-8601 timestamps returned by NewsAPI.
 *
 * Everything is written against `java.time` (enabled on all API levels via
 * core library desugaring) and kept free of Android framework calls so the
 * functions stay unit-testable on the JVM.
 */
object TimeUtils {

    private val dateOnlyFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneOffset.UTC)

    /**
     * Parses an ISO-8601 timestamp (e.g. `2024-05-01T10:00:00Z`,
     * `2024-05-01T10:00:00+05:30` or `2024-05-01T10:00:00`) to an [Instant],
     * or returns `null` when the input is null/blank/unparseable.
     */
    fun parsePublishedAt(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(raw).toInstant()
        } catch (_: Exception) {
            try {
                Instant.parse(raw)
            } catch (_: Exception) {
                try {
                    // Fallback for timestamps without a zone — assume UTC.
                    java.time.LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Compact, human-friendly "time ago" label ("Just now", "5m ago",
     * "3h ago", "2d ago") falling back to an absolute date for anything
     * older than a week. Returns `null` when [raw] can't be parsed.
     */
    fun relativeTime(raw: String?, now: Instant = Instant.now()): String? {
        val published = parsePublishedAt(raw) ?: return null
        val elapsed = Duration.between(published, now)
        if (elapsed.isNegative) return "Just now" // clock skew / future timestamp
        return when {
            elapsed.toMinutes() < 1 -> "Just now"
            elapsed.toHours() < 1 -> "${elapsed.toMinutes()}m ago"
            elapsed.toDays() < 1 -> "${elapsed.toHours()}h ago"
            elapsed.toDays() < 7 -> "${elapsed.toDays()}d ago"
            else -> formatDate(raw)
        }
    }

    /** Absolute date label such as `1 May 2024`, or `null` when unparseable. */
    fun formatDate(raw: String?): String? {
        val published = parsePublishedAt(raw) ?: return null
        return dateOnlyFormatter.format(published)
    }
}
