package com.chris.uwa_social.presentation

import com.chris.uwa_social.presentation.feed.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class TimeFormatterTest {

    private val fixedNow = Instant.parse("2026-09-24T18:00:00Z")

    @Test
    fun `formatRelativeTime returns Just now for events under one minute ago`() {
        val thirtySecondsAgo = fixedNow.minus(30, ChronoUnit.SECONDS)
        assertEquals("Just now", TimeFormatter.formatRelativeTime(thirtySecondsAgo, fixedNow))
    }

    @Test
    fun `formatRelativeTime returns minutes for events under one hour ago`() {
        val fiveMinutesAgo = fixedNow.minus(5, ChronoUnit.MINUTES)
        assertEquals("5m", TimeFormatter.formatRelativeTime(fiveMinutesAgo, fixedNow))
    }

    @Test
    fun `formatRelativeTime returns hours for events under 24 hours ago`() {
        val twoHoursAgo = fixedNow.minus(2, ChronoUnit.HOURS)
        assertEquals("2h", TimeFormatter.formatRelativeTime(twoHoursAgo, fixedNow))
    }

    @Test
    fun `formatRelativeTime returns Yesterday for events 1 day ago`() {
        val oneDayAgo = fixedNow.minus(1, ChronoUnit.DAYS)
        assertEquals("Yesterday", TimeFormatter.formatRelativeTime(oneDayAgo, fixedNow))
    }

    @Test
    fun `formatRelativeTime returns days for events between 2 and 6 days ago`() {
        val threeDaysAgo = fixedNow.minus(3, ChronoUnit.DAYS)
        assertEquals("3d", TimeFormatter.formatRelativeTime(threeDaysAgo, fixedNow))
    }

    @Test
    fun `formatRelativeTime returns Just now for future timestamps`() {
        val future = fixedNow.plus(10, ChronoUnit.MINUTES)
        assertEquals("Just now", TimeFormatter.formatRelativeTime(future, fixedNow))
    }
}
