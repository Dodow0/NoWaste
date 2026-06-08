package com.nowaste.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class ReminderRulesTest {
    @Test
    fun reminderDatesIncludeYesterdayTodayAndConfiguredFutureWindow() {
        val today = LocalDate.of(2026, 5, 25)

        assertEquals(
            listOf(
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 5, 28),
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 5, 30),
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 6, 1),
            ),
            reminderExpiryDatesFor(today, nearExpiryDays = 7),
        )
    }

    @Test
    fun reminderDatesExcludeDayAfterConfiguredWindow() {
        val today = LocalDate.of(2026, 5, 25)

        assertFalse(LocalDate.of(2026, 6, 2) in reminderExpiryDatesFor(today, nearExpiryDays = 7))
    }
}
