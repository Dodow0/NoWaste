package com.nowaste.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ExpiryProgress(
    val fractionElapsed: Float,
    val remainingDays: Long,
    val totalDays: Long,
)

fun calculateExpiryProgress(
    createdDate: LocalDate,
    expiryDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): ExpiryProgress {
    val totalDays = ChronoUnit.DAYS.between(createdDate, expiryDate).coerceAtLeast(1)
    val elapsedDays = ChronoUnit.DAYS.between(createdDate, today).coerceIn(0, totalDays)
    val remainingDays = ChronoUnit.DAYS.between(today, expiryDate)
    return ExpiryProgress(
        fractionElapsed = (elapsedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f),
        remainingDays = remainingDays,
        totalDays = totalDays,
    )
}
