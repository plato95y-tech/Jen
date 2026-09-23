package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FormatUtils
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.WarningAmber
import com.example.util.DebtAgingSummary
import com.example.util.RiskLevel
import com.example.util.StoreDebtRisk

@Composable
fun DebtAgingTabContent(
    agingSummary: DebtAgingSummary,
    storeRisks: List<StoreDebtRisk>,
    currency: String,
    isPrivacyMode: Boolean,
    onStoreClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var riskFilter by remember { mutableStateOf<RiskLevel?>(null) } // null = all

    val filteredRisks = remember(storeRisks, riskFilter) {
        if (riskFilter == null) storeRisks
        else storeRisks.filter { it.riskLevel == riskFilter }
    }

    val healthColor = remember(agingSummary.financialHealthColorHex) {
        try {
            Color(android.graphics.Color.parseColor(agingSummary.financialHealthColorHex))
        } catch (_: Exception) {
            PaymentGreen
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Financial Health & Recovery Rate Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(healthColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = healthColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "مؤشر السلامة المالية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تقييم الوضع الائتماني وسرعة التحصيل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = healthColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = agingSummary.financialHealthGrade,
                                color = healthColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Recovery Rate Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معدل التحصيل التراكمي:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${agingSummary.collectionRatePercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = healthColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (agingSummary.collectionRatePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = healthColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Stat Badges: Outstanding, Recovered, Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill(
                            title = "ديون قائمة",
                            amount = if (isPrivacyMode) "••••" else FormatUtils.formatCurrency(agingSummary.totalOutstandingDebt, currency),
                            color = DebtRed,
                            modifier = Modifier.weight(1f)
                        )
                        StatPill(
                            title = "تم تحصيله",
                            amount = if (isPrivacyMode) "••••" else FormatUtils.formatCurrency(agingSummary.totalPaid, currency),
                            color = PaymentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatPill(
                            title = "إجمالي الائتمان",
                            amount = if (isPrivacyMode) "••••" else FormatUtils.formatCurrency(agingSummary.totalCumulativeDebt, currency),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Advice Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = agingSummary.financialHealthAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Debt Aging Buckets Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تحليل أعمار الديون (Aging Report)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "توزيع المبالغ غير المسددة حسب مدة تأخرها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Multi-segment Proportional Distribution Bar
                    if (agingSummary.totalOutstandingDebt > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (agingSummary.currentPeriodPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(agingSummary.currentPeriodPercent.toFloat().coerceAtLeast(0.1f))
                                        .fillMaxHeight()
                                        .background(PaymentGreen)
                                )
                            }
                            if (agingSummary.period30To60Percent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(agingSummary.period30To60Percent.toFloat().coerceAtLeast(0.1f))
                                        .fillMaxHeight()
                                        .background(Color(0xFF0288D1)) // Blue
                                )
                            }
                            if (agingSummary.period60To90Percent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(agingSummary.period60To90Percent.toFloat().coerceAtLeast(0.1f))
                                        .fillMaxHeight()
                                        .background(WarningAmber) // Amber
                                )
                            }
                            if (agingSummary.overdue90PlusPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(agingSummary.overdue90PlusPercent.toFloat().coerceAtLeast(0.1f))
                                        .fillMaxHeight()
                                        .background(DebtRed) // Red
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 4 Buckets Detailed List
                    AgingBucketRow(
                        label = "أقل من 30 يوماً (ديون جارية)",
                        amount = agingSummary.currentPeriodAmount,
                        count = agingSummary.currentPeriodCount,
                        percent = agingSummary.currentPeriodPercent,
                        color = PaymentGreen,
                        currency = currency,
                        isPrivacyMode = isPrivacyMode
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    AgingBucketRow(
                        label = "من 31 إلى 60 يوماً (مستحقة قريباً)",
                        amount = agingSummary.period30To60Amount,
                        count = agingSummary.period30To60Count,
                        percent = agingSummary.period30To60Percent,
                        color = Color(0xFF0288D1),
                        currency = currency,
                        isPrivacyMode = isPrivacyMode
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    AgingBucketRow(
                        label = "من 61 إلى 90 يوماً (تأخر في السداد)",
                        amount = agingSummary.period60To90Amount,
                        count = agingSummary.period60To90Count,
                        percent = agingSummary.period60To90Percent,
                        color = WarningAmber,
                        currency = currency,
                        isPrivacyMode = isPrivacyMode
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    AgingBucketRow(
                        label = "أكثر من 90 يوماً (ديون متعثرة / حرجة)",
                        amount = agingSummary.overdue90PlusAmount,
                        count = agingSummary.overdue90PlusCount,
                        percent = agingSummary.overdue90PlusPercent,
                        color = DebtRed,
                        currency = currency,
                        isPrivacyMode = isPrivacyMode
                    )
                }
            }
        }

        // Section Header for Customer Risk Ranking
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تصنيف العملاء والمخاطر الائتمانية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredRisks.size} عميل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Risk Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = riskFilter == null,
                        onClick = { riskFilter = null },
                        label = { Text("الكل (${storeRisks.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = riskFilter == RiskLevel.CRITICAL,
                        onClick = { riskFilter = if (riskFilter == RiskLevel.CRITICAL) null else RiskLevel.CRITICAL },
                        label = { Text("حرج جداً", fontSize = 11.sp, color = DebtRed) }
                    )
                    FilterChip(
                        selected = riskFilter == RiskLevel.HIGH,
                        onClick = { riskFilter = if (riskFilter == RiskLevel.HIGH) null else RiskLevel.HIGH },
                        label = { Text("مرتفع", fontSize = 11.sp, color = WarningAmber) }
                    )
                    FilterChip(
                        selected = riskFilter == RiskLevel.LOW,
                        onClick = { riskFilter = if (riskFilter == RiskLevel.LOW) null else RiskLevel.LOW },
                        label = { Text("منخفض", fontSize = 11.sp, color = PaymentGreen) }
                    )
                }
            }
        }

        // Customer Debt Risk Cards
        if (filteredRisks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PaymentGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد حسابات تحت هذا التصنيف",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(filteredRisks, key = { it.storeId }) { riskItem ->
                StoreRiskCard(
                    item = riskItem,
                    currency = currency,
                    isPrivacyMode = isPrivacyMode,
                    onClick = { onStoreClick(riskItem.storeId) }
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    title: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun AgingBucketRow(
    label: String,
    amount: Double,
    count: Int,
    percent: Double,
    color: Color,
    currency: String,
    isPrivacyMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count معاملة • $percent% من المحفظة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = if (isPrivacyMode) "••••" else FormatUtils.formatCurrency(amount, currency),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StoreRiskCard(
    item: StoreDebtRisk,
    currency: String,
    isPrivacyMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val riskColor = remember(item.riskLevel) {
        when (item.riskLevel) {
            RiskLevel.CRITICAL -> DebtRed
            RiskLevel.HIGH -> WarningAmber
            RiskLevel.MEDIUM -> Color(0xFF0288D1)
            RiskLevel.LOW -> PaymentGreen
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("risk_store_card_${item.storeId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(riskColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.storeName.take(1),
                            color = riskColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.storeName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (item.phone.isNotBlank()) {
                            Text(
                                text = item.phone,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.riskLevel.label,
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Debt & Percentage row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الرصيد المتبقي:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "••••" else FormatUtils.formatCurrency(item.remainingBalance, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DebtRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "حصة الدين من المحفظة:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.percentageOfTotalDebt}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alert indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "أقدم دين: منذ ${item.oldestDebtDays} يوماً",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.oldestDebtDays > 60) DebtRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.isOverLimit) {
                    Text(
                        text = "• تجاوز سقف الدين",
                        style = MaterialTheme.typography.labelSmall,
                        color = DebtRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (item.isOverdue) {
                    Text(
                        text = "• تأخر عن الموعد",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick WhatsApp and Call Actions
            if (item.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${item.phone}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "تعذر فتح الهاتف", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اتصال", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val cleanPhone = item.phone.filter { it.isDigit() }
                            val msg = "السلام عليكم ورحمة الله، نود تذكيركم بالرصيد المتبقي بمبلغ ${FormatUtils.formatCurrency(item.remainingBalance, currency)}، نأمل التكرم بالسداد في أقرب وقت. شكراً لكم."
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "تطبيق واتساب غير مثبت", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تذكير واتساب", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
