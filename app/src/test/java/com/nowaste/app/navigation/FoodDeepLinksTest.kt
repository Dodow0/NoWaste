package com.nowaste.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDeepLinksTest {
    @Test
    fun foodItemDeepLinkUsesNotificationRoute() {
        assertEquals("nowaste://foods/42", FoodDeepLinks.foodItem(42))
    }
}
