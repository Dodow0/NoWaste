package com.nowaste.app.navigation

object FoodDeepLinks {
    const val FoodItemPattern = "nowaste://foods/{itemId}"

    fun foodItem(itemId: Long): String =
        "nowaste://foods/$itemId"
}
