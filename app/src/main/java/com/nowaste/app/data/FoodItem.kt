package com.nowaste.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nowaste.app.domain.ShelfLifeUnit
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val expiryDate: LocalDate,
    val productionDate: LocalDate? = null,
    val shelfLifeAmount: Long? = null,
    val shelfLifeUnit: ShelfLifeUnit? = null,
    val reminderDaysBeforeExpiry: Int? = null,
    val categoryTag: String,
    val note: String,
    val photoUri: String,
    val quantity: Int = 1,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
