package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val arabicLocale = Locale.forLanguageTag("ar")
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", arabicLocale)
    private val timeFormat = SimpleDateFormat("hh:mm a", arabicLocale)
    private val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", arabicLocale)

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatAmount(amount: Double, isPrivacyMode: Boolean = false): String {
        if (isPrivacyMode) return "••••••"
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%,.0f", amount)
        } else {
            String.format(Locale.US, "%,.2f", amount)
        }
    }

    fun formatCurrency(amount: Double, currency: String, isPrivacyMode: Boolean = false): String {
        if (isPrivacyMode) return "•••••• $currency"
        return "${formatAmount(amount)} $currency"
    }

    fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }
}

