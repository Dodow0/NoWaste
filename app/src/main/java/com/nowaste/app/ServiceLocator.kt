package com.nowaste.app

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nowaste.app.data.AppDatabase
import com.nowaste.app.data.FoodRepository
import com.nowaste.app.network.ProductLookupService
import com.nowaste.app.settings.AppSettings

object ServiceLocator {
    @Volatile
    private var database: AppDatabase? = null
    @Volatile
    private var settings: AppSettings? = null

    fun database(context: Context): AppDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "nowaste.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { database = it }
        }

    fun foodRepository(context: Context): FoodRepository =
        FoodRepository(database(context).foodItemDao())

    fun appSettings(context: Context): AppSettings =
        settings ?: synchronized(this) {
            settings ?: AppSettings(context.applicationContext).also { settings = it }
        }

    fun productLookupService(): ProductLookupService = ProductLookupService()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE food_items ADD COLUMN barcodeValue TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE food_items ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
        }
    }
}
