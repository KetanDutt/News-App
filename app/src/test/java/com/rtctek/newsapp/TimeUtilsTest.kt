package com.rtctek.newsapp

import com.rtctek.newsapp.utils.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * JVM unit tests for [TimeUtils] (pure java.time, no Android framework).
 */
class TimeUtilsTest {

    private val now: Instant = Instant.parse("2024-05-01T12:00:00Z")

    // ── relativeTime ────────────────────────────────────────────────

    @Test
    fun `relativeTime returns null for null input`() {
        assertNull(TimeUtils.relativeTime(null, now))
    }

    @Test
    fun `relativeTime returns null for blank input`() {
        assertNull(TimeUtils.relativeTime("   ", now))
    }

    @Test
    fun `relativeTime returns null for garbage input`() {
        assertNull(TimeUtils.relativeTime("not-a-date", now))
    }

    @Test
    fun `relativeTime says just now for very recent stories`() {
        assertEquals("Just now", TimeUtils.relativeTime("2024-05-01T11:59:40Z", now))
    }

    @Test
    fun `relativeTime formats minutes`() {
        assertEquals("39m ago", TimeUtils.relativeTime("2024-05-01T11:21:00Z", now))
    }

    @Test
    fun `relativeTime formats hours`() {
        assertEquals("2h ago", TimeUtils.relativeTime("2024-05-01T10:00:00Z", now))
    }

    @Test
    fun `relativeTime formats days`() {
        assertEquals("3d ago", TimeUtils.relativeTime("2024-04-28T12:00:00Z", now))
    }

    @Test
    fun `relativeTime falls back to date after a week`() {
        assertEquals("20 Apr 2024", TimeUtils.relativeTime("2024-04-20T12:00:00Z", now))
    }

    @Test
    fun `relativeTime treats future timestamps as just now`() {
        assertEquals("Just now", TimeUtils.relativeTime("2024-05-01T15:00:00Z", now))
    }

    @Test
    fun `relativeTime parses offsets other than UTC`() {
        // 14:30+05:30 == 09:00Z → 3h ago
        assertEquals("3h ago", TimeUtils.relativeTime("2024-05-01T14:30:00+05:30", now))
    }

    @Test
    fun `relativeTime parses zone-less timestamps as UTC`() {
        assertEquals("2h ago", TimeUtils.relativeTime("2024-05-01T10:00:00", now))
    }

    // ── formatDate ──────────────────────────────────────────────────

    @Test
    fun `formatDate renders absolute date`() {
        assertEquals("1 May 2024", TimeUtils.formatDate("2024-05-01T10:00:00Z"))
    }

    @Test
    fun `formatDate returns null for unparseable input`() {
        assertNull(TimeUtils.formatDate(null))
        assertNull(TimeUtils.formatDate("yesterday"))
    }
}
