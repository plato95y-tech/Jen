package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CurrencyEntity
import com.example.data.entity.ExchangeRateHistoryEntity
import com.example.data.export.BackupFileInfo
import com.example.data.export.ParsedBackupData
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FormatUtils
import com.example.ui.components.PinSetupDialog
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedContainer
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DebtViewModel
import com.example.util.BiometricHelper
import com.example.util.DailyReminderScheduler
import com.example.util.DebtNotificationHelper
import com.example.util.TafqeetUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class SettingsTab(val title: String, val icon: ImageVector) {
    CURRENCY("العملات والصرف", Icons.Default.CurrencyExchange),
    BACKUP("النسخ والاستعادة", Icons.Default.Backup),
    SECURITY("الأمان والخصوصية", Icons.Default.Security),
    PREFERENCES("التفضيلات والنظام", Icons.Default.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    initialTab: SettingsTab = SettingsTab.CURRENCY,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(initialTab) }

    // Observers from ViewModel
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val defaultCurrencyCode by viewModel.defaultCurrencyCode.collectAsStateWithLifecycle()
    val allCurrencies by viewModel.allCurrencies.collectAsStateWithLifecycle()
    val allRateHistory by viewModel.allRateHistory.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val useLatestRateForAll by viewModel.useLatestRateForAll.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsStateWithLifecycle()
    val autoBackupIntervalDays by viewModel.autoBackupIntervalDays.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val savedBackups by viewModel.savedBackups.collectAsStateWithLifecycle()
    val isSmartSummaryEnabled by viewModel.isSmartSummaryNotificationEnabled.collectAsStateWithLifecycle()
    val isDebtLimitAlertEnabled by viewModel.isDebtLimitAlertEnabled.collectAsStateWithLifecycle()
    val isDailyReminderEnabled by viewModel.isDailyReminderEnabled.collectAsStateWithLifecycle()
    val dailyReminderHour by viewModel.dailyReminderHour.collectAsStateWithLifecycle()
    val dailyReminderMinute by viewModel.dailyReminderMinute.collectAsStateWithLifecycle()
    val dailyReminderDays by viewModel.dailyReminderDays.collectAsStateWithLifecycle()
    val skipReminderIfRecordedToday by viewModel.skipReminderIfRecordedToday.collectAsStateWithLifecycle()
    val isAutoPruneRateHistoryEnabled by viewModel.isAutoPruneRateHistoryEnabled.collectAsStateWithLifecycle()
    val isPrivacyMode by viewModel.isPrivacyMode.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
    val stores by viewModel.storesWithBalances.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    // Dialog state holders
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAddCurrencyDialog by remember { mutableStateOf(false) }
    var currencyToEditRate by remember { mutableStateOf<CurrencyEntity?>(null) }
    var currencyToDelete by remember { mutableStateOf<CurrencyEntity?>(null) }
    var showFullCleanConfirmDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    // Backup & Restore Dialogs
    var parsedBackupToRestore by remember { mutableStateOf<ParsedBackupData?>(null) }
    var showRestoreOptionsDialog by remember { mutableStateOf(false) }
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }
    var backupFileToRestore by remember { mutableStateOf<File?>(null) }
    var backupFileToDelete by remember { mutableStateOf<File?>(null) }

    // Storage Access Framework Launcher to save backup into user-selected folder
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val success = viewModel.exportBackupToUri(context, uri)
                    if (success) {
                        Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح في المجلد المحدد!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "فشل حفظ النسخة في المجلد", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "حدث خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Storage Access Framework Launcher to pick a JSON backup file from anywhere
    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseBackup(
                context = context,
                uri = uri,
                onParsed = { parsedData ->
                    parsedBackupToRestore = parsedData
                    showRestoreOptionsDialog = true
                },
                onError = { err ->
                    Toast.makeText(context, "الملف غير صالح: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Auto prune orphaned rate histories when currency tab is loaded and auto-prune is enabled
    LaunchedEffect(selectedTab, isAutoPruneRateHistoryEnabled) {
        if (selectedTab == SettingsTab.CURRENCY && isAutoPruneRateHistoryEnabled) {
            viewModel.pruneUnusedRateHistory()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Modern Segmented Capsule Tab Bar matching reference design with soft balanced contrast
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val targetBgColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                        } else {
                            Color.Transparent
                        }
                        val animatedBgColor by animateColorAsState(targetValue = targetBgColor, label = "tabBg")

                        val targetContentColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        }
                        val animatedContentColor by animateColorAsState(targetValue = targetContentColor, label = "tabContent")

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_tab_${tab.name.lowercase()}"),
                            shape = RoundedCornerShape(20.dp),
                            color = animatedBgColor,
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null,
                            onClick = { selectedTab = tab }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = if (isSelected) {
                                        Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = CircleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    } else {
                                        Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = animatedContentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tab.title,
                                    color = animatedContentColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                SettingsTab.CURRENCY -> {
                    CurrencyTabContent(
                        currency = currency,
                        defaultCurrencyCode = defaultCurrencyCode,
                        allCurrencies = allCurrencies,
                        allRateHistory = allRateHistory,
                        allTransactions = allTransactions,
                        useLatestRateForAll = useLatestRateForAll,
                        isAutoPruneEnabled = isAutoPruneRateHistoryEnabled,
                        onOpenCurrencyDialog = { showCurrencyDialog = true },
                        onOpenAddCurrencyDialog = { showAddCurrencyDialog = true },
                        onEditRate = { currencyToEditRate = it },
                        onDeleteCurrency = { currencyToDelete = it },
                        onSetDefaultCurrency = { curr ->
                            viewModel.setDefaultCurrency(curr)
                            Toast.makeText(context, "تم تعيين العملة الافتراضية بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        onToggleLatestRate = { viewModel.setUseLatestRateForAll(it) },
                        onToggleAutoPrune = { viewModel.setAutoPruneRateHistoryEnabled(it) },
                        onDeleteRateHistory = { historyId ->
                            viewModel.deleteRateHistory(historyId) {
                                Toast.makeText(context, "تم حذف سجل سعر الصرف", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPruneUnusedHistory = {
                            viewModel.pruneUnusedRateHistory { count ->
                                if (count > 0) {
                                    Toast.makeText(context, "تم حذف $count سجلات غير مرتبطة بأي معاملات", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "لا توجد سجلات غير مرتبطة لتنظيفها", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onOpenFullCleanDialog = { showFullCleanConfirmDialog = true }
                    )
                }

                SettingsTab.BACKUP -> {
                    BackupTabContent(
                        lastBackupTime = lastBackupTime,
                        storesCount = stores.size,
                        txCount = allTransactions.size,
                        isAutoBackupEnabled = isAutoBackupEnabled,
                        autoBackupIntervalDays = autoBackupIntervalDays,
                        savedBackups = savedBackups,
                        onExportToFolder = {
                            val timeStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                            createBackupLauncher.launch("نسخة_ديون_$timeStr.json")
                        },
                        onImportFromFile = {
                            restoreFilePickerLauncher.launch("application/json")
                        },
                        onToggleAutoBackup = { viewModel.setAutoBackupEnabled(it) },
                        onSetAutoBackupInterval = { viewModel.setAutoBackupIntervalDays(it) },
                        onRestoreSavedFile = { file -> backupFileToRestore = file },
                        onDeleteSavedFile = { file -> backupFileToDelete = file }
                    )
                }

                SettingsTab.SECURITY -> {
                    SecurityTabContent(
                        isAppLockEnabled = isAppLockEnabled,
                        isBiometricEnabled = isBiometricEnabled,
                        isPrivacyMode = isPrivacyMode,
                        onToggleAppLock = { enabled ->
                            if (enabled) {
                                showPinSetupDialog = true
                            } else {
                                viewModel.setAppLock(false)
                                Toast.makeText(context, "تم إلغاء قفل التطبيق", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onChangePin = { showPinSetupDialog = true },
                        onToggleBiometric = { enabled ->
                            if (enabled) {
                                if (BiometricHelper.isBiometricAvailable(context)) {
                                    viewModel.setBiometricEnabled(true)
                                    Toast.makeText(context, "تم تفعيل القفل بالبصمة", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "البصمة غير متوفرة أو غير معرفة على جهازك", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                viewModel.setBiometricEnabled(false)
                            }
                        },
                        onTogglePrivacyMode = { viewModel.setPrivacyMode(it) },
                        onClearAllData = { showClearDataConfirm = true }
                    )
                }

                SettingsTab.PREFERENCES -> {
                    PreferencesTabContent(
                        themeMode = themeMode,
                        themePalette = themePalette,
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onSetThemePalette = { viewModel.setThemePalette(it) },
                        isSmartSummaryEnabled = isSmartSummaryEnabled,
                        isDebtLimitAlertEnabled = isDebtLimitAlertEnabled,
                        onToggleSmartSummary = { viewModel.setSmartSummaryNotificationEnabled(it) },
                        onToggleDebtLimitAlert = { viewModel.setDebtLimitAlertEnabled(it) },
                        isDailyReminderEnabled = isDailyReminderEnabled,
                        dailyReminderHour = dailyReminderHour,
                        dailyReminderMinute = dailyReminderMinute,
                        dailyReminderDays = dailyReminderDays,
                        skipReminderIfRecordedToday = skipReminderIfRecordedToday,
                        onToggleDailyReminder = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !DebtNotificationHelper.hasNotificationPermission(context)) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setDailyReminderEnabled(enabled, context)
                        },
                        onSetDailyReminderTime = { h, m -> viewModel.setDailyReminderTime(h, m, context) },
                        onSetDailyReminderDays = { days -> viewModel.setDailyReminderDays(days, context) },
                        onToggleSkipReminderIfRecordedToday = { viewModel.setSkipReminderIfRecordedToday(it) }
                    )
                }
            }
        }
    }

    // ================== DIALOGS ==================

    // 1. Add Currency Dialog
    if (showAddCurrencyDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var symbol by remember { mutableStateOf("") }
        var rateStr by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showAddCurrencyDialog = false },
            title = { Text("إضافة عملة جديدة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("رمز العملة (مثل USD, EUR, SAR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_currency_code_input")
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم العملة (مثل دولار، يورو)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_currency_name_input")
                    )
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it },
                        label = { Text("الشعار / الرمز المختصر (مثل $ أو ر.س)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_currency_symbol_input")
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("سعر الصرف مقابل العملة الافتراضية ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_currency_rate_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = rateStr.toDoubleOrNull() ?: 1.0
                        if (code.isNotBlank() && name.isNotBlank()) {
                            viewModel.addCurrency(
                                code = code.trim(),
                                name = name.trim(),
                                symbol = if (symbol.isBlank()) code.trim() else symbol.trim(),
                                exchangeRate = rate
                            ) {
                                Toast.makeText(context, "تمت إضافة عملة $name بنجاح", Toast.LENGTH_SHORT).show()
                            }
                            showAddCurrencyDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_currency_button")
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCurrencyDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 2. Edit Exchange Rate Dialog
    currencyToEditRate?.let { curr ->
        var newRateStr by remember(curr) { mutableStateOf(curr.exchangeRate.toString()) }
        var rateNote by remember(curr) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { currencyToEditRate = null },
            title = { Text("تعديل سعر صرف ${curr.name} (${curr.code})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1 ${curr.code} = كم يعادل بـ $currency (الافتراضية)؟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newRateStr,
                        onValueChange = { newRateStr = it },
                        label = { Text("سعر الصرف الجديد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_currency_rate_input")
                    )
                    OutlinedTextField(
                        value = rateNote,
                        onValueChange = { rateNote = it },
                        label = { Text("ملاحظة سبب التغيير (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = newRateStr.toDoubleOrNull()
                        if (rate != null && rate > 0) {
                            viewModel.updateExchangeRate(curr.code, rate, rateNote)
                            Toast.makeText(context, "تم تحديث سعر الصرف وتسجيله في السجل التاريخي", Toast.LENGTH_SHORT).show()
                            currencyToEditRate = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_edit_rate_button")
                ) {
                    Text("حفظ التحديث")
                }
            },
            dismissButton = {
                TextButton(onClick = { currencyToEditRate = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3. Delete Currency Dialog
    currencyToDelete?.let { curr ->
        ConfirmActionDialog(
            title = "حذف عملة ${curr.name}",
            message = "هل أنت متأكد من رغبتك في حذف عملة ${curr.name} (${curr.code}) من قائمة العملات؟ سيتم أيضاً حذف سجلات أسعار الصرف المرتبطة بها.",
            confirmText = "حذف العملة",
            isDestructive = true,
            onDismiss = { currencyToDelete = null },
            onConfirm = {
                viewModel.deleteCurrency(curr.code)
                Toast.makeText(context, "تم حذف العملة ${curr.name}", Toast.LENGTH_SHORT).show()
                currencyToDelete = null
            }
        )
    }

    // 4. Default Currency Change Dialog
    if (showCurrencyDialog) {
        var customCurrency by remember { mutableStateOf(currency) }
        val currencyPresets = listOf("ر.س", "ر.ي", "د.إ", "$", "€", "ج.م", "د.ك", "د.ع")

        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("تغيير رمز العملة الافتراضية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر من الرموز الشائعة:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currencyPresets.take(4).forEach { p ->
                            FilterChip(
                                selected = customCurrency == p,
                                onClick = { customCurrency = p },
                                label = { Text(p) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currencyPresets.drop(4).forEach { p ->
                            FilterChip(
                                selected = customCurrency == p,
                                onClick = { customCurrency = p },
                                label = { Text(p) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customCurrency,
                        onValueChange = { customCurrency = it },
                        label = { Text("أو اكتب رمز العملة يدوياً") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customCurrency.isNotBlank()) {
                            viewModel.setCurrency(customCurrency)
                            Toast.makeText(context, "تم ضبط العملة: $customCurrency", Toast.LENGTH_SHORT).show()
                        }
                        showCurrencyDialog = false
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 5. Pin Setup Dialog
    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onPinConfirmed = { newPin ->
                viewModel.setAppLock(true, newPin)
                showPinSetupDialog = false
                Toast.makeText(context, "تم تعيين وتفعيل رمز PIN بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 6. Restore Mode Dialog (File Picker JSON)
    if (showRestoreOptionsDialog && parsedBackupToRestore != null) {
        val data = parsedBackupToRestore!!
        AlertDialog(
            onDismissRequest = { showRestoreOptionsDialog = false },
            title = {
                Text(
                    text = "خيارات استعادة النسخة الاحتياطية",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تم قراءة ملف النسخة بنجاح من جهازك:")
                    Text("• تاريخ التصدير: ${data.exportDate}", fontWeight = FontWeight.Medium)
                    Text("• عدد المحلات: ${data.stores.size} محل/عميل")
                    Text("• عدد المعاملات: ${data.transactions.size} معاملة")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اختر كيفية استعادة البيانات:",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showRestoreOptionsDialog = false
                            viewModel.restoreBackupData(
                                data = data,
                                replaceExisting = false,
                                onSuccess = {
                                    Toast.makeText(context, "تم دمج البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "فشل الدمج: $err", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.testTag("restore_merge_button")
                    ) {
                        Text("دمج مع الحالي")
                    }

                    Button(
                        onClick = {
                            showRestoreOptionsDialog = false
                            showReplaceConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DebtRed),
                        modifier = Modifier.testTag("restore_replace_option_button")
                    ) {
                        Text("استبدال بالكامل")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreOptionsDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 7. Replace Confirm Dialog
    if (showReplaceConfirmDialog && parsedBackupToRestore != null) {
        val data = parsedBackupToRestore!!
        ConfirmActionDialog(
            title = "تأكيد الاستبدال الشامل للبيانات",
            message = "تحذير: سيتم حذف جميع المحلات والمعاملات الحالية نهائياً واستبدالها ببيانات النسخة (${data.stores.size} محل و ${data.transactions.size} معاملة). هل تريد المتابعة؟",
            confirmText = "نعم، استبدال بالكامل",
            isDestructive = true,
            onDismiss = { showReplaceConfirmDialog = false },
            onConfirm = {
                showReplaceConfirmDialog = false
                viewModel.restoreBackupData(
                    data = data,
                    replaceExisting = true,
                    onSuccess = {
                        Toast.makeText(context, "تمت الاستعادة بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "فشل الاستعادة: $err", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // 8. Restore Saved Local File Dialog
    if (backupFileToRestore != null) {
        val file = backupFileToRestore!!
        AlertDialog(
            onDismissRequest = { backupFileToRestore = null },
            title = { Text("استعادة من النسخة المحفوظة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الملف: ${file.name}")
                    Text("اختر أسلوب الاستعادة:")
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val f = backupFileToRestore
                            backupFileToRestore = null
                            if (f != null) {
                                viewModel.restoreSavedBackupFile(
                                    file = f,
                                    replaceExisting = false,
                                    onSuccess = { Toast.makeText(context, "تم الدمج بنجاح!", Toast.LENGTH_SHORT).show() },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                )
                            }
                        }
                    ) {
                        Text("دمج")
                    }
                    Button(
                        onClick = {
                            val f = backupFileToRestore
                            backupFileToRestore = null
                            if (f != null) {
                                viewModel.restoreSavedBackupFile(
                                    file = f,
                                    replaceExisting = true,
                                    onSuccess = { Toast.makeText(context, "تمت الاستعادة الشاملة بنجاح!", Toast.LENGTH_SHORT).show() },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                    ) {
                        Text("استبدال")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { backupFileToRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 9. Delete Saved Backup File Dialog
    if (backupFileToDelete != null) {
        val file = backupFileToDelete!!
        ConfirmActionDialog(
            title = "حذف ملف النسخة الاحتياطية",
            message = "هل أنت متأكد من رغبتك في حذف ملف (${file.name}) من الذاكرة نهائياً؟",
            confirmText = "حذف",
            isDestructive = true,
            onDismiss = { backupFileToDelete = null },
            onConfirm = {
                viewModel.deleteSavedBackup(file)
                backupFileToDelete = null
                Toast.makeText(context, "تم حذف ملف النسخة", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 10. Clear All Data Confirm Dialog
    if (showClearDataConfirm) {
        ConfirmActionDialog(
            title = "تأكيد مسح جميع البيانات",
            message = "تحذير شديد الخطورة: سيتم حذف جميع المحلات والعملاء والديون والمدفوعات بشكل نهائي لا يمكن التراجع عنه. هل أنت متأكد؟",
            confirmText = "مسح كل البيانات",
            isDestructive = true,
            onDismiss = { showClearDataConfirm = false },
            onConfirm = {
                showClearDataConfirm = false
                viewModel.clearAllData {
                    Toast.makeText(context, "تم مسح جميع البيانات بنجاح", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            }
        )
    }

    // 11. Full Clean Rate History Modal Bottom Sheet
    if (showFullCleanConfirmDialog) {
        ModalBottomSheet(
            onDismissRequest = { showFullCleanConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 40.dp)
            ) {
                Text(
                    text = "تنظيف كامل للسجل التاريخي",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "سيتم حذف جميع السجلات التاريخية المؤرشفة (المغلقة) والاحتفاظ فقط بأحدث سعر نشط لكل عملة. قد يؤثر ذلك على دقة تقييم المعاملات القديمة. هل تريد المتابعة؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showFullCleanConfirmDialog = false }
                    ) {
                        Text(
                            text = "إلغاء",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                    TextButton(
                        onClick = {
                            showFullCleanConfirmDialog = false
                            viewModel.clearAllArchivedRateHistory { count ->
                                if (count > 0) {
                                    Toast.makeText(context, "تم حذف $count سجلات تاريخية بنجاح", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "لا توجد سجلات تاريخية مؤرشفة لحذفها", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "تنظيف كامل",
                            fontWeight = FontWeight.Bold,
                            color = DebtRed,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== TAB 1: CURRENCY & EXCHANGE RATES ====================
@Composable
private fun CurrencyTabContent(
    currency: String,
    defaultCurrencyCode: String,
    allCurrencies: List<CurrencyEntity>,
    allRateHistory: List<ExchangeRateHistoryEntity>,
    allTransactions: List<com.example.data.entity.TransactionEntity>,
    useLatestRateForAll: Boolean,
    isAutoPruneEnabled: Boolean,
    onOpenCurrencyDialog: () -> Unit,
    onOpenAddCurrencyDialog: () -> Unit,
    onEditRate: (CurrencyEntity) -> Unit,
    onDeleteCurrency: (CurrencyEntity) -> Unit,
    onSetDefaultCurrency: (CurrencyEntity) -> Unit,
    onToggleLatestRate: (Boolean) -> Unit,
    onToggleAutoPrune: (Boolean) -> Unit,
    onDeleteRateHistory: (Long) -> Unit,
    onPruneUnusedHistory: () -> Unit,
    onOpenFullCleanDialog: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.US) }

    val usedRates = remember(allTransactions) {
        allTransactions.map { it.currencyCode.uppercase() to it.exchangeRate }.toSet()
    }
    val activeCurrenciesMap = remember(allCurrencies) {
        allCurrencies.associate { it.code.uppercase() to it.exchangeRate }
    }
    val orphanedCount = remember(allRateHistory, usedRates, activeCurrenciesMap) {
        allRateHistory.count { history ->
            val isCurrentActive = activeCurrenciesMap[history.currencyCode.uppercase()] == history.exchangeRate && history.effectiveTo == null
            !isCurrentActive && !usedRates.contains(history.currencyCode.uppercase() to history.exchangeRate)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Primary Currency Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "العملة الأساسية النشطة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currency ($defaultCurrencyCode)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenCurrencyDialog,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("change_currency_button")
                    ) {
                        Text("تعديل الرمز", fontSize = 12.sp)
                    }
                }
            }
        }

        // Exchange Rate Mode Toggle Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "استخدام سعر الصرف الأخير لجميع السجلات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (useLatestRateForAll)
                                "مفعل: يتم احتساب كافة الديون السابقة والجديدة وفق أحدث سعر صرف مسجل."
                            else
                                "معطل: تحتفظ كل حركة دين بسعر الصرف المسجل وقت إنشائها كمعاملة تاريخية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useLatestRateForAll,
                        onCheckedChange = onToggleLatestRate,
                        modifier = Modifier.testTag("use_latest_rate_switch")
                    )
                }
            }
        }

        // Available Currencies Header & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "العملات وأسعار الصرف المعرفة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allCurrencies.size} عملات متوفرة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenAddCurrencyDialog,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_currency_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة عملة", fontSize = 13.sp)
                }
            }
        }

        // Currencies List
        items(allCurrencies) { curr ->
            val isDef = curr.isDefault || curr.code.equals(defaultCurrencyCode, ignoreCase = true)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDef)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDef) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDef) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = curr.symbol.take(3),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDef) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = curr.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "(${curr.code})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isDef) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PaymentGreenContainer
                                    ) {
                                        Text(
                                            text = "افتراضية",
                                            color = PaymentGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isDef)
                                    "1 ${curr.code} = 1.0 (العملة الأساسية)"
                                else
                                    "1 ${curr.code} = ${FormatUtils.formatDecimal(curr.exchangeRate)} $currency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onEditRate(curr) },
                            modifier = Modifier.testTag("edit_rate_button_${curr.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل سعر الصرف",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (!isDef) {
                            IconButton(
                                onClick = { onSetDefaultCurrency(curr) },
                                modifier = Modifier.testTag("set_default_button_${curr.code}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StarOutline,
                                    contentDescription = "تعيين كافتراضية",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteCurrency(curr) },
                                modifier = Modifier.testTag("delete_currency_button_${curr.code}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف العملة",
                                    tint = DebtRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================== SECTION 3: EXCHANGE RATE HISTORY ==================
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = "سجل أسعار الصرف التاريخي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "مراجعة وتنظيف السجلات التي لم تعد لها معاملات مرتبطة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoDelete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Two KPI Stat Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Right card in RTL: Total records
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${allRateHistory.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "إجمالي السجلات",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Left card in RTL: Orphaned records
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, PaymentGreen.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$orphanedCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaymentGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "غير مرتبطة بمعاملات",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Auto Clean Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "التنظيف التلقائي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "حذف السجلات غير المرتبطة تلقائياً عند حذف المعاملات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isAutoPruneEnabled,
                    onCheckedChange = onToggleAutoPrune
                )
            }
        }

        // Action Buttons Row: "تنظيف غير المرتبطة" and "تنظيف كامل"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Right button (RTL): "تنظيف غير المرتبطة"
                OutlinedButton(
                    onClick = onPruneUnusedHistory,
                    enabled = orphanedCount > 0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("prune_unused_history_button"),
                    border = BorderStroke(
                        1.dp,
                        if (orphanedCount > 0) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تنظيف غير المرتبطة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Left button (RTL): "تنظيف كامل"
                OutlinedButton(
                    onClick = onOpenFullCleanDialog,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("full_clean_history_button"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تنظيف كامل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Section Title: "أحدث السجلات"
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "أحدث السجلات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (allRateHistory.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoDelete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد سجلات تاريخية لأسعار الصرف حتى الآن",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(allRateHistory) { history ->
                val isActive = history.effectiveTo == null
                val isCurrentActive = activeCurrenciesMap[history.currencyCode.uppercase()] == history.exchangeRate && isActive
                val isLinkedToTransactions = usedRates.contains(history.currencyCode.uppercase() to history.exchangeRate)
                val isOrphaned = !isCurrentActive && !isLinkedToTransactions

                val fromStr = timeFormat.format(Date(history.effectiveFrom))
                    .replace("AM", "ص")
                    .replace("PM", "م")
                val dateText = if (isActive) {
                    "من $fromStr"
                } else {
                    val toStr = history.effectiveTo?.let {
                        timeFormat.format(Date(it)).replace("AM", "ص").replace("PM", "م")
                    } ?: "الآن"
                    "من $fromStr إلى $toStr"
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_card_${history.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right side in RTL: Currency code + Badges + Date text
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = history.currencyCode,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFD4EDDA)
                                    ) {
                                        Text(
                                            text = "نشط",
                                            color = Color(0xFF155724),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isOrphaned) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "مؤرشف",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PaymentGreen.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, PaymentGreen.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "غير مرتبط",
                                            color = PaymentGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "مؤرشف",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "مرتبط بمعاملات",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Left side in RTL: 1 = 410.0 ر.ي
                        Text(
                            text = "1 = ${FormatUtils.formatDecimal(history.exchangeRate)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ==================== TAB 2: BACKUP & RESTORE ====================
@Composable
private fun BackupTabContent(
    lastBackupTime: Long,
    storesCount: Int,
    txCount: Int,
    isAutoBackupEnabled: Boolean,
    autoBackupIntervalDays: Int,
    savedBackups: List<BackupFileInfo>,
    onExportToFolder: () -> Unit,
    onImportFromFile: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onSetAutoBackupInterval: (Int) -> Unit,
    onRestoreSavedFile: (File) -> Unit,
    onDeleteSavedFile: (File) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val lastBackupFormatted = if (lastBackupTime > 0L) dateFormat.format(Date(lastBackupTime)) else "لم يتم إجراء نسخة بعد"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "حالة أمان البيانات والنسخ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "آخر نسخة: $lastBackupFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$storesCount",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "محل / عميل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$txCount",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "معاملة مسجلة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Action 1: Export & Save in Custom Folder (Storage Access Framework)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "النسخ الاحتياطي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onExportToFolder,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_to_folder_button")
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء و تصدير نسخة احتياطية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action 2: Import & Restore from JSON File
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = PaymentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "استعادة البيانات من ملف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "اختر ملف نسخة احتياطية بصيغة JSON من جهازك لاسترجاع المحلات والديون مع إمكانية الدمج أو الاستبدال الكامل.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onImportFromFile,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_from_file_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار ملف واستعادة البيانات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action 3: Unified Auto Backup Setting Card (The ONLY place in the app!)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "النسخ الاحتياطي التلقائي الدوري",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يقوم التطبيق بإنشاء وحفظ نسخة احتياطية في ذاكرة التطبيق تلقائياً لمنع فقدان بياناتك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = onToggleAutoBackup,
                            modifier = Modifier.testTag("auto_backup_toggle")
                        )
                    }

                    if (isAutoBackupEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            text = "تكرار النسخ التلقائي:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1 to "يومياً", 3 to "كل 3 أيام", 7 to "أسبوعياً").forEach { (days, label) ->
                                FilterChip(
                                    selected = autoBackupIntervalDays == days,
                                    onClick = { onSetAutoBackupInterval(days) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Locally Saved App Backups
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "النسخ المحفوظة على هذا الجهاز (${savedBackups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (savedBackups.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد نسخ احتياطية محفوظة حالياً في ذاكرة التطبيق.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(savedBackups) { info ->
                val dateStr = dateFormat.format(Date(info.lastModified))
                val sizeKb = (info.sizeBytes / 1024).coerceAtLeast(1)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = info.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$dateStr • الحجم: $sizeKb كيلوبايت",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (info.storesCount > 0 || info.transactionsCount > 0) {
                                Text(
                                    text = "${info.storesCount} محل • ${info.transactionsCount} معاملة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = { onRestoreSavedFile(info.file) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("استعادة", fontSize = 12.sp)
                            }

                            IconButton(onClick = { onDeleteSavedFile(info.file) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف النسخة",
                                    tint = DebtRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 3: SECURITY & PRIVACY ====================
@Composable
private fun SecurityTabContent(
    isAppLockEnabled: Boolean,
    isBiometricEnabled: Boolean,
    isPrivacyMode: Boolean,
    onToggleAppLock: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onTogglePrivacyMode: (Boolean) -> Unit,
    onClearAllData: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PIN App Lock Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "قفل التطبيق برمز PIN",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAppLockEnabled) "مفعل: يلزم إدخال الرمز لفتح التطبيق" else "معطل حالياً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = onToggleAppLock,
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    if (isAppLockEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        OutlinedButton(
                            onClick = onChangePin,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("change_pin_button")
                        ) {
                            Text("تغيير رمز PIN الحالي", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Biometric Unlock Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "تسجيل الدخول بالبصمة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "فتح قفل التطبيق باستخدام مستشعر البصمة أو الوجه السريع.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        enabled = isAppLockEnabled,
                        modifier = Modifier.testTag("biometric_switch")
                    )
                }
            }
        }

        // Privacy Mode Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = WarningAmber.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "وضع الخصوصية (إخفاء الأرقام)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إخفاء مبالغ الديون والأرصدة على الشاشات برمز نجوم (***) لحماية الخصوصية عند وجود أشخاص بجانبك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isPrivacyMode,
                        onCheckedChange = onTogglePrivacyMode,
                        modifier = Modifier.testTag("privacy_mode_switch")
                    )
                }
            }
        }

        // Danger Zone: Clear Data (تم نقله إلى قسم الأمان)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DebtRedContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, DebtRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("danger_zone_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DebtRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "منطقة الخطر - إعادة تعيين التطبيق",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DebtRed
                        )
                    }
                    Text(
                        text = "مسح جميع المحلات والعملاء والديون والمدفوعات من ذاكرة الهاتف نهائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onClearAllData,
                        colors = ButtonDefaults.buttonColors(containerColor = DebtRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("clear_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح جميع البيانات نهائياً")
                    }
                }
            }
        }
    }
}

// ==================== TAB 4: PREFERENCES & SYSTEM ====================
@Composable
private fun PreferencesTabContent(
    themeMode: String,
    themePalette: String,
    onSetThemeMode: (String) -> Unit,
    onSetThemePalette: (String) -> Unit,
    isSmartSummaryEnabled: Boolean,
    isDebtLimitAlertEnabled: Boolean,
    onToggleSmartSummary: (Boolean) -> Unit,
    onToggleDebtLimitAlert: (Boolean) -> Unit,
    isDailyReminderEnabled: Boolean,
    dailyReminderHour: Int,
    dailyReminderMinute: Int,
    dailyReminderDays: Set<Int>,
    skipReminderIfRecordedToday: Boolean,
    onToggleDailyReminder: (Boolean) -> Unit,
    onSetDailyReminderTime: (Int, Int) -> Unit,
    onSetDailyReminderDays: (Set<Int>) -> Unit,
    onToggleSkipReminderIfRecordedToday: (Boolean) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appearance & Identity Card (مظهر وهوية التطبيق)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("appearance_identity_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "مظهر وهوية التطبيق",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تخصيص نمط العرض والسمات اللونية للهوية البصرية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "نمط العرض (الوضع الليلي):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeOptions = listOf(
                            Triple("SYSTEM", "تلقائي النظام", Icons.Default.BrightnessAuto),
                            Triple("LIGHT", "الوضع الفاتح", Icons.Default.LightMode),
                            Triple("DARK", "الوضع الداكن", Icons.Default.DarkMode)
                        )
                        themeOptions.forEach { (mode, label, icon) ->
                            val isSelected = themeMode.equals(mode, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetThemeMode(mode) },
                                leadingIcon = {
                                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = "السمة اللونية للهوية:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemePalette.entries.forEach { palette ->
                            val isSelected = themePalette.equals(palette.id, ignoreCase = true)
                            Surface(
                                onClick = { onSetThemePalette(palette.id) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                            Surface(
                                                shape = CircleShape,
                                                color = palette.primaryColor,
                                                border = BorderStroke(1.5.dp, Color.White),
                                                modifier = Modifier.size(26.dp)
                                            ) {}
                                            Surface(
                                                shape = CircleShape,
                                                color = palette.secondaryColor,
                                                border = BorderStroke(1.5.dp, Color.White),
                                                modifier = Modifier.size(26.dp)
                                            ) {}
                                        }
                                        Column {
                                            Text(
                                                text = palette.titleAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = palette.subtitleAr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "محدد",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Notifications Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "التنبيهات الذكية والإشعارات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "الملخص الأسبوعي للديون",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "إشعار أسبوعي يلخص إجمالي ديونك والمبالغ المسددة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSmartSummaryEnabled,
                            onCheckedChange = onToggleSmartSummary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "تنبيه تجاوز سقف الدين",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "تنبيه فوري عند تسجيل دين يتجاوز الحد الأقصى للمحل.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDebtLimitAlertEnabled,
                            onCheckedChange = onToggleDebtLimitAlert
                        )
                    }
                }
            }
        }

        // Daily Transaction Reminder Card (قسم التذكير)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("daily_reminder_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Icon, Title and Subtitle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "التذكير",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التذكير",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ضع تذكير تسجيل معاملات.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Main switch: التذكير اليومي
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "التذكير اليومي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDailyReminderEnabled) "مفعّل (يتم التنبيه في الموعد المحدد)" else "متوقف (اضغط للتفعيل)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDailyReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isDailyReminderEnabled,
                                onCheckedChange = onToggleDailyReminder,
                                modifier = Modifier.testTag("daily_reminder_switch")
                            )
                        }
                    }

                    // Additional settings when enabled
                    AnimatedVisibility(visible = isDailyReminderEnabled) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // 1. وقت التذكير (دعم AM / PM وضع 12 ساعة)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "وقت التذكير",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "وضع 12 ساعة (AM / PM)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    onSetDailyReminderTime(hourOfDay, minute)
                                                },
                                                dailyReminderHour,
                                                dailyReminderMinute,
                                                false // 12-hour mode with AM/PM support
                                            ).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = DailyReminderScheduler.format12Hour(dailyReminderHour, dailyReminderMinute),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "انقر لتحديد ساعة ودقيقة التذكير",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                TimePickerDialog(
                                                    context,
                                                    { _, hourOfDay, minute ->
                                                        onSetDailyReminderTime(hourOfDay, minute)
                                                    },
                                                    dailyReminderHour,
                                                    dailyReminderMinute,
                                                    false
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تغيير الوقت")
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // 2. أيام التذكير (تحديد يوم، عدة أيام، أو جميع الأيام مع تمييز بصري واضح)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "أيام التذكير",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    val allDaysSelected = DailyReminderScheduler.WEEK_DAYS.all { dailyReminderDays.contains(it.calendarDay) }
                                    TextButton(
                                        onClick = {
                                            if (allDaysSelected) {
                                                val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                                                onSetDailyReminderDays(setOf(today))
                                            } else {
                                                onSetDailyReminderDays(DailyReminderScheduler.WEEK_DAYS.map { it.calendarDay }.toSet())
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (allDaysSelected) "إلغاء تحديد الكل" else "جميع الأيام",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "يمكنك تحديد يوم واحد أو عدة أيام أو جميع الأيام (الأيام المحددة تظهر بلون مميز):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    DailyReminderScheduler.WEEK_DAYS.forEach { dayInfo ->
                                        val isSelected = dailyReminderDays.contains(dayInfo.calendarDay)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val newDays = if (isSelected) {
                                                        if (dailyReminderDays.size > 1) dailyReminderDays - dayInfo.calendarDay else dailyReminderDays
                                                    } else {
                                                        dailyReminderDays + dayInfo.calendarDay
                                                    }
                                                    onSetDailyReminderDays(newDays)
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = dayInfo.shortName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // 3. خيار ذكي مهم جدًا
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "لا تذكّرني إذا قمت بالتسجيل اليوم",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "لن يتم إرسال التذكير إذا قمت بإضافة أو تسجيل عملية دين او سداد اليوم.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = skipReminderIfRecordedToday,
                                    onCheckedChange = onToggleSkipReminderIfRecordedToday,
                                    modifier = Modifier.testTag("skip_reminder_if_recorded_switch")
                                )
                            }
                        }
                    }
                }
            }
        }

        // App Information Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "تطبيق دفتر الديون والمحلات",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "الإصدار 2.5.0 • بدون إنترنت • خصوصية تامة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Developer & Contact Signature Footer (Yahya Mosa 773063084)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_signature_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Yahya Mosa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "773063084",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
