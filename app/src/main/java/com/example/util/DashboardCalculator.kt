package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.dao.StoreWithBalance
import com.example.data.entity.CurrencyEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.data.model.DashboardAnalytics
import com.example.data.model.MonthlyFinancialData
import com.example.data.model.StoreDebtShare
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DashboardCalculator {

    val sliceColors = listOf(
        Color(0xFF2563EB), // Vibrant Royal Blue
        Color(0xFF059669), // Vibrant Emerald
        Color(0xFFD97706), // Warm Amber
        Color(0xFFDC2626), // Coral Crimson
        Color(0xFF7C3AED), // Rich Violet
        Color(0xFF0891B2), // Deep Cyan
        Color(0xFFDB2777), // Deep Rose
        Color(0xFF64748B)  // Slate (Others)
    )

    private val arabicMonthNames = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun calculateAnalytics(
        stores: List<StoreWithBalance>,
        transactions: List<TransactionEntity>,
        currencies: List<CurrencyEntity>,
        defaultCurrencyCode: String,
        useLatestRate: Boolean,
        monthsCount: Int = 6
    ): DashboardAnalytics {
        val rateMap = currencies.associate { it.code to it.exchangeRate }

        var totalDebt = 0.0
        var totalPayment = 0.0

        // Map of "yyyy-MM" to (debt, payment, txCount)
        val monthlyMap = mutableMapOf<String, Triple<Double, Double, Int>>()

        for (tx in transactions) {
            val converted = CurrencyUtils.convertToDefaultCurrency(
                amount = tx.amount,
                txCurrencyCode = tx.currencyCode,
                txExchangeRate = tx.exchangeRate,
                defaultCurrencyCode = defaultCurrencyCode,
                useLatestRateForAll = useLatestRate,
                latestRateMap = rateMap
            )

            if (tx.type == TransactionType.DEBT) {
                totalDebt += converted
            } else {
                totalPayment += converted
            }

            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1 // 1..12
            val key = String.format(Locale.US, "%04d-%02d", year, month)

            val current = monthlyMap[key] ?: Triple(0.0, 0.0, 0)
            if (tx.type == TransactionType.DEBT) {
                monthlyMap[key] = Triple(current.first + converted, current.second, current.third + 1)
            } else {
                monthlyMap[key] = Triple(current.first, current.second + converted, current.third + 1)
            }
        }

        val netBalance = (totalDebt - totalPayment).coerceAtLeast(0.0)

        // Build continuous sequence of the past `monthsCount` months ending in current month
        val monthlyDataList = mutableListOf<MonthlyFinancialData>()
        val cal = Calendar.getInstance()
        // Go back (monthsCount - 1) months
        cal.add(Calendar.MONTH, -(monthsCount - 1))

        for (i in 0 until monthsCount) {
            val year = cal.get(Calendar.YEAR)
            val monthIdx = cal.get(Calendar.MONTH) // 0..11
            val key = String.format(Locale.US, "%04d-%02d", year, monthIdx + 1)
            val monthName = arabicMonthNames.getOrElse(monthIdx) { "" }
            val yearShort = (year % 100).toString()

            val entry = monthlyMap[key] ?: Triple(0.0, 0.0, 0)
            val mDebt = entry.first
            val mPay = entry.second
            val txCount = entry.third

            monthlyDataList.add(
                MonthlyFinancialData(
                    yearMonth = key,
                    monthLabel = monthName,
                    fullLabel = "$monthName $year",
                    totalDebt = mDebt,
                    totalPayment = mPay,
                    netChange = mDebt - mPay,
                    transactionCount = txCount
                )
            )

            cal.add(Calendar.MONTH, 1)
        }

        // Calculate store debt distribution (Donut / Pie Chart)
        val storesWithPositiveBalance = stores.filter { it.remainingBalance > 0.001 }
            .sortedByDescending { it.remainingBalance }

        val totalOutstanding = storesWithPositiveBalance.sumOf { it.remainingBalance }

        val storeShares = mutableListOf<StoreDebtShare>()
        if (totalOutstanding > 0) {
            val topLimit = 5
            val topStores = storesWithPositiveBalance.take(topLimit)
            val otherStores = storesWithPositiveBalance.drop(topLimit)

            topStores.forEachIndexed { index, store ->
                val pct = ((store.remainingBalance / totalOutstanding) * 100).toFloat()
                storeShares.add(
                    StoreDebtShare(
                        storeId = store.id,
                        storeName = store.name,
                        debtAmount = store.remainingBalance,
                        percentage = pct,
                        color = sliceColors.getOrElse(index) { sliceColors.last() }
                    )
                )
            }

            if (otherStores.isNotEmpty()) {
                val othersTotal = otherStores.sumOf { it.remainingBalance }
                val othersPct = ((othersTotal / totalOutstanding) * 100).toFloat()
                storeShares.add(
                    StoreDebtShare(
                        storeId = -1L,
                        storeName = "عملاء آخرون (${otherStores.size})",
                        debtAmount = othersTotal,
                        percentage = othersPct,
                        color = sliceColors.last()
                    )
                )
            }
        }

        // Overdue count
        val now = System.currentTimeMillis()
        val overdueCount = stores.count { it.dueDate != null && it.dueDate <= now && it.remainingBalance > 0.001 }

        val collectionRate = if (totalDebt > 0.0) {
            ((totalPayment / totalDebt) * 100).toInt().coerceIn(0, 100)
        } else 100

        val activeDebtStoresCount = stores.count { it.remainingBalance > 0.001 }
        val settledStoresCount = stores.count { it.remainingBalance <= 0.001 && it.transactionCount > 0 }

        return DashboardAnalytics(
            totalDebt = totalDebt,
            totalPayment = totalPayment,
            netBalance = netBalance,
            overdueCount = overdueCount,
            collectionRate = collectionRate,
            monthlyData = monthlyDataList,
            storeShares = storeShares,
            activeDebtStoresCount = activeDebtStoresCount,
            settledStoresCount = settledStoresCount
        )
    }
}
