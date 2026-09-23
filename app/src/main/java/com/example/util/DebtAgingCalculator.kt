package com.example.util

import com.example.data.dao.StoreWithBalance
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import kotlin.math.roundToInt

data class DebtAgingSummary(
    val currentPeriodAmount: Double,       // < 30 days
    val currentPeriodCount: Int,
    val currentPeriodPercent: Double,

    val period30To60Amount: Double,        // 31 - 60 days
    val period30To60Count: Int,
    val period30To60Percent: Double,

    val period60To90Amount: Double,        // 61 - 90 days
    val period60To90Count: Int,
    val period60To90Percent: Double,

    val overdue90PlusAmount: Double,       // > 90 days
    val overdue90PlusCount: Int,
    val overdue90PlusPercent: Double,

    val totalOutstandingDebt: Double,
    val totalPaid: Double,
    val totalCumulativeDebt: Double,
    val collectionRatePercent: Double,
    val financialHealthGrade: String,      // "ممتاز", "جيد", "متوسط", "حرج"
    val financialHealthColorHex: String,
    val financialHealthAdvice: String
)

data class StoreDebtRisk(
    val storeId: Long,
    val storeName: String,
    val phone: String,
    val remainingBalance: Double,
    val debtLimit: Double,
    val percentageOfTotalDebt: Double,
    val oldestDebtDays: Int,
    val isOverLimit: Boolean,
    val isOverdue: Boolean,
    val riskLevel: RiskLevel
)

enum class RiskLevel(val label: String, val colorHex: String) {
    LOW("منخفض", "#2E7D32"),
    MEDIUM("متوسط", "#F57C00"),
    HIGH("مرتفع", "#E65100"),
    CRITICAL("حرج جداً", "#C62828")
}

data class MonthlyComparison(
    val thisMonthDebts: Double,
    val thisMonthPayments: Double,
    val lastMonthDebts: Double,
    val lastMonthPayments: Double,
    val debtChangePercent: Double,
    val paymentChangePercent: Double
)

object DebtAgingCalculator {

    private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L

    fun calculateAging(
        storesWithBalances: List<StoreWithBalance>,
        transactions: List<TransactionEntity>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): DebtAgingSummary {
        var currentAmount = 0.0
        var currentCount = 0

        var p30To60Amount = 0.0
        var p30To60Count = 0

        var p60To90Amount = 0.0
        var p60To90Count = 0

        var p90PlusAmount = 0.0
        var p90PlusCount = 0

        var totalOutstanding = 0.0
        var totalPaid = 0.0
        var totalCumulativeDebt = 0.0

        // Map transactions by store
        val txsByStore = transactions.groupBy { it.storeId }

        for (item in storesWithBalances) {
            val balance = item.remainingBalance
            if (balance <= 0.0) {
                totalPaid += item.totalPaid
                totalCumulativeDebt += item.totalDebt
                continue
            }

            totalOutstanding += balance
            totalPaid += item.totalPaid
            totalCumulativeDebt += item.totalDebt

            // FIFO allocation of remaining unpaid debt against store's debt transactions
            val storeTxs = txsByStore[item.id]?.filter { it.type == TransactionType.DEBT }
                ?.sortedBy { it.timestamp } ?: emptyList()

            var remainingToAllocate = balance

            for (tx in storeTxs.reversed()) {
                if (remainingToAllocate <= 0.0) break
                val portion = minOf(remainingToAllocate, tx.amount)
                val ageDays = ((currentTimeMillis - tx.timestamp) / DAY_IN_MILLIS).coerceAtLeast(0)

                when {
                    ageDays <= 30 -> {
                        currentAmount += portion
                        currentCount++
                    }
                    ageDays in 31..60 -> {
                        p30To60Amount += portion
                        p30To60Count++
                    }
                    ageDays in 61..90 -> {
                        p60To90Amount += portion
                        p60To90Count++
                    }
                    else -> {
                        p90PlusAmount += portion
                        p90PlusCount++
                    }
                }
                remainingToAllocate -= portion
            }

            // If some balance remained unallocated (e.g. initial debt without tx), place in current or 30+
            if (remainingToAllocate > 0.0) {
                val storeAgeDays = ((currentTimeMillis - item.createdAt) / DAY_IN_MILLIS).coerceAtLeast(0)
                when {
                    storeAgeDays <= 30 -> {
                        currentAmount += remainingToAllocate
                        currentCount++
                    }
                    storeAgeDays in 31..60 -> {
                        p30To60Amount += remainingToAllocate
                        p30To60Count++
                    }
                    storeAgeDays in 61..90 -> {
                        p60To90Amount += remainingToAllocate
                        p60To90Count++
                    }
                    else -> {
                        p90PlusAmount += remainingToAllocate
                        p90PlusCount++
                    }
                }
            }
        }

        val totalForPercents = if (totalOutstanding > 0.0) totalOutstanding else 1.0
        val currentPercent = (currentAmount / totalForPercents) * 100.0
        val p30To60Percent = (p30To60Amount / totalForPercents) * 100.0
        val p60To90Percent = (p60To90Amount / totalForPercents) * 100.0
        val p90PlusPercent = (p90PlusAmount / totalForPercents) * 100.0

        val collectionRate = if (totalCumulativeDebt > 0.0) {
            (totalPaid / totalCumulativeDebt) * 100.0
        } else {
            100.0
        }

        // Evaluate Financial Health Grade & Advice
        val (grade, color, advice) = evaluateHealth(
            collectionRate = collectionRate,
            overdue90Percent = p90PlusPercent,
            totalOutstanding = totalOutstanding
        )

        return DebtAgingSummary(
            currentPeriodAmount = currentAmount,
            currentPeriodCount = currentCount,
            currentPeriodPercent = (currentPercent * 10).roundToInt() / 10.0,
            period30To60Amount = p30To60Amount,
            period30To60Count = p30To60Count,
            period30To60Percent = (p30To60Percent * 10).roundToInt() / 10.0,
            period60To90Amount = p60To90Amount,
            period60To90Count = p60To90Count,
            period60To90Percent = (p60To90Percent * 10).roundToInt() / 10.0,
            overdue90PlusAmount = p90PlusAmount,
            overdue90PlusCount = p90PlusCount,
            overdue90PlusPercent = (p90PlusPercent * 10).roundToInt() / 10.0,
            totalOutstandingDebt = totalOutstanding,
            totalPaid = totalPaid,
            totalCumulativeDebt = totalCumulativeDebt,
            collectionRatePercent = (collectionRate * 10).roundToInt() / 10.0,
            financialHealthGrade = grade,
            financialHealthColorHex = color,
            financialHealthAdvice = advice
        )
    }

