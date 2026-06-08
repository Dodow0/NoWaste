package com.nowaste.app.dataio

import com.nowaste.app.data.FoodItem
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.domain.ShelfLifeUnit
import org.json.JSONObject
import java.io.BufferedReader
import java.io.PushbackReader
import java.io.Reader
import java.io.Writer
import java.time.LocalDate
import java.time.LocalDateTime

enum class FoodExportFormat {
    Json,
    Csv,
}

object FoodDataIo {
    fun importFoods(reader: Reader): List<FoodItemInput> {
        val pushbackReader = PushbackReader(BufferedReader(reader), 2)
        val first = readNextNonWhitespace(pushbackReader)
        return when (first) {
            '['.code -> readJsonArray(pushbackReader)
            '{'.code -> readJsonWrapperObject(pushbackReader)
            -1 -> emptyList()
            else -> {
                pushbackReader.unread(first)
                parseCsvImport(pushbackReader)
            }
        }
    }

    fun exportFoods(
        items: List<FoodItem>,
        format: FoodExportFormat,
        writer: Writer,
        exportedAt: LocalDateTime = LocalDateTime.now(),
    ) {
        when (format) {
            FoodExportFormat.Json -> writeJsonExport(items, writer, exportedAt)
            FoodExportFormat.Csv -> writeCsvExport(items, writer)
        }
    }
}

private fun readJsonWrapperObject(reader: PushbackReader): List<FoodItemInput> {
    val inputs = mutableListOf<FoodItemInput>()
    var next = readNextNonWhitespace(reader)
    if (next == '}'.code) return inputs

    while (next != -1) {
        require(next == '"'.code) { "JSON object keys must be strings." }
        val key = readJsonString(reader)
        require(readNextNonWhitespace(reader) == ':'.code) { "JSON object key is missing ':'." }
        val valueStart = readNextNonWhitespace(reader)
        if (key == "items" && valueStart == '['.code) {
            inputs += readJsonArray(reader)
        } else {
            skipJsonValue(reader, valueStart)
        }

        next = readNextNonWhitespace(reader)
        when (next) {
            ','.code -> next = readNextNonWhitespace(reader)
            '}'.code -> return inputs
            else -> error("JSON object is not properly terminated.")
        }
    }
    error("JSON object is not properly terminated.")
}

private fun readJsonArray(reader: PushbackReader): List<FoodItemInput> {
    val inputs = mutableListOf<FoodItemInput>()
    var next = readNextNonWhitespace(reader)
    if (next == ']'.code) return inputs

    while (next != -1) {
        if (next == '{'.code) {
            runCatching {
                JSONObject(readJsonObjectLiteral(reader)).toFoodItemInputOrNull()
            }.getOrNull()?.let(inputs::add)
        } else {
            skipJsonValue(reader, next)
        }

        next = readNextNonWhitespace(reader)
        when (next) {
            ','.code -> next = readNextNonWhitespace(reader)
            ']'.code -> return inputs
            else -> error("JSON array is not properly terminated.")
        }
    }
    error("JSON array is not properly terminated.")
}

private fun readJsonObjectLiteral(reader: PushbackReader): String {
    val builder = StringBuilder("{")
    var depth = 1
    var inString = false
    while (depth > 0) {
        val next = reader.read()
        if (next == -1) error("JSON object is not properly terminated.")
        val char = next.toChar()
        builder.append(char)

        if (inString) {
            when (char) {
                '\\' -> {
                    val escaped = reader.read()
                    if (escaped == -1) error("JSON string is not properly terminated.")
                    builder.append(escaped.toChar())
                }
                '"' -> inString = false
            }
        } else {
            when (char) {
                '"' -> inString = true
                '{', '[' -> depth += 1
                '}', ']' -> depth -= 1
            }
        }
    }
    return builder.toString()
}

private fun skipJsonValue(reader: PushbackReader, first: Int) {
    when (first) {
        '"'.code -> readJsonString(reader)
        '{'.code -> readJsonObjectLiteral(reader)
        '['.code -> skipJsonArray(reader)
        -1 -> error("JSON value is missing.")
        else -> {
            var next = first
            while (next != -1) {
                val char = next.toChar()
                if (char == ',' || char == ']' || char == '}' || char.isWhitespace()) {
                    reader.unread(next)
                    return
                }
                next = reader.read()
            }
        }
    }
}

private fun skipJsonArray(reader: PushbackReader) {
    var depth = 1
    var inString = false
    while (depth > 0) {
        val next = reader.read()
        if (next == -1) error("JSON array is not properly terminated.")
        val char = next.toChar()
        if (inString) {
            when (char) {
                '\\' -> if (reader.read() == -1) error("JSON string is not properly terminated.")
                '"' -> inString = false
            }
        } else {
            when (char) {
                '"' -> inString = true
                '[' -> depth += 1
                ']' -> depth -= 1
                '{' -> skipJsonObjectAfterOpen(reader)
            }
        }
    }
}

