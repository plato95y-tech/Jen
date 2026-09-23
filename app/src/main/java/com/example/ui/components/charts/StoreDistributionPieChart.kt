package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreDebtShare
import com.example.ui.components.FormatUtils
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreDistributionPieChart(
    storeShares: List<StoreDebtShare>,
    totalDebt: Double,
    currency: String,
    onNavigateToStore: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isPrivacyMode: Boolean = false
) {
    var selectedStoreIndex by remember { mutableStateOf<Int?>(if (storeShares.isNotEmpty()) 0 else null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(storeShares) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 850))
        if (storeShares.isNotEmpty() && selectedStoreIndex == null) {
            selectedStoreIndex = 0
        }
    }

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
                        text = "توزيع الديون حسب المحل والعميل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "رسم دائري يوضح تركز المديونيات وأعلى العملاء",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (storeShares.isEmpty() || totalDebt <= 0.0) {
                // Empty / Settled state
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد ديون متبقية حالياً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "جميع حسابات العملاء مسددة وخالصة بالكامل",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Donut Chart with Center Text
                val selectedShare = selectedStoreIndex?.let { storeShares.getOrNull(it) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
                            .pointerInput(storeShares) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                    // Check if tapped within donut ring
                                    val minDim = minOf(size.width, size.height).toFloat()
                                    val innerR = (minDim / 2f) * 0.55f
                                    val outerR = (minDim / 2f) * 0.95f

                                    if (distance in (innerR * 0.8f)..(outerR * 1.2f)) {
                                        var angle = (atan2(dy, dx) * 180f / PI).toFloat()
                                        if (angle < 0) angle += 360f

                                        // Start angle is -90 (top)
                                        var adjustedAngle = (angle + 90f) % 360f

                                        var currentSweepStart = 0f
                                        for (i in storeShares.indices) {
                                            val sweep = (storeShares[i].percentage / 100f) * 360f
                                            if (adjustedAngle >= currentSweepStart && adjustedAngle < (currentSweepStart + sweep)) {
                                                selectedStoreIndex = i
                                                break
                                            }
                                            currentSweepStart += sweep
                                        }
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 32.dp.toPx()
                        val arcSize = size.minDimension - strokeWidth - 10.dp.toPx()
                        val topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f)
                        val fullSize = Size(arcSize, arcSize)

                        var startAngle = -90f
                        val progress = animProgress.value

                        storeShares.forEachIndexed { index, share ->
                            val fullSweep = (share.percentage / 100f) * 360f
                            val animatedSweep = fullSweep * progress
                            val isSelected = index == selectedStoreIndex

                            val currentStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

                            drawArc(
                                color = share.color,
                                startAngle = startAngle,
                                sweepAngle = (animatedSweep - 1.5f).coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = if (isSelected) topLeft - Offset(3.dp.toPx(), 3.dp.toPx()) else topLeft,
                                size = if (isSelected) Size(fullSize.width + 6.dp.toPx(), fullSize.height + 6.dp.toPx()) else fullSize,
                                style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                            )

                            startAngle += animatedSweep
                        }
                    }

                    // Center Content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(115.dp)
                            .padding(4.dp)
                    ) {
                        if (selectedShare != null) {
                            Text(
                                text = selectedShare.storeName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", selectedShare.percentage)}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = selectedShare.color
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatUtils.formatCurrency(selectedShare.debtAmount, currency, isPrivacyMode),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "إجمالي الديون",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatUtils.formatCurrency(totalDebt, currency, isPrivacyMode),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Legend List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    storeShares.forEachIndexed { index, share ->
                        val isSelected = index == selectedStoreIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedStoreIndex = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) share.color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.5.dp, share.color) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(share.color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = share.storeName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = share.color.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", share.percentage)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = share.color,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(share.debtAmount, currency, isPrivacyMode),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (share.storeId > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "عرض التفاصيل",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { onNavigateToStore(share.storeId) }
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
}
