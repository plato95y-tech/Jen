package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.dao.BalanceSummary
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DebtCeilingIndicator
import com.example.ui.components.FormatUtils
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.ReminderCustomizationDialog
import com.example.ui.components.StatSummaryCard
import com.example.ui.components.StoreEditDialog
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedContainer
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.ui.viewmodel.DebtViewModel
import com.example.ui.viewmodel.ReportPeriod
import com.example.util.DebtNotificationHelper
import com.example.util.FileShareUtils
import com.example.util.ReminderMessageUtils
import com.example.util.TafqeetUtils
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: Long,
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (storeId: Long, type: String, txId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val store by viewModel.getStore(storeId).collectAsStateWithLifecycle(initialValue = null)
    val transactions by viewModel.getStoreTransactions(storeId).collectAsStateWithLifecycle(initialValue = emptyList())
    val summary by viewModel.getStoreSummary(storeId).collectAsStateWithLifecycle(initialValue = BalanceSummary(0.0, 0.0, 0.0, 0))
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val defaultCurrencyCode by viewModel.defaultCurrencyCode.collectAsStateWithLifecycle()
    val allCurrencies by viewModel.allCurrencies.collectAsStateWithLifecycle()
    val useLatestRateForAll by viewModel.useLatestRateForAll.collectAsStateWithLifecycle()
    val latestRateMap = remember(allCurrencies) { allCurrencies.associate { it.code to it.exchangeRate } }
    val isPrivacyMode by viewModel.isPrivacyMode.collectAsStateWithLifecycle()

    var showEditStoreDialog by remember { mutableStateOf(false) }
    var showDeleteStoreConfirm by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var viewingReceiptTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "تم تفعيل صلاحية إشعارات التنبيه بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = store?.name ?: "تفاصيل الحساب",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
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
                actions = {
                    IconButton(
                        onClick = { viewModel.togglePrivacyMode() },
                        modifier = Modifier.testTag("toggle_privacy_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPrivacyMode) "إلغاء إخفاء الأرصدة" else "إخفاء الأرصدة (وضع الخصوصية)",
                            tint = if (isPrivacyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات إضافية")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل بيانات المحل") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showEditStoreDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تصدير كشف حساب PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    val file = viewModel.exportPdf(
                                        context = context,
                                        period = ReportPeriod.ALL,
                                        customStart = 0L,
                                        customEnd = 0L,
                                        storeId = storeId,
                                        storeName = store?.name
                                    )
                                    FileShareUtils.shareFile(context, file, "application/pdf", "كشف حساب ${store?.name}")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تصدير كشف حساب CSV (إكسل)") },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    val file = viewModel.exportCsv(
                                        context = context,
                                        period = ReportPeriod.ALL,
                                        customStart = 0L,
                                        customEnd = 0L,
                                        storeId = storeId,
                                        storeName = store?.name
                                    )
                                    FileShareUtils.shareFile(context, file, "text/csv", "كشف حساب ${store?.name}")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف المحل وسجلاته", color = DebtRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DebtRed) },
                            onClick = {
                                showMenu = false
                                showDeleteStoreConfirm = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header stats block
            item {
                store?.let { currentStore ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            if (currentStore.phone.isNotBlank() || currentStore.notes.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentStore.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = currentStore.phone,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (currentStore.dueDate != null) {
                                        val isOverdue = currentStore.dueDate <= System.currentTimeMillis() && summary.remainingBalance > 0
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isOverdue) WarningAmberContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = if (isOverdue) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "موعد السداد: ${FormatUtils.formatDate(currentStore.dueDate)}",
                                                    fontSize = 11.sp,
                                                    color = if (isOverdue) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                if (currentStore.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentStore.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Prominent Remaining Balance (M3 Container with Live Tafqeet)
                            val isCredit = summary.remainingBalance < 0
                            val hasDebt = summary.remainingBalance > 0
                            val balanceTitle = when {
                                hasDebt -> "إجمالي المبلغ المتبقي عليه"
                                isCredit -> "إجمالي المبلغ المتبقي له"
                                else -> "تم السداد بالكامل"
                            }
                            val balanceColor = when {
                                hasDebt -> DebtRed
                                isCredit -> Color(0xFF0284C7)
                                else -> PaymentGreen
                            }
                            val balanceContainer = when {
                                hasDebt -> DebtRedContainer
                                isCredit -> Color(0xFFE0F2FE)
                                else -> PaymentGreenContainer
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = balanceContainer,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    balanceColor.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = balanceTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = balanceColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(summary.remainingBalance, currency, isPrivacyMode),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = balanceColor
                                    )

                                    // Arabic Tafqeet (المبلغ كتابة بالكلمات)
                                    if (summary.remainingBalance != 0.0 && !isPrivacyMode) {
                                        val spelledRemaining = TafqeetUtils.spellAmount(kotlin.math.abs(summary.remainingBalance), currency)
                                        if (spelledRemaining.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White.copy(alpha = 0.6f)
                                            ) {
                                                Text(
                                                    text = spelledRemaining,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = balanceColor,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Total Debt & Total Paid Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatSummaryCard(
                                    title = "إجمالي الديون",
                                    amount = summary.totalDebt,
                                    currency = currency,
                                    subtitle = "جميع المشتريات بالدين",
                                    color = DebtRed,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    isPrivacyMode = isPrivacyMode,
                                    modifier = Modifier.weight(1f)
                                )
                                StatSummaryCard(
                                    title = "إجمالي المسدد",
                                    amount = summary.totalPaid,
                                    currency = currency,
                                    subtitle = "جميع الدفعات المسددة",
                                    color = PaymentGreen,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    isPrivacyMode = isPrivacyMode,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Ceiling progress
                            if (currentStore.debtLimit > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                DebtCeilingIndicator(
                                    currentDebt = summary.remainingBalance,
                                    limit = currentStore.debtLimit,
                                    currency = currency
                                )
                            }
                        }
                    }
                }
            }

            // Communication & Reminder Actions (Proposal #1 & #2)
            item {
                store?.let { currentStore ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المراسلة والتنبيهات ومشاركة الكشف",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (currentStore.dueDate != null) {
                                    val isOverdue = currentStore.dueDate <= System.currentTimeMillis() && summary.remainingBalance > 0
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isOverdue) DebtRedContainer else PaymentGreenContainer
                                    ) {
                                        Text(
                                            text = if (isOverdue) "مستحق السداد" else "تنبيه مجدول",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverdue) DebtRed else PaymentGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Quick Action Buttons: WhatsApp Reminder, Direct Call, Alert Trigger
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showReminderDialog = true },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("send_reminder_dialog_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF25D366) // WhatsApp green
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تذكير سداد",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }

                                if (currentStore.phone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { ReminderMessageUtils.dialPhone(context, currentStore.phone) },
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .testTag("quick_call_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("اتصال", fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !DebtNotificationHelper.hasNotificationPermission(context)) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            DebtNotificationHelper.showNotification(
                                                context = context,
                                                notificationId = storeId.toInt().coerceAtLeast(1001),
                                                title = "⏰ تنبيه سداد: ${currentStore.name}",
                                                message = "رصيد مستحق بقيمة ${FormatUtils.formatCurrency(summary.remainingBalance, currency)}"
                                            )
                                            Toast.makeText(context, "تم إرسال إشعار التذكير بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .testTag("trigger_notification_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تنبيه", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Quick action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigateToAddTransaction(storeId, "DEBT", 0L) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("store_add_debt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DebtRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.NorthEast, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل دين جديد", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onNavigateToAddTransaction(storeId, "PAYMENT", 0L) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("store_add_payment_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SouthWest, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل دفعة سداد", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Ledger title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "كشف الحساب وسجل المعاملات (${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Empty state for transactions
            if (transactions.isEmpty()) {
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
                                text = "لا توجد معاملات مسجلة حتى الآن",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "استخدم الأزرار بالأعلى لتسجيل دين جديد أو دفعة سداد لهذا المحل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Transactions list (chronological) - displayed in original currency
                items(transactions, key = { it.id }) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        defaultCurrencySymbol = currency,
                        defaultCurrencyCode = defaultCurrencyCode,
                        useLatestRateForAll = useLatestRateForAll,
                        latestRateMap = latestRateMap,
                        isPrivacyMode = isPrivacyMode,
                        onEdit = { onNavigateToAddTransaction(storeId, tx.type.name, tx.id) },
                        onDelete = { transactionToDelete = tx },
                        onViewReceipt = { viewingReceiptTx = it }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Receipt viewer dialog (Proposal #2)
    viewingReceiptTx?.let { tx ->
        ReceiptViewerDialog(
            transaction = tx,
            currency = currency,
            onDismiss = { viewingReceiptTx = null }
        )
    }

    // Reminder customization & sharing dialog (Proposal #1 & #2)
    if (showReminderDialog && store != null) {
        val lastTx = transactions.firstOrNull()
        ReminderCustomizationDialog(
            storeName = store!!.name,
            phone = store!!.phone,
            remainingBalance = summary.remainingBalance,
            currency = currency,
            totalDebt = summary.totalDebt,
            totalPaid = summary.totalPaid,
            dueDate = store!!.dueDate,
            lastTransactionDate = lastTx?.timestamp,
            onDismiss = { showReminderDialog = false }
        )
    }

    // Edit store dialog
    if (showEditStoreDialog && store != null) {
        StoreEditDialog(
            initialStore = store,
            onDismiss = { showEditStoreDialog = false },
            onSave = { name, phone, notes, limit, dueDate ->
                viewModel.saveStore(
                    id = storeId,
                    name = name,
                    phone = phone,
                    notes = notes,
                    debtLimit = limit,
                    dueDate = dueDate
                ) {
                    showEditStoreDialog = false
                }
            }
        )
    }

    // Confirm store delete dialog
    if (showDeleteStoreConfirm) {
        ConfirmActionDialog(
            title = "حذف المحل",
            message = "هل أنت متأكد من حذف المحل '${store?.name}' وجميع سجلات ديونه ومدفوعاته؟ لا يمكن التراجع عن هذا الإجراء.",
            confirmText = "حذف نهائي",
            isDestructive = true,
            onDismiss = { showDeleteStoreConfirm = false },
            onConfirm = {
                showDeleteStoreConfirm = false
                viewModel.deleteStore(storeId) {
                    onNavigateBack()
                }
            }
        )
    }

    // Confirm transaction delete dialog
    transactionToDelete?.let { tx ->
        val isDebt = tx.type == TransactionType.DEBT
        ConfirmActionDialog(
            title = if (isDebt) "حذف قيد الدين" else "حذف قيد السداد",
            message = "هل أنت متأكد من حذف معاملة ${if (isDebt) "الدين" else "السداد"} بمبلغ ${FormatUtils.formatCurrency(tx.amount, currency)}؟",
            confirmText = "حذف",
            isDestructive = true,
            onDismiss = { transactionToDelete = null },
            onConfirm = {
                viewModel.deleteTransaction(tx.id)
                transactionToDelete = null
            }
        )
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    defaultCurrencySymbol: String,
    defaultCurrencyCode: String = "",
    useLatestRateForAll: Boolean = false,
    latestRateMap: Map<String, Double> = emptyMap(),
    isPrivacyMode: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: (TransactionEntity) -> Unit = {}
) {
    val isDebt = transaction.type == TransactionType.DEBT
    var showMenu by remember { mutableStateOf(false) }
    val txCurrency = transaction.currencyCode.ifBlank { defaultCurrencyCode.ifBlank { defaultCurrencySymbol } }
    val isDifferentCurrency = !transaction.currencyCode.isBlank() && !transaction.currencyCode.equals(defaultCurrencyCode, ignoreCase = true)

    val effectiveRate = if (useLatestRateForAll) {
        latestRateMap[transaction.currencyCode] ?: transaction.exchangeRate
    } else {
        transaction.exchangeRate
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isDebt) DebtRedContainer else PaymentGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDebt) Icons.Default.NorthEast else Icons.Default.SouthWest,
                            contentDescription = null,
                            tint = if (isDebt) DebtRed else PaymentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDebt) DebtRedContainer else PaymentGreenContainer
                            ) {
                                Text(
                                    text = if (isDebt) "دين (شراء)" else "دفعة سداد",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDebt) DebtRed else PaymentGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = FormatUtils.formatDateTime(transaction.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (transaction.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transaction.note,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Amount, receipt icon & menu (Original Currency)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (transaction.imageUri != null) {
                        IconButton(
                            onClick = { onViewReceipt(transaction) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("receipt_button_${transaction.id}")
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "عرض الفاتورة المرفقة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        // Original currency amount display
                        Text(
                            text = if (isPrivacyMode) {
                                FormatUtils.formatCurrency(transaction.amount, txCurrency, true)
                            } else {
                                "${if (isDebt) "+" else "-"}${FormatUtils.formatCurrency(transaction.amount, txCurrency)}"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDebt) DebtRed else PaymentGreen
                        )

                        // If different currency, display rate and converted value in default currency
                        if (isDifferentCurrency && !isPrivacyMode) {
                            val converted = transaction.amount * effectiveRate
                            Text(
                                text = "صرف: $effectiveRate (يعادل: ${FormatUtils.formatCurrency(converted, defaultCurrencySymbol)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("tx_menu_${transaction.id}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (transaction.imageUri != null) {
                                DropdownMenuItem(
                                    text = { Text("عرض ومشاركة الفاتورة") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onViewReceipt(transaction)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("تعديل") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف", color = DebtRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DebtRed) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Tafqeet subtitle (كتابة المبلغ حرفياً)
            if (!isPrivacyMode) {
                val spelled = TafqeetUtils.spellAmount(transaction.amount, txCurrency)
                if (spelled.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = spelled,
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isDebt) DebtRed else PaymentGreen).copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(start = 54.dp)
                    )
                }
            }
        }
    }
}
