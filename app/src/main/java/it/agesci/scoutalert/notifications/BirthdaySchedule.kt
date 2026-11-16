package it.agesci.scoutalert.notifications

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

private val CHECK_TIME: LocalTime = LocalTime.of(7, 30)

internal fun nextBirthdayCheck(now: ZonedDateTime): ZonedDateTime {
    val today = now.toLocalDate().atTime(CHECK_TIME).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate()
        .plusDays(1)
        .atTime(CHECK_TIME)
        .atZone(now.zone)
}

internal fun delayUntilNextBirthdayCheck(now: ZonedDateTime): Duration =
    Duration.between(now, nextBirthdayCheck(now)).coerceAtLeast(Duration.ZERO)
