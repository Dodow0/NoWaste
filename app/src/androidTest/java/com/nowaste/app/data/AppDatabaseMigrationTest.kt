package com.nowaste.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationFromOneToTwoKeepsExistingFoodItems() = runTest {
        createVersionOneDatabase()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DATABASE)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val migrated = database.foodItemDao().getById(1)

        assertNotNull(migrated)
        assertEquals("Milk", migrated?.name)
        assertEquals(LocalDate.of(2026, 5, 30), migrated?.expiryDate)
        assertEquals("Dairy", migrated?.categoryTag)
        assertEquals("content://com.nowaste.app.fileprovider/food_photos/food_legacy.jpg", migrated?.photoUri)

        database.close()
    }

    private fun createVersionOneDatabase() {
        val databasePath = context.getDatabasePath(TEST_DATABASE)
        databasePath.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databasePath, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `food_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `expiryDate` TEXT NOT NULL,
                    `productionDate` TEXT,
                    `shelfLifeAmount` INTEGER,
                    `shelfLifeUnit` TEXT,
                    `reminderDaysBeforeExpiry` INTEGER,
                    `categoryTag` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    `photoUri` TEXT NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `food_items` (
                    `id`,
                    `name`,
                    `expiryDate`,
                    `productionDate`,
                    `shelfLifeAmount`,
                    `shelfLifeUnit`,
                    `reminderDaysBeforeExpiry`,
                    `categoryTag`,
                    `note`,
                    `photoUri`,
                    `quantity`,
                    `createdAt`,
                    `updatedAt`
                ) VALUES (
                    1,
                    'Milk',
                    '2026-05-30',
                    '2026-05-01',
                    30,
                    'DAYS',
                    5,
                    'Dairy',
                    'Opened',
                    'content://com.nowaste.app.fileprovider/food_photos/food_legacy.jpg',
                    2,
                    '2026-05-25T09:00',
                    '2026-05-25T10:00'
                )
                """.trimIndent(),
            )
            database.version = 1
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
