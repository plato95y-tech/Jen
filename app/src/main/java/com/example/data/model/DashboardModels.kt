package com.example.data.model

import androidx.compose.ui.graphics.Color

data class MonthlyFinancialData(
    val yearMonth: String,       // e.g. "2026-09"
    val monthLabel: String,      // e.g. "سبتمبر"
    val fullLabel: String,       // e.g. "سبتمبر 2026"
    val totalDebt: Double,       // in default currency
    val totalPayment: Double,    // in default currency
    val netChange: Double,       // totalDebt - totalPayment
    val transactionCount: Int
)

data class StoreDebtShare(
    val storeId: Long,
    val storeName: String,
    val debtAmount: Double,
    val percentage: Float,       // 0..100
    val color: Color
)

data class DashboardAnalytics(
    val totalDebt: Double,
    val totalPayment: Double,
    val netBalance: Double,
    val overdueCount: Int,
    val collectionRate: Int,     // 0..100%
    val monthlyData: List<MonthlyFinancialData>,
    val storeShares: List<StoreDebtShare>,
    val activeDebtStoresCount: Int,
    val settledStoresCount: Int
)
