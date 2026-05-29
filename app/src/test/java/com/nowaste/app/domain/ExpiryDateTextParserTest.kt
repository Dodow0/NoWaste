package com.nowaste.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ExpiryDateTextParserTest {
    @Test
    fun extractsHyphenSeparatedDate() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("保质期至 2026-05-20 请冷藏"),
        )
    }

    @Test
    fun extractsChineseDate() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("有效期至2026年5月20日"),
        )
    }

    @Test
    fun extractsDateWithOcrSpacesAndDots() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("保质期至 2026 . 05 . 20"),
        )
    }

    @Test
    fun normalizesFullWidthOcrCharacters() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("有效期至２０２６／０５／２０"),
        )
    }

    @Test
    fun extractsCompactDate() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("EXP:20260520"),
        )
    }

    @Test
    fun extractsContextualSpacedDate() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("EXP 2026 05 20"),
        )
    }

    @Test
    fun expandsTwoDigitYear() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("有效期至26-05-20"),
        )
    }

    @Test
    fun prefersExplicitExpiryDateOverProductionDate() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("生产日期 2025-05-20 有效期至 2026-05-20"),
        )
    }

    @Test
    fun infersExpiryDateFromProductionDateAndShelfLifeMonths() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractExpiryDateFromText("生产日期: 2025-05-20 保质期: 12个月"),
        )
    }

    @Test
    fun infersExpiryDateFromProductionDateAndShelfLifeDays() {
        assertEquals(
            LocalDate.of(2025, 6, 19),
            extractExpiryDateFromText("MFG 2025/05/20 保质期30天"),
        )
    }

    @Test
    fun extractsBareShelfLifeDays() {
        assertEquals(
            ShelfLifeDuration(180, ShelfLifeUnit.DAYS),
            extractShelfLifeDurationFromText("180天"),
        )
    }

    @Test
    fun extractsBareShelfLifeChineseDays() {
        assertEquals(
            ShelfLifeDuration(35, ShelfLifeUnit.DAYS),
            extractShelfLifeDurationFromText("35日"),
        )
    }

    @Test
    fun extractsFuzzyShelfLifeMonths() {
        assertEquals(
            ShelfLifeDuration(6, ShelfLifeUnit.MONTHS),
            extractShelfLifeDurationFromText("保质期 6 个月"),
        )
    }

    @Test
    fun extractsChineseShelfLifeMonths() {
        assertEquals(
            ShelfLifeDuration(18, ShelfLifeUnit.MONTHS),
            extractShelfLifeDurationFromText("十八个月"),
        )
    }

    @Test
    fun extractsChineseShelfLifeDays() {
        assertEquals(
            ShelfLifeDuration(20, ShelfLifeUnit.DAYS),
            extractShelfLifeDurationFromText("二十日"),
        )
    }

    @Test
    fun extractsBareShelfLifeYear() {
        assertEquals(
            ShelfLifeDuration(1, ShelfLifeUnit.YEARS),
            extractShelfLifeDurationFromText("1年"),
        )
    }

    @Test
    fun extractsBareChineseShelfLifeYear() {
        assertEquals(
            ShelfLifeDuration(1, ShelfLifeUnit.YEARS),
            extractShelfLifeDurationFromText("一年"),
        )
    }

    @Test
    fun extractsChineseShelfLifeHundreds() {
        assertEquals(
            ShelfLifeDuration(180, ShelfLifeUnit.DAYS),
            extractShelfLifeDurationFromText("保质期一百八十天"),
        )
    }

    @Test
    fun calculatesExpiryFromShelfLifeDuration() {
        assertEquals(
            LocalDate.of(2025, 11, 20),
            extractShelfLifeDurationFromText("6个月")?.addTo(LocalDate.of(2025, 5, 20)),
        )
    }

    @Test
    fun calculatesExpiryFromChineseShelfLifeDuration() {
        assertEquals(
            LocalDate.of(2026, 11, 20),
            extractShelfLifeDurationFromText("十八个月")?.addTo(LocalDate.of(2025, 5, 20)),
        )
    }

    @Test
    fun calculatesExpiryFromYearShelfLifeDuration() {
        assertEquals(
            LocalDate.of(2026, 5, 20),
            extractShelfLifeDurationFromText("一年")?.addTo(LocalDate.of(2025, 5, 20)),
        )
    }

    @Test
    fun extractsProductionDateWithProductionContext() {
        assertEquals(
            LocalDate.of(2025, 5, 20),
            extractProductionDateFromText("生产日期 2025-05-20 有效期至 2026-05-20"),
        )
    }

    @Test
    fun extractsProductionDateWhenKeywordAppearsAfterDate() {
        assertEquals(
            LocalDate.of(2025, 5, 20),
            extractProductionDateFromText("2025-05-20 生产日期 2026-05-20 到期日"),
        )
    }

    @Test
    fun extractsProductionDateWithChineseNumerals() {
        assertEquals(
            LocalDate.of(2026, 1, 30),
            extractProductionDateFromText("生产日期 二〇二六年一月三十日"),
        )
    }

    @Test
    fun fallsBackToEarliestDateForProductionDate() {
        assertEquals(
            LocalDate.of(2025, 5, 20),
            extractProductionDateFromText("2026-05-20 2025-05-20"),
        )
    }

    @Test
    fun doesNotTreatPlainDateAsBareShelfLife() {
        assertNull(extractShelfLifeDurationFromText("生产日期 2026年5月20日"))
    }

    @Test
    fun returnsNullWhenNoFullDateExists() {
        assertNull(extractExpiryDateFromText("生产日期见喷码，保质期12个月"))
    }

    @Test
    fun ignoresInvalidDate() {
        assertNull(extractExpiryDateFromText("有效期至 2026-13-40"))
    }
}
