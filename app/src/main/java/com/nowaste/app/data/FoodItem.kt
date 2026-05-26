package com.nowaste.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val expiryDate: LocalDate,
    val categoryTag: String,
    val note: String,
    val barcodeValue: String,
    val photoUri: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
