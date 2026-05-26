package com.nowaste.app.domain

import java.time.LocalDate

fun reminderExpiryDatesFor(date: LocalDate, nearExpiryDays: Int = 1): List<LocalDate> =
    listOf(
        date.plusDays(nearExpiryDays.coerceAtLeast(1).toLong()),
        date,
        date.minusDays(1),
    )
