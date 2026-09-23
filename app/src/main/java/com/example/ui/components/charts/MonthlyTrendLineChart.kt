package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
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
fun MonthlyTrendLineChart(
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
                    text = "لا توجد بيانات شهرية كافية للرسم البياني",
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
            // Header with Chart Title and Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تطور الديون والسدادات شهرياً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "رسم بياني خطي مقارن لحركة الأموال",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DebtRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ديون جديدة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DebtRed
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PaymentGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "سدادات",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaymentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart Area
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
                                    val paddingLeft = 40.dp.toPx()
                                    val paddingRight = 16.dp.toPx()
                                    val usableWidth = size.width - paddingLeft - paddingRight
                                    val step = if (count > 1) usableWidth / (count - 1) else usableWidth

                                    var closestIdx = 0
                                    var minDiff = Float.MAX_VALUE
                                    for (i in 0 until count) {
                                        val x = paddingLeft + (i * step)
                                        val diff = kotlin.math.abs(offset.x - x)
                                        if (diff < minDiff) {
                                            minDiff = diff
                                            closestIdx = i
                                        }
                                    }
                                    selectedIndex = closestIdx
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
                    val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth

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

                    // Build Debt Path and Payment Path
                    val debtPath = Path()
                    val paymentPath = Path()
                    val debtFillPath = Path()
                    val paymentFillPath = Path()

                    val debtPoints = mutableListOf<Offset>()
                    val paymentPoints = mutableListOf<Offset>()

                    val progress = animProgress.value

                    for (i in 0 until count) {
                        val item = monthlyData[i]
                        val x = paddingLeft + (i * stepX)

                        val debtRatio = ((item.totalDebt / maxAmount) * progress).toFloat().coerceIn(0f, 1f)
                        val payRatio = ((item.totalPayment / maxAmount) * progress).toFloat().coerceIn(0f, 1f)

                        val debtY = paddingTop + (chartHeight * (1f - debtRatio))
                        val payY = paddingTop + (chartHeight * (1f - payRatio))

                        val dPoint = Offset(x, debtY)
                        val pPoint = Offset(x, payY)

                        debtPoints.add(dPoint)
                        paymentPoints.add(pPoint)

                        if (i == 0) {
                            debtPath.moveTo(dPoint.x, dPoint.y)
                            paymentPath.moveTo(pPoint.x, pPoint.y)

                            debtFillPath.moveTo(dPoint.x, paddingTop + chartHeight)
                            debtFillPath.lineTo(dPoint.x, dPoint.y)

                            paymentFillPath.moveTo(pPoint.x, paddingTop + chartHeight)
                            paymentFillPath.lineTo(pPoint.x, pPoint.y)
                        } else {
                            // Smooth bezier curve
                            val prevD = debtPoints[i - 1]
                            val cxD1 = (prevD.x + dPoint.x) / 2f
                            debtPath.cubicTo(cxD1, prevD.y, cxD1, dPoint.y, dPoint.x, dPoint.y)
                            debtFillPath.cubicTo(cxD1, prevD.y, cxD1, dPoint.y, dPoint.x, dPoint.y)

                            val prevP = paymentPoints[i - 1]
                            val cxP1 = (prevP.x + pPoint.x) / 2f
                            paymentPath.cubicTo(cxP1, prevP.y, cxP1, pPoint.y, pPoint.x, pPoint.y)
                            paymentFillPath.cubicTo(cxP1, prevP.y, cxP1, pPoint.y, pPoint.x, pPoint.y)
                        }

                        // Draw X-axis label (month name)
                        val monthLabel = item.monthLabel
                        val mLayout = textMeasurer.measure(monthLabel, textStyle)
                        drawText(
                            textLayoutResult = mLayout,
                            topLeft = Offset(x - (mLayout.size.width / 2f), paddingTop + chartHeight + 6.dp.toPx())
                        )
                    }

                    // Complete fill paths down to baseline
                    val lastX = paddingLeft + ((count - 1) * stepX)
                    val baselineY = paddingTop + chartHeight

                    debtFillPath.lineTo(lastX, baselineY)
                    debtFillPath.close()

                    paymentFillPath.lineTo(lastX, baselineY)
                    paymentFillPath.close()

                    // Draw Gradient Fills
                    drawPath(
                        path = debtFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(DebtRed.copy(alpha = 0.22f), Color.Transparent),
                            startY = paddingTop,
                            endY = baselineY
                        )
                    )

                    drawPath(
                        path = paymentFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(PaymentGreen.copy(alpha = 0.18f), Color.Transparent),
                            startY = paddingTop,
                            endY = baselineY
                        )
                    )

                    // Draw Stroke Lines
                    drawPath(
                        path = debtPath,
                        color = DebtRed,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = paymentPath,
                        color = PaymentGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Highlight selected index
                    selectedIndex?.let { sIdx ->
                        if (sIdx in 0 until count) {
                            val selX = paddingLeft + (sIdx * stepX)

                            // Vertical highlight indicator line
                            drawLine(
                                color = primaryColor.copy(alpha = 0.6f),
                                start = Offset(selX, paddingTop),
                                end = Offset(selX, baselineY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )

                            // Draw glowing points for selected month
                            val dPt = debtPoints[sIdx]
                            val pPt = paymentPoints[sIdx]

                            // Debt Point
                            drawCircle(color = DebtRed.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = dPt)
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = dPt)
                            drawCircle(color = DebtRed, radius = 3.5.dp.toPx(), center = dPt)

                            // Payment Point
                            drawCircle(color = PaymentGreen.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = pPt)
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pPt)
                            drawCircle(color = PaymentGreen, radius = 3.5.dp.toPx(), center = pPt)
                        }
                    }
                }
            }

            // Selected Month Floating Info Card
            selectedIndex?.let { sIdx ->
                if (sIdx in monthlyData.indices) {
                    val selItem = monthlyData[sIdx]
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selItem.fullLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${selItem.transactionCount} عملية مسجلة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ديون:",
                                        fontSize = 10.sp,
                                        color = DebtRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(selItem.totalDebt, currency, isPrivacyMode),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DebtRed
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "سدادات:",
                                        fontSize = 10.sp,
                                        color = PaymentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(selItem.totalPayment, currency, isPrivacyMode),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaymentGreen
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
