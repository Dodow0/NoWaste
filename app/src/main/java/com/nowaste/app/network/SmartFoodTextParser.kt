package com.nowaste.app.network

import com.nowaste.app.domain.ShelfLifeDuration
import com.nowaste.app.domain.ShelfLifeUnit
import com.nowaste.app.domain.extractExpiryDateFromText
import com.nowaste.app.domain.extractProductionDateFromText
import com.nowaste.app.domain.extractShelfLifeDurationFromText
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.LocalDate

data class SmartFoodParseConfig(
    val apiUrl: String,
    val apiKey: String,
    val model: String,
)

data class SmartFoodParseResult(
    val name: String? = null,
    val categoryTag: String? = null,
    val productionDate: LocalDate? = null,
    val shelfLife: ShelfLifeDuration? = null,
    val expiryDate: LocalDate? = null,
    val note: String? = null,
)

class SmartFoodTextParser {
    fun parseBatch(
        text: String,
        config: SmartFoodParseConfig,
    ): List<SmartFoodParseResult> {
        require(text.isNotBlank()) { "请输入要解析的文字。" }
        require(config.apiUrl.isNotBlank()) { "请先在设置中填写智能解析 API URL。" }
        require(config.apiKey.isNotBlank()) { "请先在设置中填写智能解析 API Key。" }
        require(config.model.isNotBlank()) { "请先在设置中填写模型名称。" }

        val responseBody = postChatCompletion(
            text = text,
            config = config,
            systemPrompt = SMART_BATCH_PARSE_PROMPT,
            readTimeoutMillis = BATCH_PARSE_READ_TIMEOUT_MILLIS,
        )
        val content = extractAssistantContent(responseBody)
        return parseBatchResultJson(content, text)
    }

    fun testConnection(config: SmartFoodParseConfig): String {
        require(config.apiUrl.isNotBlank()) { "请先在设置中填写智能解析 API URL。" }
        require(config.apiKey.isNotBlank()) { "请先在设置中填写智能解析 API Key。" }
        require(config.model.isNotBlank()) { "请先在设置中填写模型名称。" }

        val responseBody = postChatCompletion(
            text = "ping",
            config = config,
            systemPrompt = SMART_CONNECTION_TEST_PROMPT,
        )
        val content = extractAssistantContent(responseBody)
            .trim()
            .ifBlank { "OK" }
        return "连接正常：${content.take(24)}"
    }

