package com.nowaste.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nowaste.app.domain.FoodItemInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class FoodRepositoryRoomTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FoodRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FoodRepository(
            dao = database.foodItemDao(),
            now = { LocalDateTime.of(2026, 5, 25, 9, 0) },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addUpdateDeleteAndSortByExpiry() = runTest {
        val laterId = repository.addFoodItem(
            FoodItemInput(
                name = "Milk",
                expiryDate = LocalDate.of(2026, 5, 30),
                categoryTag = "Dairy",
                note = "Opened",
                barcodeValue = "690000000001",
                photoUri = "content://com.nowaste.app.fileprovider/food_photos/milk.jpg",
            ),
        )
        val earlierId = repository.addFoodItem(
            FoodItemInput(
                name = "Apple",
                expiryDate = LocalDate.of(2026, 5, 26),
                categoryTag = "Fruit",
                note = "",
            ),
        )

        val sorted = repository.observeFoodItemsSortedByExpiry().first()
        assertEquals(listOf(earlierId, laterId), sorted.map { it.id })
        assertEquals(
            "content://com.nowaste.app.fileprovider/food_photos/milk.jpg",
            database.foodItemDao().getById(laterId)?.photoUri,
        )

        repository.updateFoodItem(
            earlierId,
            FoodItemInput(
                name = "Green apple",
                expiryDate = LocalDate.of(2026, 5, 27),
                categoryTag = "Fruit",
                note = "Crisp",
                barcodeValue = "690000000002",
                photoUri = "content://com.nowaste.app.fileprovider/food_photos/apple.jpg",
            ),
        )
        assertEquals("Green apple", database.foodItemDao().getById(earlierId)?.name)
        assertEquals("690000000002", database.foodItemDao().getById(earlierId)?.barcodeValue)
        assertEquals(
            "content://com.nowaste.app.fileprovider/food_photos/apple.jpg",
            database.foodItemDao().getById(earlierId)?.photoUri,
        )

        repository.deleteFoodItem(laterId)
        val afterDelete = repository.observeFoodItemsSortedByExpiry().first()
        assertEquals(listOf(earlierId), afterDelete.map { it.id })
    }

    @Test
    fun reminderCheckReturnsThresholdTodayAndYesterdayOnly() = runTest {
        val today = LocalDate.of(2026, 5, 25)
        repository.addFoodItem(FoodItemInput("Threshold", today.plusDays(7), "", ""))
        repository.addFoodItem(FoodItemInput("Today", today, "", ""))
        repository.addFoodItem(FoodItemInput("Yesterday", today.minusDays(1), "", ""))
        repository.addFoodItem(FoodItemInput("Later", today.plusDays(2), "", ""))

        val reminders = repository.getItemsForReminderCheck(today, nearExpiryDays = 7)

        assertEquals(listOf("Yesterday", "Today", "Threshold"), reminders.map { it.name })
        assertTrue(reminders.none { it.name == "Later" })
    }
}