private fun skipJsonObjectAfterOpen(reader: PushbackReader) {
    var depth = 1
    var inString = false
    while (depth > 0) {
        val next = reader.read()
        if (next == -1) error("JSON object is not properly terminated.")
        val char = next.toChar()
        if (inString) {
            when (char) {
                '\\' -> if (reader.read() == -1) error("JSON string is not properly terminated.")
                '"' -> inString = false
            }
        } else {
            when (char) {
                '"' -> inString = true
                '{', '[' -> depth += 1
                '}', ']' -> depth -= 1
            }
        }
    }
}

private fun readJsonString(reader: PushbackReader): String {
    val builder = StringBuilder()
    while (true) {
        val next = reader.read()
        if (next == -1) error("JSON string is not properly terminated.")
        val char = next.toChar()
        when (char) {
            '"' -> return builder.toString()
            '\\' -> {
                val escaped = reader.read()
                if (escaped == -1) error("JSON string is not properly terminated.")
                builder.append(
                    when (val escapedChar = escaped.toChar()) {
                        '"', '\\', '/' -> escapedChar
                        'b' -> '\b'
                        'f' -> '\u000C'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        'u' -> readUnicodeEscape(reader)
                        else -> escapedChar
                    },
                )
            }
            else -> builder.append(char)
        }
    }
}

private fun readUnicodeEscape(reader: PushbackReader): Char {
    val hex = CharArray(4)
    repeat(4) { index ->
        val next = reader.read()
        if (next == -1) error("JSON unicode escape is not properly terminated.")
        hex[index] = next.toChar()
    }
    return hex.concatToString().toInt(16).toChar()
}

private fun readNextNonWhitespace(reader: PushbackReader): Int {
    var next = reader.read()
    while (next != -1) {
        val char = next.toChar()
        if (!char.isWhitespace() && char != '\uFEFF') return next
        next = reader.read()
    }
    return -1
}

private fun parseCsvImport(reader: Reader): List<FoodItemInput> {
    var headers: List<String>? = null
    val inputs = mutableListOf<FoodItemInput>()
    readCsvRows(reader) { row ->
        if (row.all { it.isBlank() }) return@readCsvRows
        val currentHeaders = headers
        if (currentHeaders == null) {
            headers = row.map { it.trim() }
        } else {
            val values = currentHeaders
                .zip(row + List((currentHeaders.size - row.size).coerceAtLeast(0)) { "" })
                .toMap()
            values.toFoodItemInputOrNull()?.let(inputs::add)
        }
    }
    return inputs
}

private fun readCsvRows(reader: Reader, onRow: (List<String>) -> Unit) {
    val pushbackReader = PushbackReader(reader, 1)
    val row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var sawAnyInput = false

    fun emitRow() {
        row += cell.toString()
        cell.clear()
        onRow(row.toList())
        row.clear()
    }

    while (true) {
        val next = pushbackReader.read()
        if (next == -1) break
        sawAnyInput = true
        val char = next.toChar()
        when {
            inQuotes && char == '"' -> {
                val following = pushbackReader.read()
                if (following == '"'.code) {
                    cell.append('"')
                } else {
                    inQuotes = false
                    if (following != -1) pushbackReader.unread(following)
                }
            }
            inQuotes -> cell.append(char)
            char == '"' -> inQuotes = true
            char == ',' -> {
                row += cell.toString()
                cell.clear()
            }
            char == '\r' || char == '\n' -> {
                if (char == '\r') {
                    val following = pushbackReader.read()
                    if (following != '\n'.code && following != -1) {
                        pushbackReader.unread(following)
                    }
                }
                emitRow()
            }
            else -> cell.append(char)
        }
    }

    if (cell.isNotEmpty() || row.isNotEmpty() || sawAnyInput && inQuotes) {
        emitRow()
    }
}

private fun JSONObject.toFoodItemInputOrNull(): FoodItemInput? {
    val name = optString("name").trim()
    val expiryDate = optNullableString("expiryDate")?.let(::parseImportDate) ?: return null
    if (name.isBlank()) return null

    return FoodItemInput(
        name = name,
        expiryDate = expiryDate,
        categoryTag = optString("categoryTag").trim(),
        note = optString("note").trim(),
        photoUri = optString("photoUri").trim(),
        quantity = optInt("quantity", 1).coerceAtLeast(1),
        productionDate = optNullableString("productionDate")?.let(::parseImportDate),
        shelfLifeAmount = optNullableLong("shelfLifeAmount"),
        shelfLifeUnit = optNullableString("shelfLifeUnit")?.let(::parseImportShelfLifeUnit),
        reminderDaysBeforeExpiry = optNullableInt("reminderDaysBeforeExpiry"),
    )
}

