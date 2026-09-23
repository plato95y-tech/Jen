package com.example.util

/**
 * محرك التفقيط العربي لتحويل الأرقام والمبالغ المالية إلى كلمات عربية فصيحة بدقة تامة.
 * يساعد التجار والعملاء على التحقق المباشر من الأصفار وصحة المبالغ المدخلة أثناء تسجيل الديون أو دفعات السداد.
 */
object TafqeetUtils {

    private val ones = arrayOf(
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"
    )

    private val tens = arrayOf(
        "", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"
    )

    private val teens = arrayOf(
        "عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    )

    private val hundreds = arrayOf(
        "", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة",
        "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    )

    /**
     * تحويل رقم من 1 إلى 999 إلى نص عربي
     */
    private fun convertHundreds(number: Int): String {
        if (number <= 0) return ""

        val h = number / 100
        val remainder = number % 100
        val t = remainder / 10
        val o = remainder % 10

        val parts = mutableListOf<String>()

        if (h > 0) {
            parts.add(hundreds[h])
        }

        if (remainder in 10..19) {
            parts.add(teens[remainder - 10])
        } else {
            if (o > 0) {
                parts.add(ones[o])
            }
            if (t > 0) {
                parts.add(tens[t])
            }
        }

        return parts.filter { it.isNotEmpty() }.joinToString(" و")
    }

    /**
     * تحويل عدد صحيح طويل (حتى المليارات) إلى نص عربي مجرد من العملة
     */
    fun convertNumberToArabicWords(number: Long): String {
        if (number == 0L) return "صفر"
        if (number < 0L) return "سالب " + convertNumberToArabicWords(-number)

        var num = number
        val billions = (num / 1_000_000_000L).toInt()
        num %= 1_000_000_000L

        val millions = (num / 1_000_000L).toInt()
        num %= 1_000_000L

        val thousands = (num / 1_000L).toInt()
        val remainder = (num % 1_000L).toInt()

        val parts = mutableListOf<String>()

        // 1. المليارات
        if (billions > 0) {
            val billionsText = when (billions) {
                1 -> "مليار"
                2 -> "ملياران"
                in 3..10 -> "${convertHundreds(billions)} مليارات"
                else -> "${convertHundreds(billions)} مليار"
            }
            parts.add(billionsText)
        }

        // 2. الملايين
        if (millions > 0) {
            val millionsText = when (millions) {
                1 -> "مليون"
                2 -> "مليونان"
                in 3..10 -> "${convertHundreds(millions)} ملايين"
                else -> "${convertHundreds(millions)} مليون"
            }
            parts.add(millionsText)
        }

        // 3. الآلاف
        if (thousands > 0) {
            val thousandsText = when (thousands) {
                1 -> "ألف"
                2 -> "ألفان"
                in 3..10 -> "${convertHundreds(thousands)} آلاف"
                else -> "${convertHundreds(thousands)} ألف"
            }
            parts.add(thousandsText)
        }

        // 4. المئات والآحاد
        if (remainder > 0) {
            parts.add(convertHundreds(remainder))
        }

        return parts.filter { it.isNotEmpty() }.joinToString(" و")
    }

    data class CurrencyInfo(
        val single: String,      // ريال
        val pair: String,        // ريالان
        val plural: String,      // ريالات
        val subSingle: String,   // هللة
        val subPlural: String    // هللات
    )

    fun resolveCurrency(currencySymbol: String): CurrencyInfo {
        val trimmed = currencySymbol.trim()
        return when {
            trimmed.contains("سعودي") || trimmed == "ر.س" || trimmed.equals("SAR", ignoreCase = true) ->
                CurrencyInfo("ريال سعودي", "ريالان سعوديان", "ريالات سعودية", "هللة", "هللات")

            trimmed.contains("يمني") || trimmed.equals("YER", ignoreCase = true) ->
                CurrencyInfo("ريال يمني", "ريالان يمنيان", "ريالات يمنية", "فلس", "فلوس")

            trimmed.contains("ريال") || trimmed == "ر.ع" || trimmed == "ر.ق" || trimmed.equals("QAR", ignoreCase = true) || trimmed.equals("OMR", ignoreCase = true) ->
                CurrencyInfo("ريال", "ريالان", "ريالات", "هللة", "هللات")

            trimmed.contains("كويتي") || trimmed == "د.ك" || trimmed.equals("KWD", ignoreCase = true) ->
                CurrencyInfo("دينار كويتي", "ديناران كويتيان", "دنانير كويتية", "فلس", "فلس")

            trimmed.contains("عراقي") || trimmed == "د.ع" || trimmed.equals("IQD", ignoreCase = true) ->
                CurrencyInfo("دينار عراقي", "ديناران عراقيان", "دنانير عراقية", "فلس", "فلس")

            trimmed.contains("أردني") || trimmed == "د.أ" || trimmed.equals("JOD", ignoreCase = true) ->
                CurrencyInfo("دينار أردني", "ديناران أردنيان", "دنانير أردنية", "قرش", "قروش")

            trimmed.contains("بحريني") || trimmed == "د.ب" || trimmed.equals("BHD", ignoreCase = true) ->
                CurrencyInfo("دينار بحريني", "ديناران بحرينيان", "دنانير بحرينية", "فلس", "فلس")

            trimmed.contains("دينار") ->
                CurrencyInfo("دينار", "ديناران", "دنانير", "فلس", "فلس")

            trimmed.contains("درهم") || trimmed == "د.إ" || trimmed.equals("AED", ignoreCase = true) ->
                CurrencyInfo("درهم إماراتي", "درهمان إماراتيان", "دراهم إماراتية", "فلس", "فلوس")

            trimmed.contains("جنيه") || trimmed == "ج.م" || trimmed.equals("EGP", ignoreCase = true) ->
                CurrencyInfo("جنيه مصري", "جنيهان مصريان", "جنيهات مصرية", "قرش", "قروش")

            trimmed.contains("دولار") || trimmed == "$" || trimmed.equals("USD", ignoreCase = true) ->
                CurrencyInfo("دولار", "دولاران", "دولارات", "سنت", "سنتات")

            trimmed.contains("يورو") || trimmed == "€" || trimmed.equals("EUR", ignoreCase = true) ->
                CurrencyInfo("يورو", "يورو", "يورو", "سنت", "سنتات")

            trimmed.isNotBlank() ->
                CurrencyInfo(trimmed, trimmed, trimmed, "جزء", "أجزاء")

            else ->
                CurrencyInfo("ريال", "ريالان", "ريالات", "هللة", "هللات")
        }
    }

    /**
     * إلحاق العملة بالمعدود مع مراعاة قواعد التمييز في النحو العربي:
     * - الأعداد من 3 إلى 10 (أو التي تنتهي من 3 إلى 10 ما عدا 100، 1000، 1000000) يتبعها جمع (ريالات).
     * - الأعداد المفردة 1: (ريال واحد)، 2: (ريالان).
     * - باقي الأعداد (11 إلى 99، والمئات والآلاف والملايين الصافية): مفرد (ريال).
     */
    private fun appendCurrency(amount: Long, words: String, currency: CurrencyInfo): String {
        if (amount == 1L) return currency.single
        if (amount == 2L) return currency.pair

        val lastTwo = (amount % 100).toInt()
        val lastThree = (amount % 1000).toInt()

        // هل ينتهي بصفر في المئات أو الآلاف؟ مثل 100، 1000، 5000، 10000
        val isRoundHundredOrThousand = (amount >= 100L && lastTwo == 0)

        val appropriateCurrency = when {
            isRoundHundredOrThousand -> currency.single // 100 ريال، 1000 ريال، 5000 ريال
            lastTwo in 3..10 -> currency.plural         // 5 ريالات، 105 ريالات، 1010 ريالات
            else -> currency.single                     // 15 ريالاً، 25 ريالاً، 1000 ريال
        }

        return "$words $appropriateCurrency"
    }

    /**
     * تحويل المبلغ المالي كاملاً (مع العملة والكسور) إلى صياغة تفقيط عربية معتمدة
     * مثال: 1000 -> "فقط ألف ريال لا غير"
     * مثال: 1500 -> "فقط ألف وخمسمائة ريال لا غير"
     */
    fun spellAmount(
        amount: Double,
        currencySymbol: String = "ريال",
        includePrefixSuffix: Boolean = true
    ): String {
        if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
            return ""
        }

        val currency = resolveCurrency(currencySymbol)
        val mainPart = amount.toLong()
        val decimalPart = Math.round((amount - mainPart) * 100).toInt().coerceIn(0, 99)

        val mainWords = if (mainPart > 0L) {
            val words = convertNumberToArabicWords(mainPart)
            appendCurrency(mainPart, words, currency)
        } else ""

        val subWords = if (decimalPart > 0) {
            val fractionWords = convertNumberToArabicWords(decimalPart.toLong())
            val subText = when (decimalPart) {
                1 -> currency.subSingle
                2 -> currency.subSingle
                in 3..10 -> currency.subPlural
                else -> currency.subSingle
            }
            "$fractionWords $subText"
        } else ""

        val combined = when {
            mainWords.isNotEmpty() && subWords.isNotEmpty() -> "$mainWords و$subWords"
            mainWords.isNotEmpty() -> mainWords
            subWords.isNotEmpty() -> subWords
            else -> "صفر ${currency.single}"
        }

        return if (includePrefixSuffix) {
            "فقط $combined لا غير"
        } else {
            combined
        }
    }

    /**
     * تفقيط فوري مبسط للنصوص المدخلة أثناء الكتابة في حقول المبالغ المالية.
     * يدعم الفواصل العشرية، المسافات، والأرقام باللغة العربية والإنجليزية.
     */
    fun spellFromInputString(
        rawInput: String,
        currencySymbol: String = "ريال",
        includePrefixSuffix: Boolean = true
    ): String? {
        val sanitized = rawInput
            .replace("،", ".")
            .replace(",", ".")
            .replace("٫", ".")
            .replace("٠", "0")
            .replace("١", "1")
            .replace("٢", "2")
            .replace("٣", "3")
            .replace("٤", "4")
            .replace("٥", "5")
            .replace("٦", "6")
            .replace("٧", "7")
            .replace("٨", "8")
            .replace("٩", "9")
            .trim()

        if (sanitized.isBlank()) return null

        val value = sanitized.toDoubleOrNull() ?: return null
        if (value <= 0.0) return null

        return spellAmount(value, currencySymbol, includePrefixSuffix = includePrefixSuffix)
    }
}