    private fun evaluateHealth(
        collectionRate: Double,
        overdue90Percent: Double,
        totalOutstanding: Double
    ): Triple<String, String, String> {
        if (totalOutstanding <= 0.0) {
            return Triple("ممتاز 100%", "#2E7D32", "لا توجد أي ديون مستحقة، جميع الحسابات مسددة بالكامل.")
        }

        return when {
            collectionRate >= 75.0 && overdue90Percent < 10.0 -> {
                Triple(
                    "ممتاز",
                    "#2E7D32",
                    "معدل التحصيل ممتاز ونسبة الديون المتعثرة ضئيلة جداً، استمر على هذه السياسة الائتمانية."
                )
            }
            collectionRate >= 50.0 && overdue90Percent < 25.0 -> {
                Triple(
                    "جيد ومستقر",
                    "#00796B",
                    "معدل التحصيل جيد. ركز على متابعة العملاء المتأخرين بين 30 و 60 يوماً لمنع تعثرها."
                )
            }
            collectionRate >= 30.0 || overdue90Percent < 45.0 -> {
                Triple(
                    "مخاطر متوسطة",
                    "#F57C00",
                    "يوجد تباطؤ في السداد وتراكم لبعض الديون القديمة. يُنصح بإرسال تذكيرات سداد ووضع سقوف ديون صارمة."
                )
            }
            else -> {
                Triple(
                    "مخاطر مرتفعة",
                    "#C62828",
                    "نسبة الديون المتعثرة (+90 يوماً) مرتفعة وتتطلب تدخلاً عاجلاً لإيقاف منح ديون جديدة وتكثيف التحصيل."
                )
            }
        }
    }

    fun rankStoreDebts(
        storesWithBalances: List<StoreWithBalance>,
        transactions: List<TransactionEntity>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<StoreDebtRisk> {
        val totalDebt = storesWithBalances.sumOf { it.remainingBalance.coerceAtLeast(0.0) }
        val txsByStore = transactions.groupBy { it.storeId }

        val rankedList = storesWithBalances
            .filter { it.remainingBalance > 0 }
            .map { item ->
                val storeTxs = txsByStore[item.id]?.filter { it.type == TransactionType.DEBT }
                val oldestTx = storeTxs?.minByOrNull { it.timestamp }
                val oldestDays = if (oldestTx != null) {
                    ((currentTimeMillis - oldestTx.timestamp) / DAY_IN_MILLIS).toInt().coerceAtLeast(0)
                } else {
                    ((currentTimeMillis - item.createdAt) / DAY_IN_MILLIS).toInt().coerceAtLeast(0)
                }

                val percent = if (totalDebt > 0.0) {
                    ((item.remainingBalance / totalDebt) * 1000.0).roundToInt() / 10.0
                } else 0.0

                val isOverLimit = item.debtLimit > 0 && item.remainingBalance > item.debtLimit
                val isOverdue = item.dueDate != null && item.dueDate <= currentTimeMillis

                val risk = when {
                    isOverLimit || oldestDays > 90 || (isOverdue && oldestDays > 60) -> RiskLevel.CRITICAL
                    oldestDays > 60 || isOverdue -> RiskLevel.HIGH
                    oldestDays > 30 -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }

                StoreDebtRisk(
                    storeId = item.id,
                    storeName = item.name,
                    phone = item.phone,
                    remainingBalance = item.remainingBalance,
                    debtLimit = item.debtLimit,
                    percentageOfTotalDebt = percent,
                    oldestDebtDays = oldestDays,
                    isOverLimit = isOverLimit,
                    isOverdue = isOverdue,
                    riskLevel = risk
                )
            }
            .sortedByDescending { it.remainingBalance }

        return rankedList
    }
}
