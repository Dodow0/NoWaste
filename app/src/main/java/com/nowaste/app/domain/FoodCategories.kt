package com.nowaste.app.domain

val DefaultFoodCategories = listOf(
    "蔬菜",
    "水果",
    "肉类",
    "乳制品",
    "主食",
    "零食",
    "饮料",
    "熟食",
    "调味品",
    "其他",
)

fun normalizeCategory(rawCategory: String?): String? {
    val raw = rawCategory.orEmpty().trim()
    if (raw.isBlank()) return null

    val lower = raw.lowercase()
    return when {
        listOf("vegetable", "vegetables", "蔬菜").any(lower::contains) -> "蔬菜"
        listOf("fruit", "fruits", "水果").any(lower::contains) -> "水果"
        listOf("meat", "pork", "beef", "chicken", "fish", "seafood", "肉").any(lower::contains) -> "肉类"
        listOf("dairy", "dair", "milk", "cheese", "yogurt", "乳", "奶").any(lower::contains) -> "乳制品"
        listOf("cereal", "rice", "noodle", "bread", "pasta", "主食", "谷物").any(lower::contains) -> "主食"
        listOf("snack", "chips", "cookie", "chocolate", "零食").any(lower::contains) -> "零食"
        listOf("beverage", "drink", "juice", "water", "tea", "coffee", "饮料").any(lower::contains) -> "饮料"
        listOf("prepared", "meal", "ready", "熟食").any(lower::contains) -> "熟食"
        listOf("condiment", "sauce", "spice", "seasoning", "调味").any(lower::contains) -> "调味品"
        else -> null
    }
}
