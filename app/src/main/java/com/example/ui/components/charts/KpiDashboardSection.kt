package com.example.ui.components.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FormatUtils
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedContainer
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun KpiDashboardSection(
    totalDebt: Double,
    totalPaid: Double,
    netBalance: Double,
    overdueCount: Int,
    currency: String,
    onOverdueClick: () -> Unit,
    onViewChartsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isPrivacyMode: Boolean = false,
    activeDebtStoresCount: Int = 0
) {
    val collectionRate = if (totalDebt > 0.0) {
        ((totalPaid / totalDebt) * 100).toInt().coerceIn(0, 100)
    } else 100

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header with Shortcut to Charts & Analytics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "مؤشرات الأداء المالي (KPIs)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (onViewChartsClick != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onViewChartsClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "الرسوم البيانية",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // 2x2 Grid of KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Total Debt (إجمالي الديون)
            KpiCard(
                title = "إجمالي الديون",
                value = FormatUtils.formatCurrency(totalDebt, currency, isPrivacyMode),
                subtitle = "المقيدة كديون جديدة",
                icon = Icons.Default.TrendingUp,
                iconTint = DebtRed,
                iconBg = DebtRedContainer,
                modifier = Modifier.weight(1f).testTag("kpi_total_debt")
            )

            // 2. Total Paid (إجمالي السدادات)
            KpiCard(
                title = "إجمالي السدادات",
                value = FormatUtils.formatCurrency(totalPaid, currency, isPrivacyMode),
                subtitle = "$collectionRate% نسبة التحصيل",
                icon = Icons.Default.TrendingDown,
                iconTint = PaymentGreen,
                iconBg = PaymentGreenContainer,
                modifier = Modifier.weight(1f).testTag("kpi_total_paid")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. Net Balance (صافي الرصيد)
            KpiCard(
                title = "صافي الرصيد",
                value = FormatUtils.formatCurrency(netBalance, currency, isPrivacyMode),
                subtitle = if (activeDebtStoresCount > 0) "$activeDebtStoresCount عميل متبقي" else "الكل مسدد",
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f).testTag("kpi_net_balance")
            )

            // 4. Overdue Clients (عدد العملاء المتأخرين)
            KpiCard(
                title = "العملاء المتأخرين",
                value = if (overdueCount > 0) "$overdueCount عملاء" else "لا يوجد متأخرين",
                subtitle = if (overdueCount > 0) "اضغط للتصفية السريعة" else "التزام تام بالمواعيد",
                icon = Icons.Default.NotificationsActive,
                iconTint = if (overdueCount > 0) WarningAmber else PaymentGreen,
                iconBg = if (overdueCount > 0) WarningAmberContainer else PaymentGreenContainer,
                onClick = onOverdueClick,
                isWarning = overdueCount > 0,
                modifier = Modifier.weight(1f).testTag("kpi_overdue_clients")
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isWarning: Boolean = false
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = if (isWarning) BorderStroke(1.2.dp, iconTint.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isWarning) iconTint else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isWarning) iconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
