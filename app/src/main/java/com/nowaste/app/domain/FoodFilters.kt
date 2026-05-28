package com.nowaste.app.domain

import com.nowaste.app.data.FoodItem

fun filterFoodItems(
    items: List<FoodItem>,
    query: String,
    selectedCategory: String?,
): List<FoodItem> {
    val normalizedQuery = query.trim().lowercase()
    return items.filter { item ->
        val matchesQuery = normalizedQuery.isBlank() ||
            item.name.lowercase().contains(normalizedQuery) ||
            item.note.lowercase().contains(normalizedQuery)
        val matchesCategory = selectedCategory == null || item.categoryTag == selectedCategory
        matchesQuery && matchesCategory
    }
}
