package com.nowaste.app

import android.content.Context
import androidx.room.Room
import com.nowaste.app.data.AppDatabase
import com.nowaste.app.data.FoodRepository
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
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { database = it }
        }

    fun foodRepository(context: Context): FoodRepository =
        database(context).let { database ->
            FoodRepository(
                dao = database.foodItemDao(),
            )
        }

    fun appSettings(context: Context): AppSettings =
        settings ?: synchronized(this) {
            settings ?: AppSettings(context.applicationContext).also { settings = it }
        }
}
