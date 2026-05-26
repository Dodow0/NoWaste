package com.nowaste.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReminderRulesTest {
    @Test
    fun reminderDatesIncludeConfiguredThresholdTodayAndYesterday() {
        val today = LocalDate.of(2026, 5, 25)

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 24),
            ),
            reminderExpiryDatesFor(today, nearExpiryDays = 7),
        )
    }
}
