package com.nowaste.app.domain

import java.time.LocalDate

data class FoodItemInput(
    val name: String,
    val expiryDate: LocalDate,
    val categoryTag: String,
    val note: String,
    val barcodeValue: String = "",
    val photoUri: String = "",
)
