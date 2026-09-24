package com.chris.uwa_social.presentation.feed.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TimeFormatter {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .withZone(ZoneId.systemDefault())

    fun formatRelativeTime(
        instant: Instant,
        now: Instant = Instant.now()
    ): String {
        if (instant.isAfter(now)) {
            return "Just now"
        }

        val seconds = ChronoUnit.SECONDS.between(instant, now)
        if (seconds < 60) {
            return "Just now"
        }

        val minutes = ChronoUnit.MINUTES.between(instant, now)
        if (minutes < 60) {
            return "${minutes}m"
        }

        val hours = ChronoUnit.HOURS.between(instant, now)
        if (hours < 24) {
            return "${hours}h"
        }

        val days = ChronoUnit.DAYS.between(instant, now)
        return when {
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d"
            else -> dateFormatter.format(instant)
        }
    }
}
