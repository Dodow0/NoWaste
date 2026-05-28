package com.nowaste.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.domain.ShelfLifeUnit
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
                photoUri = "content://com.nowaste.app.fileprovider/food_photos/milk.jpg",
                productionDate = LocalDate.of(2026, 5, 1),
                shelfLifeAmount = 30L,
                shelfLifeUnit = ShelfLifeUnit.DAYS,
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
        assertEquals(LocalDate.of(2026, 5, 1), database.foodItemDao().getById(laterId)?.productionDate)
        assertEquals(30L, database.foodItemDao().getById(laterId)?.shelfLifeAmount)
        assertEquals(ShelfLifeUnit.DAYS, database.foodItemDao().getById(laterId)?.shelfLifeUnit)

        repository.updateFoodItem(
            earlierId,
            FoodItemInput(
                name = "Green apple",
                expiryDate = LocalDate.of(2026, 5, 27),
                categoryTag = "Fruit",
                note = "Crisp",
                photoUri = "content://com.nowaste.app.fileprovider/food_photos/apple.jpg",
                productionDate = LocalDate.of(2026, 5, 20),
                shelfLifeAmount = 6L,
                shelfLifeUnit = ShelfLifeUnit.MONTHS,
            ),
        )
        assertEquals("Green apple", database.foodItemDao().getById(earlierId)?.name)
        assertEquals(
            "content://com.nowaste.app.fileprovider/food_photos/apple.jpg",
            database.foodItemDao().getById(earlierId)?.photoUri,
        )
        assertEquals(LocalDate.of(2026, 5, 20), database.foodItemDao().getById(earlierId)?.productionDate)
        assertEquals(6L, database.foodItemDao().getById(earlierId)?.shelfLifeAmount)
        assertEquals(ShelfLifeUnit.MONTHS, database.foodItemDao().getById(earlierId)?.shelfLifeUnit)

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

    @Test
    fun reminderCheckUsesItemSpecificReminderDaysWhenSet() = runTest {
        val today = LocalDate.of(2026, 5, 25)
        repository.addFoodItem(
            FoodItemInput(
                name = "Custom",
                expiryDate = today.plusDays(5),
                categoryTag = "",
                note = "",
                reminderDaysBeforeExpiry = 5,
            ),
        )
        repository.addFoodItem(FoodItemInput("Default", today.plusDays(5), "", ""))

        val reminders = repository.getItemsForReminderCheck(today, nearExpiryDays = 3)

        assertEquals(listOf("Custom"), reminders.map { it.name })
    }
}
