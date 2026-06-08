package com.nowaste.app.dataio

import com.nowaste.app.data.FoodItem
import com.nowaste.app.domain.ShelfLifeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalDateTime

class FoodDataIoTest {
    @Test
    fun importsJsonArrayAndSkipsInvalidItems() {
        val content = """
            [
              {
                "name": "Milk",
                "expiryDate": "2026-05-30",
                "categoryTag": "Dairy",
                "quantity": 2,
                "productionDate": "2026-05-01",
                "shelfLifeAmount": 30,
                "shelfLifeUnit": "DAYS",
                "reminderDaysBeforeExpiry": 5
              },
              { "name": "", "expiryDate": "2026-05-31" },
              { "name": "No date" }
            ]
        """.trimIndent()

        val inputs = FoodDataIo.importFoods(StringReader(content))

        assertEquals(1, inputs.size)
        assertEquals("Milk", inputs.single().name)
        assertEquals(LocalDate.of(2026, 5, 30), inputs.single().expiryDate)
        assertEquals(2, inputs.single().quantity)
        assertEquals(LocalDate.of(2026, 5, 1), inputs.single().productionDate)
        assertEquals(30L, inputs.single().shelfLifeAmount)
        assertEquals(ShelfLifeUnit.DAYS, inputs.single().shelfLifeUnit)
        assertEquals(5, inputs.single().reminderDaysBeforeExpiry)
    }

    @Test
    fun importsJsonWrappedItemsArray() {
        val content = """
            {
              "schemaVersion": 1,
              "items": [
                { "name": "Apple", "expiryDate": "2026-06-01", "note": "Crisp" }
              ]
            }
        """.trimIndent()

        val inputs = FoodDataIo.importFoods(StringReader(content))

        assertEquals(1, inputs.size)
        assertEquals("Apple", inputs.single().name)
        assertEquals("Crisp", inputs.single().note)
    }

    @Test
    fun importsJsonWrappedItemsAfterSkippingNestedFields() {
        val content = """
            {
              "metadata": {
                "source": "manual",
                "history": [
                  { "event": "created", "payload": { "ignored": true } }
                ]
              },
              "items": [
                { "name": "Eggs", "expiryDate": "2026-06-02" }
              ]
            }
        """.trimIndent()

        val inputs = FoodDataIo.importFoods(StringReader(content))

        assertEquals(1, inputs.size)
        assertEquals("Eggs", inputs.single().name)
        assertEquals(LocalDate.of(2026, 6, 2), inputs.single().expiryDate)
    }

    @Test
    fun importsCsvWithQuotedCommasAndNewlines() {
        val content = """
            name,expiryDate,categoryTag,quantity,note
            "Yogurt, plain",2026-05-30,Dairy,3,"Line 1
            Line 2"
            Invalid,,Dairy,1,No date
        """.trimIndent()

        val inputs = FoodDataIo.importFoods(StringReader(content))

        assertEquals(1, inputs.size)
        assertEquals("Yogurt, plain", inputs.single().name)
        assertEquals(3, inputs.single().quantity)
        assertEquals("Line 1\nLine 2", inputs.single().note)
    }

    @Test
    fun exportsCsvWithEscapedCells() {
        val writer = StringWriter()

        FoodDataIo.exportFoods(
            items = listOf(foodItem(name = "Yogurt, plain", note = "Line 1\nLine 2")),
            format = FoodExportFormat.Csv,
            writer = writer,
            exportedAt = LocalDateTime.of(2026, 5, 25, 9, 0),
        )

        val exported = writer.toString()
        assertTrue(exported.contains("\"Yogurt, plain\""))
        assertTrue(exported.contains("\"Line 1\nLine 2\""))
    }

    @Test
    fun exportsJsonObjectWithItems() {
        val writer = StringWriter()

        FoodDataIo.exportFoods(
            items = listOf(foodItem(name = "Milk", note = "Opened")),
            format = FoodExportFormat.Json,
            writer = writer,
            exportedAt = LocalDateTime.of(2026, 5, 25, 9, 0),
        )

        val exported = writer.toString()
        assertTrue(exported.contains("\"schemaVersion\": 1"))
        assertTrue(exported.contains("\"exportedAt\": \"2026-05-25T09:00\""))
        assertTrue(exported.contains("\"name\": \"Milk\""))
    }

    private fun foodItem(
        name: String,
        note: String,
    ): FoodItem =
        FoodItem(
            id = 1,
            name = name,
            expiryDate = LocalDate.of(2026, 5, 30),
            categoryTag = "Dairy",
            note = note,
            photoUri = "",
            quantity = 1,
            createdAt = LocalDateTime.of(2026, 5, 25, 9, 0),
            updatedAt = LocalDateTime.of(2026, 5, 25, 9, 0),
        )
}
