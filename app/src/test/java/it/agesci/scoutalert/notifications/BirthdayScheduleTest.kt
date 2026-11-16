package it.agesci.scoutalert.notifications

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BirthdayScheduleTest {
    private val zone = ZoneId.of("Europe/Rome")

    @Test
    fun `uses today at 0730 before target time`() {
        val now = ZonedDateTime.of(2026, 11, 16, 6, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 11, 16, 7, 30, 0, 0, zone),
            nextBirthdayCheck(now)
        )
    }

    @Test
    fun `rolls to tomorrow after target time`() {
        val now = ZonedDateTime.of(2026, 11, 16, 8, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 11, 17, 7, 30, 0, 0, zone),
            nextBirthdayCheck(now)
        )
    }

    @Test
    fun `keeps local 0730 across daylight saving transition`() {
        val now = ZonedDateTime.of(2026, 3, 28, 8, 0, 0, 0, zone)
        val next = nextBirthdayCheck(now)

        assertEquals(7, next.hour)
        assertEquals(30, next.minute)
        assertEquals(Duration.ofHours(22).plusMinutes(30), Duration.between(now, next))
    }
}
