package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlyFinancialData
import com.example.ui.components.FormatUtils
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaymentGreen
import kotlin.math.max

@Composable
fun MonthlyComparisonBarChart(
    monthlyData: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier,
    isPrivacyMode: Boolean = false
) {
    if (monthlyData.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد بيانات مقارنة للأشهر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(monthlyData.lastIndex) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(monthlyData) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
        selectedIndex = monthlyData.lastIndex
    }

    val textMeasurer = rememberTextMeasurer()
    val maxAmount = remember(monthlyData) {
        val highest = monthlyData.maxOfOrNull { max(it.totalDebt, it.totalPayment) } ?: 0.0
        if (highest <= 0.0) 100.0 else highest * 1.15
    }

    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val textStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "أعمدة مقارنة بين الأشهر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "دين جديد مقابل سداد لكل شهر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DebtRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "دين جديد",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DebtRed
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PaymentGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "سداد",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaymentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(monthlyData) {
                            detectTapGestures { offset ->
                                val count = monthlyData.size
                                if (count > 0) {
                                    val paddingLeft = 44.dp.toPx()
                                    val paddingRight = 16.dp.toPx()
                                    val usableWidth = size.width - paddingLeft - paddingRight
                                    val groupWidth = usableWidth / count

                                    if (offset.x >= paddingLeft && offset.x <= (size.width - paddingRight)) {
                                        val idx = ((offset.x - paddingLeft) / groupWidth).toInt()
                                            .coerceIn(0, count - 1)
                                        selectedIndex = idx
                                    }
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingBottom = 26.dp.toPx()
                    val paddingTop = 14.dp.toPx()
                    val paddingLeft = 46.dp.toPx()
                    val paddingRight = 16.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom
                    val count = monthlyData.size
                    val groupWidth = chartWidth / count
                    val progress = animProgress.value

                    // Draw 4 horizontal dashed reference lines
                    val gridLinesCount = 3
                    for (i in 0..gridLinesCount) {
                        val fraction = i.toFloat() / gridLinesCount
                        val y = paddingTop + (chartHeight * (1f - fraction))
                        val value = maxAmount * fraction

                        drawLine(
                            color = axisColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Format Y-axis value label
                        val label = formatCompactNumber(value)
                        val textLayout = textMeasurer.measure(label, textStyle)
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(paddingLeft - textLayout.size.width - 6.dp.toPx(), y - (textLayout.size.height / 2f))
                        )
                    }

                    // Bar properties
                    val barWidth = (groupWidth * 0.32f).coerceAtMost(16.dp.toPx()).coerceAtLeast(6.dp.toPx())
                    val barSpacing = 3.dp.toPx()

                    for (i in 0 until count) {
                        val item = monthlyData[i]
                        val groupCenterX = paddingLeft + (i * groupWidth) + (groupWidth / 2f)
                        val isSelected = i == selectedIndex

                        // Selected month group background highlight pill
                        if (isSelected) {
                            val highlightLeft = paddingLeft + (i * groupWidth) + 2.dp.toPx()
                            val highlightRight = highlightLeft + groupWidth - 4.dp.toPx()
                            val highlightPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = highlightLeft,
                                        top = paddingTop - 4.dp.toPx(),
                                        right = highlightRight,
                                        bottom = paddingTop + chartHeight + 2.dp.toPx(),
                                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                    )
                                )
                            }
                            drawPath(
                                path = highlightPath,
                                color = primaryColor.copy(alpha = 0.08f)
                            )
                        }

                        // Debt Bar (Red)
                        val debtRatio = ((item.totalDebt / maxAmount) * progress).toFloat().coerceIn(0f, 1f)
                        val debtBarHeight = (chartHeight * debtRatio).coerceAtLeast(if (item.totalDebt > 0) 4.dp.toPx() else 0f)
                        val debtBarLeft = groupCenterX - barWidth - (barSpacing / 2f)
                        val debtBarTop = paddingTop + chartHeight - debtBarHeight

                        if (debtBarHeight > 0) {
                            val debtPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = debtBarLeft,
                                        top = debtBarTop,
                                        right = debtBarLeft + barWidth,
                                        bottom = paddingTop + chartHeight,
                                        topRightCornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                        topLeftCornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                )
                            }
                            drawPath(path = debtPath, color = DebtRed)
                        }

                        // Payment Bar (Green)
                        val payRatio = ((item.totalPayment / maxAmount) * progress).toFloat().coerceIn(0f, 1f)
                        val payBarHeight = (chartHeight * payRatio).coerceAtLeast(if (item.totalPayment > 0) 4.dp.toPx() else 0f)
                        val payBarLeft = groupCenterX + (barSpacing / 2f)
                        val payBarTop = paddingTop + chartHeight - payBarHeight

                        if (payBarHeight > 0) {
                            val payPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = payBarLeft,
                                        top = payBarTop,
                                        right = payBarLeft + barWidth,
                                        bottom = paddingTop + chartHeight,
                                        topRightCornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                        topLeftCornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                )
                            }
                            drawPath(path = payPath, color = PaymentGreen)
                        }

                        // X-axis label (Month Name)
                        val mLayout = textMeasurer.measure(
                            item.monthLabel,
                            if (isSelected) textStyle.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            ) else textStyle
                        )
                        drawText(
                            textLayoutResult = mLayout,
                            topLeft = Offset(groupCenterX - (mLayout.size.width / 2f), paddingTop + chartHeight + 6.dp.toPx())
                        )
                    }
                }
            }

            // Selected Month Comparison Details Card
            selectedIndex?.let { sIdx ->
                if (sIdx in monthlyData.indices) {
                    val selItem = monthlyData[sIdx]
                    val diff = selItem.totalDebt - selItem.totalPayment
                    val isNetDebtIncrease = diff > 0

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selItem.fullLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isNetDebtIncrease) DebtRed.copy(alpha = 0.15f) else PaymentGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (diff == 0.0) "متوازن" else if (isNetDebtIncrease) "زيادة ديون: +${FormatUtils.formatCurrency(diff, currency, isPrivacyMode)}" else "سداد فائض: -${FormatUtils.formatCurrency(-diff, currency, isPrivacyMode)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNetDebtIncrease) DebtRed else PaymentGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(DebtRed)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "دين جديد: ${FormatUtils.formatCurrency(selItem.totalDebt, currency, isPrivacyMode)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(PaymentGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "سداد: ${FormatUtils.formatCurrency(selItem.totalPayment, currency, isPrivacyMode)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompactNumber(value: Double): String {
    return when {
        value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format(java.util.Locale.US, "%.0fK", value / 1_000)
        else -> String.format(java.util.Locale.US, "%.0f", value)
    }
}
