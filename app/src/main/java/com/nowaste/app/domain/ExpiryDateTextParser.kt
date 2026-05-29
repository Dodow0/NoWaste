package com.nowaste.app.domain

import java.time.DateTimeException
import java.time.LocalDate

private val FullDatePattern =
    Regex("""(?<!\d)(20\d{2}|\d{2})\s*[-./年]\s*(\d{1,2})\s*[-./月]\s*(\d{1,2})\s*日?(?!\d)""")
private val ContextualSpacedDatePattern = Regex("""(?<!\d)(20\d{2})\s+(\d{1,2})\s+(\d{1,2})(?!\d)""")
private val CompactDatePattern = Regex("""(?<!\d)(20\d{2})(\d{2})(\d{2})(?!\d)""")
private val ChineseDatePattern =
    Regex("""([二〇零一二三四五六七八九]{2,4})\s*年\s*([一二三四五六七八九十]{1,3})\s*月\s*([一二三四五六七八九十]{1,3})\s*日?""")
private val ExpiryKeywordPattern =
    Regex("""(?i)(保质期至|有效期至|有效日期至|到期日|截止日期|食用期限|最佳食用期至|请于|EXP|Expiry|Best\s*Before|Use\s*By)""")
private val ProductionKeywordPattern =
    Regex("""(?i)(生产日期|生产日|生产于|生产批号|生產日期|制造日期|制造日|製造日期|出厂日期|包装日期|包裝日期|加工日期|灌装日期|灌裝日期|喷码日期|喷印日期|打码日期|日期见|MFG|MFD|PROD|PD)""")
private val ExplicitShelfLifePattern =
    Regex("""(?i)(?:保质期|保存期|质保期|shelf\s*life)\s*[:：]?\s*(?<!\d)([0-9零〇一二两三四五六七八九十百]{1,8})\s*(年|个月|月|天|日|days?|months?|years?)(?!\d)""")
private val BareShelfLifePattern =
    Regex("""(?i)(?<![\d年月./-])([0-9零〇一二两三四五六七八九十百]{1,8})\s*(个月|天|日|days?|months?)(?!\d)""")
private val BareYearShelfLifePattern =
    Regex("""(?i)(?<![\d年月./-])([1-9]|[一二两三四五六七八九十]{1,3})\s*(年|years?)(?!\s*[月\d])""")

enum class ShelfLifeUnit {
    DAYS,
    MONTHS,
    YEARS,
}

data class ShelfLifeDuration(
    val amount: Long,
    val unit: ShelfLifeUnit,
) {
    fun addTo(date: LocalDate): LocalDate =
        when (unit) {
            ShelfLifeUnit.DAYS -> date.plusDays(amount)
            ShelfLifeUnit.MONTHS -> date.plusMonths(amount)
            ShelfLifeUnit.YEARS -> date.plusYears(amount)
        }

    fun toDisplayText(): String =
        "$amount${
            when (unit) {
                ShelfLifeUnit.DAYS -> "天"
                ShelfLifeUnit.MONTHS -> "个月"
                ShelfLifeUnit.YEARS -> "年"
            }
        }"
}

private data class DateCandidate(
    val date: LocalDate,
    val range: IntRange,
)

fun extractExpiryDateFromText(text: String): LocalDate? {
    val normalizedText = normalizeOcrText(text)
    val dateCandidates = findDateCandidates(normalizedText)
    if (dateCandidates.isEmpty()) return null

    val explicitExpiryDates = dateCandidates.filter { hasExpiryContext(normalizedText, it.range) }
    if (explicitExpiryDates.isNotEmpty()) {
        return explicitExpiryDates.maxOf { it.date }
    }

    inferExpiryFromProductionAndShelfLife(normalizedText, dateCandidates)?.let { return it }

    val nonProductionDates = dateCandidates.filterNot { hasProductionContext(normalizedText, it.range) }
    return (nonProductionDates.ifEmpty { dateCandidates }).maxOf { it.date }
}

fun extractProductionDateFromText(text: String): LocalDate? {
    val normalizedText = normalizeOcrText(text)
    val dateCandidates = findDateCandidates(normalizedText)
    if (dateCandidates.isEmpty()) return null

    return dateCandidates
        .filter { hasProductionContext(normalizedText, it.range) }
        .minByOrNull { it.date }
        ?.date
        ?: dateCandidates.minByOrNull { it.date }?.date
}

fun extractShelfLifeDurationFromText(text: String): ShelfLifeDuration? {
    val normalizedText = normalizeOcrText(text)
    val match = ExplicitShelfLifePattern.find(normalizedText)
        ?: BareShelfLifePattern.find(normalizedText)
        ?: BareYearShelfLifePattern.find(normalizedText)
        ?: return null
    return match.toShelfLifeDuration()
}

