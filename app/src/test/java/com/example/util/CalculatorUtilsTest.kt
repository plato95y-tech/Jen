package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorUtilsTest {

    @Test
    fun testSimpleAddition() {
        val result = CalculatorUtils.evaluate("150+30")
        assertNotNull(result)
        assertEquals(180.0, result!!, 0.001)
    }

    @Test
    fun testSubtractionAndMultiplication() {
        val result = CalculatorUtils.evaluate("200-50*2")
        assertNotNull(result)
        assertEquals(100.0, result!!, 0.001)
    }

    @Test
    fun testParentheses() {
        val result = CalculatorUtils.evaluate("(200-50)*2")
        assertNotNull(result)
        assertEquals(300.0, result!!, 0.001)
    }

    @Test
    fun testArabicDigitsConversion() {
        val result = CalculatorUtils.evaluate("١٥٠+٣٠")
        assertNotNull(result)
        assertEquals(180.0, result!!, 0.001)
    }

    @Test
    fun testIsMathExpression() {
        assertTrue(CalculatorUtils.isMathExpression("150+30"))
        assertTrue(CalculatorUtils.isMathExpression("50*2"))
        assertFalse(CalculatorUtils.isMathExpression("150"))
        assertFalse(CalculatorUtils.isMathExpression("150.5"))
    }

    @Test
    fun testFormatResult() {
        assertEquals("180", CalculatorUtils.formatResult(180.0))
        assertEquals("180.5", CalculatorUtils.formatResult(180.5))
    }
}
