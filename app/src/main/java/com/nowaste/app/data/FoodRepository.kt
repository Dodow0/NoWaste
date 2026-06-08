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
        val itemId = dao.insert(
            FoodItem(
                name = input.name.trim(),
                expiryDate = input.expiryDate,
                productionDate = input.productionDate,
                shelfLifeAmount = input.shelfLifeAmount,
                shelfLifeUnit = input.shelfLifeUnit,
                reminderDaysBeforeExpiry = input.reminderDaysBeforeExpiry,
                categoryTag = input.categoryTag.trim(),
                note = input.note.trim(),
                photoUri = input.photoUri.trim(),
                quantity = input.quantity.coerceAtLeast(1),
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
        return itemId
    }

    suspend fun updateFoodItem(id: Long, input: FoodItemInput) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                name = input.name.trim(),
                expiryDate = input.expiryDate,
                productionDate = input.productionDate,
                shelfLifeAmount = input.shelfLifeAmount,
                shelfLifeUnit = input.shelfLifeUnit,
                reminderDaysBeforeExpiry = input.reminderDaysBeforeExpiry,
                categoryTag = input.categoryTag.trim(),
                note = input.note.trim(),
                photoUri = input.photoUri.trim(),
                quantity = input.quantity.coerceAtLeast(1),
                updatedAt = now(),
            ),
        )
    }

    suspend fun updateFoodItemQuantity(id: Long, quantity: Int) {
        dao.updateQuantity(
            id = id,
            quantity = quantity.coerceAtLeast(1),
            updatedAt = now(),
        )
    }

    suspend fun deleteFoodItem(id: Long): FoodItem? {
        val existing = dao.getById(id) ?: return null
        return if (dao.deleteById(id) > 0) existing else null
    }

    suspend fun getItemsForReminderCheck(date: LocalDate, nearExpiryDays: Int = 1): List<FoodItem> {
        return dao.getAllForReminderCheck().filter { item ->
            val reminderDays = item.reminderDaysBeforeExpiry ?: nearExpiryDays
            item.expiryDate in reminderExpiryDatesFor(date, reminderDays)
        }
    }
}
