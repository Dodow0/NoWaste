package com.nowaste.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ReminderSchedulerTest {
    @Test
    fun initialDelayUsesTodayWhenReminderTimeIsStillAhead() {
        val now = LocalDateTime.of(2026, 5, 25, 8, 30)

        assertEquals(Duration.ofMinutes(30), ReminderScheduler.initialDelay(9, 0, now))
    }

    @Test
    fun initialDelayUsesTomorrowWhenReminderTimeHasPassed() {
        val now = LocalDateTime.of(2026, 5, 25, 10, 0)

        assertEquals(Duration.ofHours(23), ReminderScheduler.initialDelay(9, 0, now))
    }
}
