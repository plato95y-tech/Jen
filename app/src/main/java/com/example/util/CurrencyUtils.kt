package com.example.util

import com.example.data.entity.CurrencyEntity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyPreset(
    val code: String,
    val name: String,
    val symbol: String,
    val defaultRateToSar: Double
)

object CurrencyUtils {
    val PRESETS = listOf(
        CurrencyPreset("SAR", "ريال سعودي", "ر.س", 1.0),
        CurrencyPreset("YER", "ريال يمني", "ر.ي", 0.0025), // 1 SAR = 400 YER
        CurrencyPreset("USD", "دولار أمريكي", "$", 3.75),
        CurrencyPreset("AED", "درهم إماراتي", "د.إ", 1.02),
        CurrencyPreset("EGP", "جنيه مصري", "ج.م", 0.076),
        CurrencyPreset("KWD", "دينار كويتي", "د.ك", 12.25),
        CurrencyPreset("EUR", "يورو", "€", 4.10),
        CurrencyPreset("OMR", "ريال عماني", "ر.ع", 9.75),
        CurrencyPreset("QAR", "ريال قطري", "ر.ق", 1.03),
        CurrencyPreset("BHD", "دينار بحريني", "د.ب", 9.95),
        CurrencyPreset("JOD", "دينار أردني", "د.أ", 5.29),
        CurrencyPreset("TRY", "ليرة تركية", "₺", 0.11)
    )

    fun getDefaultCurrenciesList(): List<CurrencyEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CurrencyEntity(code = "SAR", name = "ريال سعودي", symbol = "ر.س", exchangeRate = 1.0, isDefault = true, updatedAt = now),
            CurrencyEntity(code = "YER", name = "ريال يمني", symbol = "ر.ي", exchangeRate = 0.0025, isDefault = false, updatedAt = now),
            CurrencyEntity(code = "USD", name = "دولار أمريكي", symbol = "$", exchangeRate = 3.75, isDefault = false, updatedAt = now),
            CurrencyEntity(code = "AED", name = "درهم إماراتي", symbol = "د.إ", exchangeRate = 1.02, isDefault = false, updatedAt = now),
            CurrencyEntity(code = "EGP", name = "جنيه مصري", symbol = "ج.م", exchangeRate = 0.076, isDefault = false, updatedAt = now),
            CurrencyEntity(code = "KWD", name = "دينار كويتي", symbol = "د.ك", exchangeRate = 12.25, isDefault = false, updatedAt = now)
        )
    }

    /**
     * Converts a transaction amount to the default currency.
     * If useLatestRateForAll is true -> uses the latest rate from the active currencies map.
     * If useLatestRateForAll is false (historical mode) -> uses the rate stored with the transaction at creation time.
     */
    fun convertToDefaultCurrency(
        amount: Double,
        txCurrencyCode: String,
        txExchangeRate: Double,
        defaultCurrencyCode: String,
        useLatestRateForAll: Boolean,
        latestRateMap: Map<String, Double>
    ): Double {
        if (txCurrencyCode.isBlank() || txCurrencyCode.equals(defaultCurrencyCode, ignoreCase = true)) {
            return amount
        }
        val rate = if (useLatestRateForAll) {
            latestRateMap[txCurrencyCode] ?: (if (txExchangeRate > 0) txExchangeRate else 1.0)
        } else {
            if (txExchangeRate > 0) txExchangeRate else (latestRateMap[txCurrencyCode] ?: 1.0)
        }
        return amount * rate
    }

    /**
     * User-friendly description of the exchange rate.
     * e.g. "1 USD = 3.75 SAR" or "1 SAR = 400 YER (1 YER = 0.0025 SAR)"
     */
    fun formatRateRelation(
        currencyCode: String,
        currencySymbol: String,
        rateToDefault: Double,
        defaultCurrencyCode: String,
        defaultCurrencySymbol: String
    ): String {
        val df = DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.US))
        if (currencyCode.equals(defaultCurrencyCode, ignoreCase = true)) {
            return "العملة الأساسية الافتراضية (1.0)"
        }
        return if (rateToDefault >= 1.0) {
            "1 $currencySymbol = ${df.format(rateToDefault)} $defaultCurrencySymbol"
        } else if (rateToDefault > 0) {
            val reciprocal = 1.0 / rateToDefault
            val dfReciprocal = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
            "1 $defaultCurrencySymbol = ${dfReciprocal.format(reciprocal)} $currencySymbol"
        } else {
            "1 $currencySymbol = 0 $defaultCurrencySymbol"
        }
    }
}
