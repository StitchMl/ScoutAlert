package it.agesci.scoutalert.notifications

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayMatcherTest {
    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun `matches date when no unit filter exists`() {
        assertTrue(matchesBirthday(today, 16, 11, null, emptySet()))
    }

    @Test
    fun `matches only enabled unit when filter exists`() {
        assertTrue(matchesBirthday(today, 16, 11, "E/G", setOf("E/G")))
        assertFalse(matchesBirthday(today, 16, 11, "L/C", setOf("E/G")))
        assertFalse(matchesBirthday(today, 16, 11, null, setOf("E/G")))
    }

    @Test
    fun `rejects different day or month`() {
        assertFalse(matchesBirthday(today, 15, 11, "E/G", emptySet()))
        assertFalse(matchesBirthday(today, 16, 12, "E/G", emptySet()))
    }
}
