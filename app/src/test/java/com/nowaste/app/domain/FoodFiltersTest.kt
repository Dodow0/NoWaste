package com.nowaste.app.domain

import com.nowaste.app.data.FoodItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FoodFiltersTest {
    private val items = listOf(
        item(id = 1, name = "牛奶", category = "乳制品", note = "早餐"),
        item(id = 2, name = "苹果", category = "水果", note = "红富士"),
        item(id = 3, name = "面包", category = "主食", note = "全麦"),
    )

    @Test
    fun filtersByQueryAcrossNameAndNote() {
        assertEquals(listOf("牛奶"), filterFoodItems(items, "早餐", null).map { it.name })
        assertEquals(listOf("苹果"), filterFoodItems(items, "苹果", null).map { it.name })
    }

    @Test
    fun filtersByCategory() {
        assertEquals(listOf("面包"), filterFoodItems(items, "", "主食").map { it.name })
    }

    private fun item(
        id: Long,
        name: String,
        category: String,
        note: String,
    ): FoodItem =
        FoodItem(
            id = id,
            name = name,
            expiryDate = LocalDate.of(2026, 5, 25),
            categoryTag = category,
            note = note,
            photoUri = "",
            createdAt = LocalDateTime.of(2026, 5, 20, 9, 0),
            updatedAt = LocalDateTime.of(2026, 5, 20, 9, 0),
        )
}