private fun Map<String, String>.toFoodItemInputOrNull(): FoodItemInput? {
    val name = this["name"].orEmpty().trim()
    val expiryDate = this["expiryDate"]?.let(::parseImportDate) ?: return null
    if (name.isBlank()) return null

    return FoodItemInput(
        name = name,
        expiryDate = expiryDate,
        categoryTag = this["categoryTag"].orEmpty().trim(),
        note = this["note"].orEmpty().trim(),
        photoUri = this["photoUri"].orEmpty().trim(),
        quantity = this["quantity"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        productionDate = this["productionDate"]?.let(::parseImportDate),
        shelfLifeAmount = this["shelfLifeAmount"]?.toLongOrNull(),
        shelfLifeUnit = this["shelfLifeUnit"]?.let(::parseImportShelfLifeUnit),
        reminderDaysBeforeExpiry = this["reminderDaysBeforeExpiry"]?.toIntOrNull(),
    )
}

private fun writeJsonExport(
    items: List<FoodItem>,
    writer: Writer,
    exportedAt: LocalDateTime,
) {
    writer.append("{\n")
    writer.append("  \"schemaVersion\": 1,\n")
    writer.append("  \"exportedAt\": ")
    writer.writeJsonString(exportedAt.toString())
    writer.append(",\n")
    writer.append("  \"items\": [")
    items.forEachIndexed { index, item ->
        if (index > 0) writer.append(",")
        writer.append("\n    {\n")
        writer.writeJsonField("id", item.id, trailingComma = true)
        writer.writeJsonField("name", item.name, trailingComma = true)
        writer.writeJsonField("expiryDate", item.expiryDate.toString(), trailingComma = true)
        writer.writeJsonField("productionDate", item.productionDate?.toString(), trailingComma = true)
        writer.writeJsonField("shelfLifeAmount", item.shelfLifeAmount, trailingComma = true)
        writer.writeJsonField("shelfLifeUnit", item.shelfLifeUnit?.name, trailingComma = true)
        writer.writeJsonField("reminderDaysBeforeExpiry", item.reminderDaysBeforeExpiry, trailingComma = true)
        writer.writeJsonField("categoryTag", item.categoryTag, trailingComma = true)
        writer.writeJsonField("quantity", item.quantity, trailingComma = true)
        writer.writeJsonField("note", item.note, trailingComma = true)
        writer.writeJsonField("photoUri", item.photoUri, trailingComma = true)
        writer.writeJsonField("createdAt", item.createdAt.toString(), trailingComma = true)
        writer.writeJsonField("updatedAt", item.updatedAt.toString(), trailingComma = false)
        writer.append("\n    }")
    }
    writer.append("\n  ]\n")
    writer.append("}\n")
}

private fun Writer.writeJsonField(name: String, value: Any?, trailingComma: Boolean) {
    append("      ")
    writeJsonString(name)
    append(": ")
    when (value) {
        null -> append("null")
        is Number -> append(value.toString())
        else -> writeJsonString(value.toString())
    }
    if (trailingComma) append(",")
    append("\n")
}

private fun Writer.writeJsonString(value: String) {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (char < ' ') {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
    }
    append('"')
}

private fun writeCsvExport(items: List<FoodItem>, writer: Writer) {
    val headers = listOf(
        "id",
        "name",
        "expiryDate",
        "productionDate",
        "shelfLifeAmount",
        "shelfLifeUnit",
        "reminderDaysBeforeExpiry",
        "categoryTag",
        "quantity",
        "note",
        "photoUri",
        "createdAt",
        "updatedAt",
    )
    writer.appendLine(headers.joinToString(",") { it.toCsvCell() })
    items.forEach { item ->
        writer.appendLine(
            listOf(
                item.id.toString(),
                item.name,
                item.expiryDate.toString(),
                item.productionDate?.toString().orEmpty(),
                item.shelfLifeAmount?.toString().orEmpty(),
                item.shelfLifeUnit?.name.orEmpty(),
                item.reminderDaysBeforeExpiry?.toString().orEmpty(),
                item.categoryTag,
                item.quantity.toString(),
                item.note,
                item.photoUri,
                item.createdAt.toString(),
                item.updatedAt.toString(),
            ).joinToString(",") { it.toCsvCell() },
        )
    }
}

private fun String.toCsvCell(): String {
    val escaped = replace("\"", "\"\"")
    return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private fun parseImportDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim()) }.getOrNull()

private fun parseImportShelfLifeUnit(value: String): ShelfLifeUnit? =
    runCatching { ShelfLifeUnit.valueOf(value.trim()) }.getOrNull()

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun JSONObject.optNullableInt(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null
