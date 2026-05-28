package com.nowaste.app.data

import androidx.room.TypeConverter
import com.nowaste.app.domain.ShelfLifeUnit
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromShelfLifeUnit(value: ShelfLifeUnit?): String? = value?.name

    @TypeConverter
    fun toShelfLifeUnit(value: String?): ShelfLifeUnit? =
        value?.let { runCatching { ShelfLifeUnit.valueOf(it) }.getOrNull() }
}
