package it.agesci.scoutalert.notifications

import java.time.LocalDate

internal fun matchesBirthday(
    date: LocalDate,
    day: Int,
    month: Int,
    unit: String?,
    enabledUnits: Set<String>
): Boolean =
    day == date.dayOfMonth &&
        month == date.monthValue &&
        (enabledUnits.isEmpty() || unit in enabledUnits)
