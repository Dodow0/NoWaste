package com.nowaste.app.ui

import com.nowaste.app.data.FoodItem
import com.nowaste.app.domain.FoodStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FoodListItemStatusTest {
    @Test
    fun itemSpecificReminderDaysOverrideGlobalNearExpiryStatus() {
        val today = LocalDate.of(2026, 5, 25)
        val item = foodItem(
            expiryDate = today.plusDays(5),
            reminderDaysBeforeExpiry = 5,
        )

        assertEquals(
            FoodStatus.NearExpiry,
            calculateFoodListItemStatus(
                item = item,
                today = today,
                globalNearExpiryDays = 3,
            ),
        )
    }

    @Test
    fun globalNearExpiryDaysUsedWhenItemHasNoOverride() {
        val today = LocalDate.of(2026, 5, 25)
        val item = foodItem(
            expiryDate = today.plusDays(5),
            reminderDaysBeforeExpiry = null,
        )

        assertEquals(
            FoodStatus.Safe,
            calculateFoodListItemStatus(
                item = item,
                today = today,
                globalNearExpiryDays = 3,
            ),
        )
    }

    private fun foodItem(
        expiryDate: LocalDate,
        reminderDaysBeforeExpiry: Int?,
    ): FoodItem =
        FoodItem(
            id = 1,
            name = "Milk",
            expiryDate = expiryDate,
            reminderDaysBeforeExpiry = reminderDaysBeforeExpiry,
            categoryTag = "",
            note = "",
            photoUri = "",
            createdAt = LocalDateTime.of(2026, 5, 20, 9, 0),
            updatedAt = LocalDateTime.of(2026, 5, 20, 9, 0),
        )
}
