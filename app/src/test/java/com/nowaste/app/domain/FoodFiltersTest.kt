package com.nowaste.app.domain

import com.nowaste.app.data.FoodItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FoodFiltersTest {
    private val items = listOf(
        item(name = "牛奶", category = "乳制品", barcode = "6901", note = "早餐"),
        item(name = "苹果", category = "水果", barcode = "6902", note = "红富士"),
        item(name = "面包", category = "主食", barcode = "6903", note = "全麦"),
    )

    @Test
    fun filtersByQueryAcrossNameBarcodeAndNote() {
        assertEquals(listOf("牛奶"), filterFoodItems(items, "早餐", null).map { it.name })
        assertEquals(listOf("苹果"), filterFoodItems(items, "6902", null).map { it.name })
    }

    @Test
    fun filtersByCategory() {
        assertEquals(listOf("面包"), filterFoodItems(items, "", "主食").map { it.name })
    }

    private fun item(
        name: String,
        category: String,
        barcode: String,
        note: String,
    ): FoodItem =
        FoodItem(
            id = barcode.takeLast(1).toLong(),
            name = name,
            expiryDate = LocalDate.of(2026, 5, 25),
            categoryTag = category,
            note = note,
            barcodeValue = barcode,
            photoUri = "",
            createdAt = LocalDateTime.of(2026, 5, 20, 9, 0),
            updatedAt = LocalDateTime.of(2026, 5, 20, 9, 0),
        )
}
