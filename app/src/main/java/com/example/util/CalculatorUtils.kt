package com.example.util

import java.text.DecimalFormat

object CalculatorUtils {

    /**
     * Evaluates a mathematical expression string containing +, -, *, /, decimals, and parentheses.
     * Returns null if the expression is invalid or incomplete.
     */
    fun evaluate(expression: String): Double? {
        val sanitized = normalizeDigits(expression)
            .replace("×", "*")
            .replace("÷", "/")
            .replace("،", ".")
            .replace(" ", "")

        if (sanitized.isBlank()) return null

        return try {
            val parser = SimpleMathParser(sanitized)
            val result = parser.parse()
            if (result.isInfinite() || result.isNaN()) null else result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the given string looks like an active math expression containing operators.
     */
    fun isMathExpression(expression: String): Boolean {
        val sanitized = normalizeDigits(expression).replace("×", "*").replace("÷", "/")
        val hasOperator = sanitized.any { it in "+-*/" }
        val hasDigits = sanitized.any { it.isDigit() }
        return hasOperator && hasDigits
    }

    private fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            when (c) {
                in '٠'..'٩' -> sb.append((c - '٠').toString())
                in '۰'..'۹' -> sb.append((c - '۰').toString())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Formats evaluated double into a clean display string without unnecessary trailing zeros.
     */
    fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            val df = DecimalFormat("#.##")
            df.format(value)
        }
    }

    private class SimpleMathParser(private val input: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Unexpected: " + ch.toChar())
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing closing parenthesis")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                val numStr = input.substring(startPos, pos)
                x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
            } else {
                throw IllegalArgumentException("Unexpected character: " + ch.toChar())
            }
            return x
        }
    }
}
