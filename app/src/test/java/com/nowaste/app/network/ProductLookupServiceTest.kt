package com.nowaste.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductLookupServiceTest {
    @Test
    fun parsesProductNameAndNormalizesCategory() {
        val result = parseProductLookupResponse(
            """
            {
              "status": 1,
              "product": {
                "product_name": "Plain Yogurt",
                "categories_tags": ["en:dairies", "en:yogurts"]
              }
            }
            """.trimIndent(),
        )

        assertEquals("Plain Yogurt", result?.name)
        assertEquals("乳制品", result?.categoryTag)
    }

    @Test
    fun returnsNullWhenProductIsMissing() {
        assertNull(parseProductLookupResponse("""{"status": 0}"""))
    }

    @Test
    fun usesCategoriesWhenTagsAreMissing() {
        val result = parseProductLookupResponse(
            """
            {
              "status": 1,
              "product": {
                "categories": "Beverages"
              }
            }
            """.trimIndent(),
        )

        assertEquals("饮料", result?.categoryTag)
    }
}
