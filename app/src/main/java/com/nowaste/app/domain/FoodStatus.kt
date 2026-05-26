package com.nowaste.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class FoodStatus {
    Safe,
    NearExpiry,
    Expired,
}

fun calculateFoodStatus(
    expiryDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    nearExpiryDays: Int = 3,
): FoodStatus {
    val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)
    return when {
        daysUntilExpiry < 0 -> FoodStatus.Expired
        daysUntilExpiry <= nearExpiryDays.coerceAtLeast(1) -> FoodStatus.NearExpiry
        else -> FoodStatus.Safe
    }
}