    private fun postChatCompletion(
        text: String,
        config: SmartFoodParseConfig,
        systemPrompt: String,
        readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
    ): String {
        val requestBody = JSONObject()
            .put("model", config.model)
            .put("temperature", 0.1)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", systemPrompt),
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", text),
                    ),
            )
            .toString()

        val connection = (URL(config.apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = readTimeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }

        return try {
            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseText = runCatching {
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrDefault("")
            if (statusCode !in 200..299) {
                throw IllegalStateException(
                    "智能解析连接失败：HTTP $statusCode${responseText.toReadableErrorSummary(statusCode)}",
                )
            }
            responseText
        } catch (error: SocketTimeoutException) {
            throw IllegalStateException(
                "智能解析请求超时。连接测试只验证接口可达，批量解析需要等待模型拆分和生成 JSON；请减少一次输入的商品数量，或换用响应更快的模型/线路。",
                error,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun extractAssistantContent(responseBody: String): String =
        try {
            JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
        } catch (error: JSONException) {
            throw IllegalStateException(
                "接口已返回内容，但不是 OpenAI Chat Completions 兼容 JSON，请检查 API URL 是否填到了 /chat/completions。",
                error,
            )
        }

    private fun String.toReadableErrorSummary(statusCode: Int): String {
        val cleanText = trim()
        if (statusCode == 530) {
            return "，服务商网关或源站异常，请检查第三方 API 地址、代理线路或服务商状态。"
        }
        if (cleanText.isBlank()) {
            return "，接口没有返回错误详情。"
        }
        if (cleanText.startsWith("<")) {
            return "，接口返回了 HTML 错误页，请检查 API URL、代理或服务商网关状态。"
        }
        return "，${cleanText.take(300)}"
    }

    private fun parseBatchResultJson(
        modelContent: String,
        originalText: String,
    ): List<SmartFoodParseResult> {
        val candidate = modelContent.trim().stripMarkdownFence()
        val items = when {
            candidate.startsWith("[") -> JSONArray(candidate)
            candidate.startsWith("{") -> {
                val json = JSONObject(extractJsonObject(candidate))
                json.optJSONArray("items") ?: JSONArray().put(json)
            }

            else -> JSONArray(extractJsonArray(candidate))
        }

        val results = (0 until items.length())
            .mapNotNull { index -> items.optJSONObject(index) }
            .map { json ->
                val itemText = json.optCleanString("original_text")
                    ?: json.optCleanString("originalText")
                    ?: originalText
                parseResultObject(json, itemText)
            }
            .filter { result ->
                !result.name.isNullOrBlank() ||
                    result.productionDate != null ||
                    result.shelfLife != null ||
                    result.expiryDate != null
            }

        require(results.isNotEmpty()) { "模型没有识别到可录入的食品，请补充食品名称、生产日期或保质期后重试。" }
        return results
    }

    private fun parseResultObject(
        json: JSONObject,
        originalText: String,
    ): SmartFoodParseResult {
        val productionDate = json.optLocalDate("production_date")
            ?: json.optLocalDate("productionDate")
            ?: extractProductionDateFromText(originalText)
        val shelfLife = json.optShelfLife()
            ?: extractShelfLifeDurationFromText(originalText)
        val expiryDate = json.optLocalDate("expiry_date")
            ?: json.optLocalDate("expiryDate")
            ?: productionDate?.let { date -> shelfLife?.addTo(date) }
            ?: extractExpiryDateFromText(originalText)
        val note = listOfNotNull(
            json.optCleanString("quantity_note"),
            json.optCleanString("quantityNote"),
            json.optCleanString("note"),
        )
            .distinct()
            .joinToString("，")
            .takeIf { it.isNotBlank() }

        return SmartFoodParseResult(
            name = json.optCleanString("name"),
            categoryTag = json.optCleanString("category_tag") ?: json.optCleanString("categoryTag"),
            productionDate = productionDate,
            shelfLife = shelfLife,
            expiryDate = expiryDate,
            note = note,
        )
    }

    private fun extractJsonObject(content: String): String {
        val candidate = content.trim().stripMarkdownFence()
        val start = candidate.indexOf('{')
        val end = candidate.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            throw IllegalStateException("模型没有返回可解析的 JSON。")
        }
        return candidate.substring(start, end + 1)
    }

    private fun extractJsonArray(content: String): String {
        val candidate = content.trim().stripMarkdownFence()
        val start = candidate.indexOf('[')
        val end = candidate.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) {
            throw IllegalStateException("模型没有返回可解析的 JSON 数组。")
        }
        return candidate.substring(start, end + 1)
    }

    private fun String.stripMarkdownFence(): String {
        if (!startsWith("```")) return this

        val firstLineEnd = indexOf('\n')
        val body = if (firstLineEnd == -1) {
            removePrefix("```")
        } else {
            substring(firstLineEnd + 1)
        }
        val fenceEnd = body.lastIndexOf("```")
        return if (fenceEnd == -1) body.trim() else body.substring(0, fenceEnd).trim()
    }

    private fun JSONObject.optCleanString(name: String): String? =
        optString(name, "")
            .trim()
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

    private fun JSONObject.optLocalDate(name: String): LocalDate? =
        optCleanString(name)?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }

    private fun JSONObject.optShelfLife(): ShelfLifeDuration? {
        val amount = optLongOrNull("shelf_life_amount")
            ?: optLongOrNull("shelfLifeAmount")
        val unit = optCleanString("shelf_life_unit")
            ?: optCleanString("shelfLifeUnit")
        return if (amount != null && unit != null) {
            ShelfLifeDuration(amount, unit.toShelfLifeUnit() ?: return null)
        } else {
            (optCleanString("shelf_life") ?: optCleanString("shelfLife"))
                ?.let(::extractShelfLifeDurationFromText)
        }
    }

    private fun JSONObject.optLongOrNull(name: String): Long? =
        if (has(name) && !isNull(name)) {
            runCatching { getLong(name) }.getOrNull()
        } else {
            null
        }

    private fun String.toShelfLifeUnit(): ShelfLifeUnit? =
        when (trim().lowercase()) {
            "day", "days", "d", "天", "日" -> ShelfLifeUnit.DAYS
            "month", "months", "m", "月", "个月" -> ShelfLifeUnit.MONTHS
            "year", "years", "y", "年" -> ShelfLifeUnit.YEARS
            else -> null
        }

    private companion object {
        private const val SMART_BATCH_PARSE_PROMPT =
            "你是食品批量录入助手。请把用户输入拆分为多个食品条目，只返回 JSON 数组，不要解释。" +
                "每个数组元素字段：" +
                "name 食品名称字符串或 null；" +
                "category_tag 分类标签字符串或 null；" +
                "production_date 生产日期，格式 yyyy-MM-dd 或 null；" +
                "shelf_life_amount 数字或 null；" +
                "shelf_life_unit 只能是 days、months、years 或 null；" +
                "expiry_date 到期日，格式 yyyy-MM-dd 或 null；" +
                "quantity_note 数量、规格、补充说明字符串或 null；" +
                "original_text 该食品对应的原始片段。" +
                "如果年份是 25、26 这类两位数，按 2025、2026 处理。" +
                "如果同时有生产日期和保质期，请计算 expiry_date。" +
                "不要把多个食品合并到一个元素。"
        private const val SMART_CONNECTION_TEST_PROMPT =
            "你是接口连通性测试助手。请只回复 OK，不要返回 JSON，不要解释。"
        private const val DEFAULT_READ_TIMEOUT_MILLIS = 30_000
        private const val BATCH_PARSE_READ_TIMEOUT_MILLIS = 120_000
    }
}
