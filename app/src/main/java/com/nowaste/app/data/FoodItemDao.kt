package com.nowaste.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC, name COLLATE NOCASE ASC")
    fun observeSortedByExpiry(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FoodItem?

    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC, name COLLATE NOCASE ASC")
    suspend fun getAllForReminderCheck(): List<FoodItem>

    @Insert
    suspend fun insert(item: FoodItem): Long

    @Update
    suspend fun update(item: FoodItem)

    @Delete
    suspend fun delete(item: FoodItem)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