private fun findDateCandidates(text: String): List<DateCandidate> {
    val fullDates = FullDatePattern.findAll(text).mapNotNull { match ->
        val (year, month, day) = match.destructured
        parseDate(year, month, day)?.let { DateCandidate(it, match.range) }
    }
    val compactDates = CompactDatePattern.findAll(text).mapNotNull { match ->
        val (year, month, day) = match.destructured
        parseDate(year, month, day)?.let { DateCandidate(it, match.range) }
    }
    val contextualSpacedDates = ContextualSpacedDatePattern.findAll(text).mapNotNull { match ->
        if (!hasExpiryContext(text, match.range) && !hasProductionContext(text, match.range)) return@mapNotNull null
        val (year, month, day) = match.destructured
        parseDate(year, month, day)?.let { DateCandidate(it, match.range) }
    }
    val chineseDates = ChineseDatePattern.findAll(text).mapNotNull { match ->
        val (year, month, day) = match.destructured
        parseChineseDate(year, month, day)?.let { DateCandidate(it, match.range) }
    }
    return (fullDates + compactDates + contextualSpacedDates + chineseDates)
        .distinctBy { it.date to it.range }
        .toList()
}

private fun parseDate(year: String, month: String, day: String): LocalDate? {
    val fullYear = if (year.length == 2) "20$year" else year
    return try {
        LocalDate.of(fullYear.toInt(), month.toInt(), day.toInt())
    } catch (_: DateTimeException) {
        null
    }
}

private fun parseChineseDate(year: String, month: String, day: String): LocalDate? {
    val parsedYear = year.mapNotNull { it.toChineseDigitOrNull() }
        .joinToString("")
        .takeIf { it.length in 2..4 }
        ?.let { if (it.length == 2) "20$it" else it }
        ?.toIntOrNull()
        ?: return null
    val parsedMonth = parseChineseNumber(month) ?: return null
    val parsedDay = parseChineseNumber(day) ?: return null
    return try {
        LocalDate.of(parsedYear, parsedMonth, parsedDay)
    } catch (_: DateTimeException) {
        null
    }
}

private fun parseChineseNumber(value: String): Int? {
    val cleanValue = value.trim()
    cleanValue.toIntOrNull()?.let { return it }
    val hundredIndex = cleanValue.indexOf('百')
    if (hundredIndex != -1) {
        val hundreds = cleanValue.substring(0, hundredIndex)
            .singleOrNull()
            ?.toChineseDigitOrNull()
            ?: 1
        val rest = cleanValue.substring(hundredIndex + 1)
        return hundreds * 100 + (rest.takeIf { it.isNotBlank() }?.let(::parseChineseNumber) ?: 0)
    }
    if (cleanValue == "十") return 10
    val tenIndex = cleanValue.indexOf('十')
    if (tenIndex == -1) {
        return cleanValue.singleOrNull()?.toChineseDigitOrNull()
    }
    val tens = cleanValue.substring(0, tenIndex)
        .singleOrNull()
        ?.toChineseDigitOrNull()
        ?: 1
    val ones = cleanValue.substring(tenIndex + 1)
        .singleOrNull()
        ?.toChineseDigitOrNull()
        ?: 0
    return tens * 10 + ones
}

private fun Char.toChineseDigitOrNull(): Int? =
    when (this) {
        '零', '〇' -> 0
        '一' -> 1
        '二', '两' -> 2
        '三' -> 3
        '四' -> 4
        '五' -> 5
        '六' -> 6
        '七' -> 7
        '八' -> 8
        '九' -> 9
        else -> null
    }

private fun inferExpiryFromProductionAndShelfLife(
    text: String,
    dateCandidates: List<DateCandidate>,
): LocalDate? {
    val productionDate = dateCandidates
        .filter { hasProductionContext(text, it.range) }
        .maxOfOrNull { it.date }
        ?: return null

    return ExplicitShelfLifePattern.find(text)?.toShelfLifeDuration()?.addTo(productionDate)
}

private fun MatchResult.toShelfLifeDuration(): ShelfLifeDuration? {
    val amount = groupValues[1].toLongOrNull()
        ?: parseChineseNumber(groupValues[1])?.toLong()
        ?: return null
    val unit = when (groupValues[2].lowercase()) {
        "年", "year", "years" -> ShelfLifeUnit.YEARS
        "个月", "月", "month", "months" -> ShelfLifeUnit.MONTHS
        "天", "日", "day", "days" -> ShelfLifeUnit.DAYS
        else -> return null
    }
    return ShelfLifeDuration(amount, unit)
}

private fun hasExpiryContext(text: String, range: IntRange): Boolean {
    val before = text.contextBefore(range, 24)
    val after = text.contextAfter(range, 10)
    return ExpiryKeywordPattern.containsMatchIn(before) || Regex("""^(前|止|到期|过期)""").containsMatchIn(after)
}

private fun hasProductionContext(text: String, range: IntRange): Boolean =
    ProductionKeywordPattern.containsMatchIn(text.contextBefore(range, 24)) ||
        ProductionKeywordPattern.containsMatchIn(text.contextAfter(range, 12))

private fun String.contextBefore(range: IntRange, length: Int): String =
    substring((range.first - length).coerceAtLeast(0), range.first)

private fun String.contextAfter(range: IntRange, length: Int): String =
    substring((range.last + 1).coerceAtMost(this.length), (range.last + 1 + length).coerceAtMost(this.length))

private fun normalizeOcrText(text: String): String =
    buildString(text.length) {
        text.forEach { char ->
            append(
                when (char) {
                    in '０'..'９' -> '0' + (char - '０')
                    '－', '—', '–' -> '-'
                    '／' -> '/'
                    '．', '。' -> '.'
                    '：' -> ':'
                    '曰' -> '日'
                    else -> char
                },
            )
        }
    }
