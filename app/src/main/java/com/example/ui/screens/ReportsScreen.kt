package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.TransactionType
import com.example.ui.components.FormatUtils
import com.example.ui.components.StatSummaryCard
import com.example.ui.components.charts.KpiDashboardSection
import com.example.ui.components.charts.MonthlyComparisonBarChart
import com.example.ui.components.charts.MonthlyTrendLineChart
import com.example.ui.components.charts.StoreDistributionPieChart
import com.example.ui.theme.DebtRed
import com.example.ui.theme.PaymentGreen
import com.example.ui.viewmodel.DebtViewModel
import com.example.ui.viewmodel.ReportPeriod
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStore: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stores by viewModel.allStores.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isPrivacyMode by viewModel.isPrivacyMode.collectAsStateWithLifecycle()

    val debtAgingSummary by viewModel.debtAgingSummary.collectAsStateWithLifecycle()
    val rankedStoreRisks by viewModel.rankedStoreRisks.collectAsStateWithLifecycle()
    val analytics by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()
    val monthsCount by viewModel.dashboardMonthsCount.collectAsStateWithLifecycle()
    val defaultCurrencyCode by viewModel.defaultCurrencyCode.collectAsStateWithLifecycle()
    val allCurrencies by viewModel.allCurrencies.collectAsStateWithLifecycle()
    val useLatestRateForAll by viewModel.useLatestRateForAll.collectAsStateWithLifecycle()
    val latestRateMap = remember(allCurrencies) { allCurrencies.associate { it.code to it.exchangeRate } }
    var selectedTab by remember { mutableStateOf(0) }

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    var selectedStoreId by remember { mutableStateOf<Long?>(null) } // null = All stores
    var isStoreDropdownExpanded by remember { mutableStateOf(false) }

    val now = remember { System.currentTimeMillis() }
    var customStart by remember { mutableStateOf(now - 30 * 24 * 60 * 60 * 1000L) }
    var customEnd by remember { mutableStateOf(now) }

    var isExportingPdf by remember { mutableStateOf(false) }
    var isExportingCsv by remember { mutableStateOf(false) }

    val filteredTransactions by viewModel.getFilteredTransactions(
        period = selectedPeriod,
        customStart = customStart,
        customEnd = customEnd,
        storeId = selectedStoreId
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    // Convert totals to default currency for reports summary
    val totalDebts = remember(filteredTransactions, defaultCurrencyCode, useLatestRateForAll, latestRateMap) {
        filteredTransactions.filter { it.type == TransactionType.DEBT }.sumOf { tx ->
            com.example.util.CurrencyUtils.convertToDefaultCurrency(
                amount = tx.amount,
                txCurrencyCode = tx.currencyCode,
                txExchangeRate = tx.exchangeRate,
                defaultCurrencyCode = defaultCurrencyCode,
                useLatestRateForAll = useLatestRateForAll,
                latestRateMap = latestRateMap
            )
        }
    }
    val totalPayments = remember(filteredTransactions, defaultCurrencyCode, useLatestRateForAll, latestRateMap) {
        filteredTransactions.filter { it.type == TransactionType.PAYMENT }.sumOf { tx ->
            com.example.util.CurrencyUtils.convertToDefaultCurrency(
                amount = tx.amount,
                txCurrencyCode = tx.currencyCode,
                txExchangeRate = tx.exchangeRate,
                defaultCurrencyCode = defaultCurrencyCode,
                useLatestRateForAll = useLatestRateForAll,
                latestRateMap = latestRateMap
            )
        }
    }
    val netChange = totalDebts - totalPayments
    val selectedStoreName = stores.find { it.id == selectedStoreId }?.name

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            isExportingPdf = true
            coroutineScope.launch {
                try {
                    val success = viewModel.exportPdfToUri(
                        context = context,
                        uri = uri,
                        period = selectedPeriod,
                        customStart = customStart,
                        customEnd = customEnd,
                        storeId = selectedStoreId,
                        storeName = selectedStoreName
                    )
                    if (success) {
                        Toast.makeText(context, "تم تصدير وحفظ ملف PDF بنجاح في المجلد المحدد", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل حفظ ملف PDF", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "حدث خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExportingPdf = false
                }
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            isExportingCsv = true
            coroutineScope.launch {
                try {
                    val success = viewModel.exportCsvToUri(
                        context = context,
                        uri = uri,
                        period = selectedPeriod,
                        customStart = customStart,
                        customEnd = customEnd,
                        storeId = selectedStoreId,
                        storeName = selectedStoreName
                    )
                    if (success) {
                        Toast.makeText(context, "تم تصدير وحفظ ملف CSV بنجاح في المجلد المحدد", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل حفظ ملف CSV", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "حدث خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExportingCsv = false
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تقارير وكشوفات الديون",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("كشف العمليات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_reports_statement")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الرسوم البيانية", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_reports_dashboard")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("أعمار الديون", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_debt_aging")
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
            // Filter Controls Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تصفية الفترة الزمنية",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // Period Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedPeriod == ReportPeriod.THIS_MONTH,
                                onClick = { selectedPeriod = ReportPeriod.THIS_MONTH },
                                label = { Text("الشهر الحالي") },
                                modifier = Modifier.testTag("period_this_month")
                            )
                            FilterChip(
                                selected = selectedPeriod == ReportPeriod.LAST_30_DAYS,
                                onClick = { selectedPeriod = ReportPeriod.LAST_30_DAYS },
                                label = { Text("آخر 30 يوم") },
                                modifier = Modifier.testTag("period_last_30_days")
                            )
                            FilterChip(
                                selected = selectedPeriod == ReportPeriod.ALL,
                                onClick = { selectedPeriod = ReportPeriod.ALL },
                                label = { Text("كل الأوقات") }
                            )
                            FilterChip(
                                selected = selectedPeriod == ReportPeriod.CUSTOM,
                                onClick = { selectedPeriod = ReportPeriod.CUSTOM },
                                label = { Text("مخصص") }
                            )
                        }

                        // Custom Date Range Pickers if CUSTOM is selected
                        if (selectedPeriod == ReportPeriod.CUSTOM) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = customStart }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val sel = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                                customStart = sel.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("من: ${FormatUtils.formatDate(customStart)}")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = customEnd }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val sel = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                                                customEnd = sel.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إلى: ${FormatUtils.formatDate(customEnd)}")
                                }
                            }
                        }

                        // Store Filter Dropdown
                        Text(
                            text = "المحل أو العميل",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        ExposedDropdownMenuBox(
                            expanded = isStoreDropdownExpanded,
                            onExpandedChange = { isStoreDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedStoreName ?: "جميع المحلات والعملاء",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStoreDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("report_store_filter")
                            )
                            ExposedDropdownMenu(
                                expanded = isStoreDropdownExpanded,
                                onDismissRequest = { isStoreDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("جميع المحلات والعملاء", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedStoreId = null
                                        isStoreDropdownExpanded = false
                                    }
                                )
                                stores.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            selectedStoreId = s.id
                                            isStoreDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Export Actions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "تصدير تقرير",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PDF Export Button
                            Button(
                                onClick = {
                                    if (filteredTransactions.isEmpty()) {
                                        Toast.makeText(context, "لا توجد معاملات لتصديرها", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val timeStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                                    val prefix = if (selectedStoreName != null) "تقرير_${selectedStoreName?.replace(" ", "_")}" else "تقرير_الديون"
                                    createPdfLauncher.launch("${prefix}_$timeStr.pdf")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_pdf_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isExportingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تصدير PDF", fontWeight = FontWeight.Bold)
                                }
                            }

                            // CSV Export Button (Excel with BOM)
                            Button(
                                onClick = {
                                    if (filteredTransactions.isEmpty()) {
                                        Toast.makeText(context, "لا توجد معاملات لتصديرها", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val timeStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                                    val prefix = if (selectedStoreName != null) "ديون_${selectedStoreName?.replace(" ", "_")}" else "تقرير_الديون"
                                    createCsvLauncher.launch("${prefix}_$timeStr.csv")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_csv_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isExportingCsv) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تصدير CSV", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Summary Metrics for Period
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryCard(
                        title = "ديون الفترة",
                        amount = totalDebts,
                        currency = currency,
                        subtitle = "${filteredTransactions.count { it.type == TransactionType.DEBT }} عمليات دين",
                        color = DebtRed,
                        containerColor = MaterialTheme.colorScheme.surface,
                        isPrivacyMode = isPrivacyMode,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = "مسددات الفترة",
                        amount = totalPayments,
                        currency = currency,
                        subtitle = "${filteredTransactions.count { it.type == TransactionType.PAYMENT }} عمليات سداد",
                        color = PaymentGreen,
                        containerColor = MaterialTheme.colorScheme.surface,
                        isPrivacyMode = isPrivacyMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Net Balance of Period
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "صافي حركة الفترة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (netChange > 0) "زيادة في رصيد الديون" else "سداد وتخفيض ديون",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = FormatUtils.formatCurrency(netChange, currency, isPrivacyMode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netChange > 0) DebtRed else PaymentGreen
                        )
                    }
                }
            }

            // Preview List Title
            item {
                Text(
                    text = "معاملات الفترة المحددة (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Empty preview state
            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "لا توجد معاملات مسجلة في هذه الفترة",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val isDebt = tx.type == TransactionType.DEBT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tx.storeName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDebt) "دين" else "سداد",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDebt) DebtRed else PaymentGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatDateTime(tx.timestamp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tx.note.isNotBlank()) {
                                    Text(
                                        text = tx.note,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            val txCurrencySymbol = tx.currencyCode.ifBlank { currency }
                            val isNonDefault = !tx.currencyCode.isBlank() && !tx.currencyCode.equals(defaultCurrencyCode, ignoreCase = true)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isPrivacyMode) {
                                        FormatUtils.formatCurrency(tx.amount, txCurrencySymbol, true)
                                    } else {
                                        "${if (isDebt) "+" else "-"}${FormatUtils.formatCurrency(tx.amount, txCurrencySymbol)}"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDebt) DebtRed else PaymentGreen
                                )
                                if (isNonDefault && !isPrivacyMode) {
                                    val effectiveRate = if (useLatestRateForAll) {
                                        latestRateMap[tx.currencyCode] ?: tx.exchangeRate
                                    } else {
                                        tx.exchangeRate
                                    }
                                    Text(
                                        text = "صرف: $effectiveRate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Period Selector
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "فترة العرض:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = monthsCount == 6,
                                            onClick = { viewModel.setDashboardMonthsCount(6) },
                                            label = { Text("آخر 6 أشهر", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )

                                        FilterChip(
                                            selected = monthsCount == 12,
                                            onClick = { viewModel.setDashboardMonthsCount(12) },
                                            label = { Text("آخر 12 شهر", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // KPI Cards Summary Section (مؤشرات الأداء المالي)
                        item {
                            KpiDashboardSection(
                                totalDebt = analytics.totalDebt,
                                totalPaid = analytics.totalPayment,
                                netBalance = analytics.netBalance,
                                overdueCount = analytics.overdueCount,
                                currency = currency,
                                onOverdueClick = { selectedTab = 2 },
                                isPrivacyMode = isPrivacyMode,
                                activeDebtStoresCount = analytics.activeDebtStoresCount
                            )
                        }

                        // Line Chart: Debt and payment evolution
                        item {
                            MonthlyTrendLineChart(
                                monthlyData = analytics.monthlyData,
                                currency = currency,
                                isPrivacyMode = isPrivacyMode
                            )
                        }

                        // Comparison Bar Chart: New debt vs payment
                        item {
                            MonthlyComparisonBarChart(
                                monthlyData = analytics.monthlyData,
                                currency = currency,
                                isPrivacyMode = isPrivacyMode
                            )
                        }

                        // Donut / Pie Chart: Debt distribution by store/client
                        item {
                            StoreDistributionPieChart(
                                storeShares = analytics.storeShares,
                                totalDebt = analytics.netBalance,
                                currency = currency,
                                onNavigateToStore = onNavigateToStore,
                                isPrivacyMode = isPrivacyMode
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                2 -> {
                    DebtAgingTabContent(
                        agingSummary = debtAgingSummary,
                        storeRisks = rankedStoreRisks,
                        currency = currency,
                        isPrivacyMode = isPrivacyMode,
                        onStoreClick = onNavigateToStore,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
