package com.nowaste.app.domain

import java.time.LocalDate

fun reminderExpiryDatesFor(date: LocalDate, nearExpiryDays: Int = 1): List<LocalDate> =
    buildList {
        add(date.minusDays(1))
        add(date)
        for (offset in 1..nearExpiryDays.coerceAtLeast(1)) {
            add(date.plusDays(offset.toLong()))
        }
    }
