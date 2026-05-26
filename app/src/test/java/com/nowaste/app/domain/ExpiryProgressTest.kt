package com.nowaste.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpiryProgressTest {
    @Test
    fun progressReflectsElapsedShelfLifeAndRemainingDays() {
        val progress = calculateExpiryProgress(
            createdDate = LocalDate.of(2026, 5, 1),
            expiryDate = LocalDate.of(2026, 5, 31),
            today = LocalDate.of(2026, 5, 16),
        )

        assertEquals(0.5f, progress.fractionElapsed, 0.001f)
        assertEquals(15L, progress.remainingDays)
        assertEquals(30L, progress.totalDays)
    }

    @Test
    fun progressCapsAtFullWhenExpired() {
        val progress = calculateExpiryProgress(
            createdDate = LocalDate.of(2026, 5, 1),
            expiryDate = LocalDate.of(2026, 5, 10),
            today = LocalDate.of(2026, 5, 13),
        )

        assertEquals(1f, progress.fractionElapsed, 0.001f)
        assertEquals(-3L, progress.remainingDays)
    }

    @Test
    fun sameDayShelfLifeUsesOneDayMinimum() {
        val progress = calculateExpiryProgress(
            createdDate = LocalDate.of(2026, 5, 1),
            expiryDate = LocalDate.of(2026, 5, 1),
            today = LocalDate.of(2026, 5, 1),
        )

        assertEquals(0f, progress.fractionElapsed, 0.001f)
        assertEquals(0L, progress.remainingDays)
        assertEquals(1L, progress.totalDays)
    }
}
