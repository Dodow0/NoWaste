package com.nowaste.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearExpiryDaysInputTest {
    @Test
    fun parsesValidBoundaryValues() {
        assertEquals(1, parseNearExpiryDaysText("1"))
        assertEquals(30, parseNearExpiryDaysText("30"))
    }

    @Test
    fun rejectsBlankAndOutOfRangeValues() {
        assertNull(parseNearExpiryDaysText(""))
        assertNull(parseNearExpiryDaysText("0"))
        assertNull(parseNearExpiryDaysText("31"))
    }

    @Test
    fun trimsValidInput() {
        assertEquals(7, parseNearExpiryDaysText(" 7 "))
    }
}
