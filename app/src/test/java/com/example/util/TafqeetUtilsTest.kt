package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TafqeetUtilsTest {

    @Test
    fun testBasicSpelling() {
        val spelled1000 = TafqeetUtils.spellAmount(1000.0, "ريال", includePrefixSuffix = false)
        assertEquals("ألف ريال", spelled1000)

        val spelled1000WithPrefix = TafqeetUtils.spellAmount(1000.0, "ريال", includePrefixSuffix = true)
        assertEquals("فقط ألف ريال لا غير", spelled1000WithPrefix)
    }

    @Test
    fun testCommonAmounts() {
        assertEquals("خمسة ريالات", TafqeetUtils.spellAmount(5.0, "ريال", false))
        assertEquals("عشرة ريالات", TafqeetUtils.spellAmount(10.0, "ريال", false))
        assertEquals("خمسة عشر ريال", TafqeetUtils.spellAmount(15.0, "ريال", false))
        assertEquals("مائة ريال", TafqeetUtils.spellAmount(100.0, "ريال", false))
        assertEquals("مائتان وخمسون ريال", TafqeetUtils.spellAmount(250.0, "ريال", false))
        assertEquals("ألف وخمسمائة ريال", TafqeetUtils.spellAmount(1500.0, "ريال", false))
        assertEquals("عشرة آلاف ريال", TafqeetUtils.spellAmount(10000.0, "ريال", false))
        assertEquals("خمسون ألف ريال", TafqeetUtils.spellAmount(50000.0, "ريال", false))
        assertEquals("مائة ألف ريال", TafqeetUtils.spellAmount(100000.0, "ريال", false))
        assertEquals("مليون ريال", TafqeetUtils.spellAmount(1000000.0, "ريال", false))
    }

    @Test
    fun testSpellFromInputString() {
        // Arabic digits
        val spelledArabicDigits = TafqeetUtils.spellFromInputString("١٠٠٠", "ريال")
        assertNotNull(spelledArabicDigits)
        assertTrue(spelledArabicDigits!!.contains("ألف ريال"))

        // Standard English digits
        val spelledStandard = TafqeetUtils.spellFromInputString("1000", "ريال")
        assertNotNull(spelledStandard)
        assertEquals("فقط ألف ريال لا غير", spelledStandard)

        // Blank or zero
        assertNull(TafqeetUtils.spellFromInputString(""))
        assertNull(TafqeetUtils.spellFromInputString("0"))
        assertNull(TafqeetUtils.spellFromInputString("   "))
    }
}
