package com.nowaste.app.network

import com.nowaste.app.domain.normalizeCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ProductLookupResult(
    val name: String?,
    val categoryTag: String?,
)

class ProductLookupService(
    private val endpointBase: String = "https://world.openfoodfacts.org/api/v2/product",
) {
    suspend fun lookup(barcode: String): ProductLookupResult? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return@withContext null

        val encodedBarcode = URLEncoder.encode(cleanBarcode, "UTF-8")
        val url = URL("$endpointBase/$encodedBarcode.json?fields=product_name,categories,categories_tags")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "NoWaste Android - local food expiry tracker")
        }

        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseProductLookupResponse(body)
        } finally {
            connection.disconnect()
        }
    }
}

fun parseProductLookupResponse(body: String): ProductLookupResult? {
    val root = JSONObject(body)
    if (root.optInt("status", 0) != 1) return null
    val product = root.optJSONObject("product") ?: return null
    val name = product.optString("product_name").takeIf { it.isNotBlank() }
    val categories = product.optJSONArray("categories_tags")
    val firstCategoryTag = if (categories != null && categories.length() > 0) {
        categories.optString(0)
    } else {
        null
    }
    val rawCategory = firstCategoryTag ?: product.optString("categories").takeIf { it.isNotBlank() }
    val normalizedCategory = normalizeCategory(rawCategory) ?: rawCategory
        ?.substringAfterLast(':')
        ?.replace('-', ' ')
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    return ProductLookupResult(
        name = name,
        categoryTag = normalizedCategory,
    ).takeIf { it.name != null || it.categoryTag != null }
}
