package com.nowaste.app.domain

import java.time.LocalDate

const val BatchPhotoPendingNamePrefix = "待补全食品"
const val BatchPhotoPendingNote = "连续拍照添加，待补全名称和到期时间。"

data class FoodItemInput(
    val name: String,
    val expiryDate: LocalDate,
    val categoryTag: String,
    val note: String,
    val photoUri: String = "",
    val quantity: Int = 1,
    val productionDate: LocalDate? = null,
    val shelfLifeAmount: Long? = null,
    val shelfLifeUnit: ShelfLifeUnit? = null,
    val reminderDaysBeforeExpiry: Int? = null,
)
