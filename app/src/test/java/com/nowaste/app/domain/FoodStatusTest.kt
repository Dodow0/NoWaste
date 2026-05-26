package com.nowaste.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FoodStatusTest {
    private val today: LocalDate = LocalDate.of(2026, 5, 25)

    @Test
    fun safeWhenExpiryIsOutsideConfiguredWindow() {
        assertEquals(FoodStatus.Safe, calculateFoodStatus(today.plusDays(2), today, nearExpiryDays = 1))
    }

    @Test
    fun nearExpiryUsesConfiguredThreshold() {
        assertEquals(FoodStatus.NearExpiry, calculateFoodStatus(today.plusDays(7), today, nearExpiryDays = 7))
    }

    @Test
    fun nearExpiryKeepsThreeDayDefaultBehavior() {
        assertEquals(FoodStatus.NearExpiry, calculateFoodStatus(today.plusDays(3), today, nearExpiryDays = 3))
        assertEquals(FoodStatus.Safe, calculateFoodStatus(today.plusDays(4), today, nearExpiryDays = 3))
    }

    @Test
    fun nearExpiryWhenExpiryIsToday() {
        assertEquals(FoodStatus.NearExpiry, calculateFoodStatus(today, today, nearExpiryDays = 1))
    }

    @Test
    fun expiredWhenExpiryIsBeforeToday() {
        assertEquals(FoodStatus.Expired, calculateFoodStatus(today.minusDays(1), today, nearExpiryDays = 7))
    }
}
