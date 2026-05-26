package com.nowaste.app.data

import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.domain.reminderExpiryDatesFor
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

class FoodRepository(
    private val dao: FoodItemDao,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    fun observeFoodItemsSortedByExpiry(): Flow<List<FoodItem>> = dao.observeSortedByExpiry()

    suspend fun addFoodItem(input: FoodItemInput): Long {
        val timestamp = now()
        return dao.insert(
            FoodItem(
                name = input.name.trim(),
                expiryDate = input.expiryDate,
                categoryTag = input.categoryTag.trim(),
                note = input.note.trim(),
                barcodeValue = input.barcodeValue.trim(),
                photoUri = input.photoUri.trim(),
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    suspend fun updateFoodItem(id: Long, input: FoodItemInput) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                name = input.name.trim(),
                expiryDate = input.expiryDate,
                categoryTag = input.categoryTag.trim(),
                note = input.note.trim(),
                barcodeValue = input.barcodeValue.trim(),
                photoUri = input.photoUri.trim(),
                updatedAt = now(),
            ),
        )
    }

    suspend fun deleteFoodItem(id: Long) {
        dao.deleteById(id)
    }

    suspend fun getItemsForReminderCheck(date: LocalDate, nearExpiryDays: Int = 1): List<FoodItem> {
        return dao.getByExpiryDates(reminderExpiryDatesFor(date, nearExpiryDays))
    }
}
