package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.BalanceSummary
import com.example.data.dao.StoreWithBalance
import com.example.data.db.AppDatabase
import com.example.data.entity.CurrencyEntity
import com.example.data.entity.ExchangeRateHistoryEntity
import com.example.data.entity.StoreEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.data.export.BackupFileInfo
import com.example.data.export.BackupManager
import com.example.data.export.CsvExporter
import com.example.data.export.ParsedBackupData
import com.example.data.export.PdfExporter
import com.example.data.pref.PreferencesManager
import com.example.data.repository.DebtRepository
import com.example.util.CurrencyUtils
import com.example.util.DebtAgingCalculator
import com.example.util.DebtAgingSummary
import com.example.util.DebtNotificationHelper
import com.example.util.StoreDebtRisk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

enum class ReportPeriod(val label: String) {
    THIS_MONTH("الشهر الحالي"),
    LAST_30_DAYS("آخر 30 يوم"),
    ALL("كل الفترات"),
    CUSTOM("فترة مخصصة")
}

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = DebtRepository(db.storeDao(), db.transactionDao(), db.currencyDao())
    val preferencesManager = PreferencesManager(application)

    val currency: StateFlow<String> = preferencesManager.currency
    val defaultCurrencyCode: StateFlow<String> = preferencesManager.defaultCurrencyCode
    val useLatestRateForAll: StateFlow<Boolean> = preferencesManager.useLatestRateForAll
    val lastBackupTime: StateFlow<Long> = preferencesManager.lastBackupTime
    val themeMode: StateFlow<String> = preferencesManager.themeMode
    val themePalette: StateFlow<String> = preferencesManager.themePalette
    val isPrivacyMode: StateFlow<Boolean> = preferencesManager.isPrivacyMode
    val isAppLockEnabled: StateFlow<Boolean> = preferencesManager.isAppLockEnabled
    val isBiometricEnabled: StateFlow<Boolean> = preferencesManager.isBiometricEnabled
    val isUnlocked: StateFlow<Boolean> = preferencesManager.isUnlocked
    val pinCode: StateFlow<String> = preferencesManager.pinCode

    // Daily Transaction Reminder
    val isDailyReminderEnabled: StateFlow<Boolean> = preferencesManager.isDailyReminderEnabled
    val dailyReminderHour: StateFlow<Int> = preferencesManager.dailyReminderHour
    val dailyReminderMinute: StateFlow<Int> = preferencesManager.dailyReminderMinute
    val dailyReminderDays: StateFlow<Set<Int>> = preferencesManager.dailyReminderDays
    val skipReminderIfRecordedToday: StateFlow<Boolean> = preferencesManager.skipReminderIfRecordedToday

    // Auto-backup configuration and saved backup files (Proposal #4)
    val isAutoBackupEnabled: StateFlow<Boolean> = preferencesManager.isAutoBackupEnabled
    val autoBackupIntervalDays: StateFlow<Int> = preferencesManager.autoBackupIntervalDays

    private val _savedBackups = MutableStateFlow<List<BackupFileInfo>>(emptyList())
    val savedBackups: StateFlow<List<BackupFileInfo>> = _savedBackups.asStateFlow()

    // All available currencies from database
    val allCurrencies: StateFlow<List<CurrencyEntity>> = repository.allCurrencies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRateHistory: StateFlow<List<ExchangeRateHistoryEntity>> = repository.getAllRateHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allStores: StateFlow<List<StoreEntity>> = repository.allStores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic stores with balances converted into the default currency
    val storesWithBalances: StateFlow<List<StoreWithBalance>> = combine(
        repository.allStores,
        repository.allTransactions,
        repository.allCurrencies,
        preferencesManager.defaultCurrencyCode,
        preferencesManager.useLatestRateForAll
    ) { stores, transactions, currencies, defCode, useLatest ->
        val rateMap = currencies.associate { it.code to it.exchangeRate }
        val txByStore = transactions.groupBy { it.storeId }
        stores.map { store ->
            val storeTx = txByStore[store.id] ?: emptyList()
            var debtSum = 0.0
            var paidSum = 0.0
            for (tx in storeTx) {
                val converted = CurrencyUtils.convertToDefaultCurrency(
                    amount = tx.amount,
                    txCurrencyCode = tx.currencyCode,
                    txExchangeRate = tx.exchangeRate,
                    defaultCurrencyCode = defCode,
                    useLatestRateForAll = useLatest,
                    latestRateMap = rateMap
                )
                if (tx.type == TransactionType.DEBT) {
                    debtSum += converted
                } else {
                    paidSum += converted
                }
            }
            StoreWithBalance(
                id = store.id,
                name = store.name,
                phone = store.phone,
                notes = store.notes,
                debtLimit = store.debtLimit,
                dueDate = store.dueDate,
                createdAt = store.createdAt,
                totalDebt = debtSum,
                totalPaid = paidSum,
                remainingBalance = debtSum - paidSum,
                transactionCount = storeTx.size
            )
        }.sortedWith(compareByDescending<StoreWithBalance> { it.remainingBalance }.thenBy { it.name })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic overall balance summary converted into the default currency
    val overallSummary: StateFlow<BalanceSummary> = combine(
        repository.allTransactions,
        repository.allCurrencies,
        preferencesManager.defaultCurrencyCode,
        preferencesManager.useLatestRateForAll
    ) { transactions, currencies, defCode, useLatest ->
        val rateMap = currencies.associate { it.code to it.exchangeRate }
        var totalDebt = 0.0
        var totalPaid = 0.0
        for (tx in transactions) {
            val converted = CurrencyUtils.convertToDefaultCurrency(
                amount = tx.amount,
                txCurrencyCode = tx.currencyCode,
                txExchangeRate = tx.exchangeRate,
                defaultCurrencyCode = defCode,
                useLatestRateForAll = useLatest,
                latestRateMap = rateMap
            )
            if (tx.type == TransactionType.DEBT) {
                totalDebt += converted
            } else {
                totalPaid += converted
            }
        }
        BalanceSummary(
            totalDebt = totalDebt,
            totalPaid = totalPaid,
            remainingBalance = totalDebt - totalPaid,
            transactionCount = transactions.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BalanceSummary(0.0, 0.0, 0.0, 0)
    )

    // Advanced Financial Intelligence & Debt Aging (Proposal #5)
    val debtAgingSummary: StateFlow<DebtAgingSummary> = combine(
        storesWithBalances,
        allTransactions
    ) { stores, txs ->
        DebtAgingCalculator.calculateAging(stores, txs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtAgingSummary(
            currentPeriodAmount = 0.0,
            currentPeriodCount = 0,
            currentPeriodPercent = 0.0,
            period30To60Amount = 0.0,
            period30To60Count = 0,
            period30To60Percent = 0.0,
            period60To90Amount = 0.0,
            period60To90Count = 0,
            period60To90Percent = 0.0,
            overdue90PlusAmount = 0.0,
            overdue90PlusCount = 0,
            overdue90PlusPercent = 0.0,
            totalOutstandingDebt = 0.0,
            totalPaid = 0.0,
            totalCumulativeDebt = 0.0,
            collectionRatePercent = 100.0,
            financialHealthGrade = "ممتاز",
            financialHealthColorHex = "#2E7D32",
            financialHealthAdvice = "جاري احتساب البيانات المالية..."
        )
    )

    val rankedStoreRisks: StateFlow<List<StoreDebtRisk>> = combine(
        storesWithBalances,
        allTransactions
    ) { stores, txs ->
        DebtAgingCalculator.rankStoreDebts(stores, txs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dashboard & Visual Analytics
    val dashboardMonthsCount = MutableStateFlow(6)

    fun setDashboardMonthsCount(months: Int) {
        dashboardMonthsCount.value = months
    }

    val dashboardAnalytics: StateFlow<com.example.data.model.DashboardAnalytics> = combine(
        combine(storesWithBalances, repository.allTransactions, repository.allCurrencies) { s, t, c ->
            Triple(s, t, c)
        },
        preferencesManager.defaultCurrencyCode,
        preferencesManager.useLatestRateForAll,
        dashboardMonthsCount
    ) { (stores, txs, currencies), defCode, useLatest, months ->
        com.example.util.DashboardCalculator.calculateAnalytics(
            stores = stores,
            transactions = txs,
            currencies = currencies,
            defaultCurrencyCode = defCode,
            useLatestRate = useLatest,
            monthsCount = months
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.util.DashboardCalculator.calculateAnalytics(
            stores = emptyList(),
            transactions = emptyList(),
            currencies = emptyList(),
            defaultCurrencyCode = "YER",
            useLatestRate = false,
            monthsCount = 6
        )
    )

    // Smart Quick Input: Last used store & Smart Notifications
    val lastUsedStoreId: StateFlow<Long> = preferencesManager.lastUsedStoreId
    val isSmartSummaryNotificationEnabled: StateFlow<Boolean> = preferencesManager.isSmartSummaryNotificationEnabled
    val isDebtLimitAlertEnabled: StateFlow<Boolean> = preferencesManager.isDebtLimitAlertEnabled

    val lastUsedStore: StateFlow<StoreWithBalance?> = combine(
        storesWithBalances,
        lastUsedStoreId
    ) { stores, lastId ->
        stores.firstOrNull { it.id == lastId } ?: stores.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndSeedDefaultCurrencies()
            val defaultCurr = repository.getDefaultCurrencyDirect()
            if (defaultCurr != null) {
                preferencesManager.setDefaultCurrencyCode(defaultCurr.code)
                preferencesManager.setCurrency(defaultCurr.symbol)
            }
        }
        loadSavedBackups()
        checkAndTriggerAutoBackup(application)
        if (preferencesManager.isDailyReminderEnabled.value) {
            com.example.util.DailyReminderScheduler.schedule(application)
        }
    }

    fun getStore(storeId: Long): Flow<StoreEntity?> = repository.getStoreById(storeId)

    fun getStoreSummary(storeId: Long): Flow<BalanceSummary> = combine(
        repository.getTransactionsByStore(storeId),
        allCurrencies,
        preferencesManager.defaultCurrencyCode,
        preferencesManager.useLatestRateForAll
    ) { transactions, currencies, defCode, useLatest ->
        val rateMap = currencies.associate { it.code to it.exchangeRate }
        var debtSum = 0.0
        var paidSum = 0.0
        for (tx in transactions) {
            val converted = CurrencyUtils.convertToDefaultCurrency(
                amount = tx.amount,
                txCurrencyCode = tx.currencyCode,
                txExchangeRate = tx.exchangeRate,
                defaultCurrencyCode = defCode,
                useLatestRateForAll = useLatest,
                latestRateMap = rateMap
            )
            if (tx.type == TransactionType.DEBT) {
                debtSum += converted
            } else {
                paidSum += converted
            }
        }
        BalanceSummary(
            totalDebt = debtSum,
            totalPaid = paidSum,
            remainingBalance = debtSum - paidSum,
            transactionCount = transactions.size
        )
    }

    fun getStoreTransactions(storeId: Long): Flow<List<TransactionEntity>> =
        repository.getTransactionsByStore(storeId)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        withContext(Dispatchers.IO) {
            db.transactionDao().getTransactionById(id)
        }

    fun saveStore(
        id: Long = 0L,
        name: String,
        phone: String,
        notes: String,
        debtLimit: Double,
        dueDate: Long?,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = StoreEntity(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                notes = notes.trim(),
                debtLimit = debtLimit,
                dueDate = dueDate
            )
            val savedId = if (id == 0L) {
                repository.insertStore(store)
            } else {
                repository.updateStore(store)
                id
            }
            // Auto schedule or cancel payment reminder notification
            if (dueDate != null) {
                val balanceSummary = repository.getStoreBalanceSummaryDirect(savedId)
                com.example.util.DebtNotificationHelper.scheduleDueReminder(
                    context = getApplication(),
                    storeId = savedId,
                    storeName = store.name,
                    balance = balanceSummary.remainingBalance,
                    currency = preferencesManager.currency.value,
                    triggerTimeMillis = dueDate
                )
            } else {
                com.example.util.DebtNotificationHelper.cancelReminder(
                    context = getApplication(),
                    storeId = savedId
                )
            }
            withContext(Dispatchers.Main) {
                onDone(savedId)
            }
        }
    }

    fun deleteStore(storeId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.util.DebtNotificationHelper.cancelReminder(
                context = getApplication(),
                storeId = storeId
            )
            repository.deleteStore(storeId)
            if (preferencesManager.isAutoPruneRateHistoryEnabled.value) {
                repository.pruneOrphanedRateHistory()
            }
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun saveTransaction(
        id: Long = 0L,
        storeId: Long,
        storeName: String,
        type: TransactionType,
        amount: Double,
        currencyCode: String = "",
        exchangeRate: Double = 1.0,
        note: String,
        imageUri: String? = null,
        timestamp: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Remember last used store for Smart Input & Smart FAB
            preferencesManager.setLastUsedStoreId(storeId)

            // If updating and replacing previous image, remove old one if different
            if (id > 0L) {
                val oldTx = repository.getTransactionById(id)
                if (oldTx?.imageUri != null && oldTx.imageUri != imageUri) {
                    com.example.util.ImageStorageUtils.deleteImage(oldTx.imageUri)
                }
            }
            val tx = TransactionEntity(
                id = id,
                storeId = storeId,
                storeName = storeName,
                type = type,
                amount = amount,
                currencyCode = currencyCode,
                exchangeRate = exchangeRate,
                note = note.trim(),
                imageUri = imageUri,
                timestamp = timestamp
            )
            if (id == 0L) {
                repository.insertTransaction(tx)
            } else {
                repository.updateTransaction(tx)
            }

            // Check if debt limit is exceeded for this client and trigger notification
            if (type == TransactionType.DEBT && preferencesManager.isDebtLimitAlertEnabled.value) {
                val store = repository.getStoreByIdDirect(storeId)
                if (store != null && store.debtLimit > 0.0) {
                    val balanceSummary = repository.getStoreBalanceSummaryDirect(storeId)
                    if (balanceSummary.remainingBalance > store.debtLimit) {
                        com.example.util.DebtNotificationHelper.showDebtLimitExceededNotification(
                            context = getApplication(),
                            storeName = store.name,
                            currentBalance = balanceSummary.remainingBalance,
                            debtLimit = store.debtLimit,
                            currency = preferencesManager.currency.value
                        )
                    }
                }
            }

            // Update all Android Home Screen Widgets with fresh figures
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())

            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun setLastUsedStoreId(storeId: Long) {
        preferencesManager.setLastUsedStoreId(storeId)
    }

    fun setSmartSummaryNotificationEnabled(enabled: Boolean) {
        preferencesManager.setSmartSummaryNotificationEnabled(enabled)
    }

    fun setDebtLimitAlertEnabled(enabled: Boolean) {
        preferencesManager.setDebtLimitAlertEnabled(enabled)
    }

    fun setDailyReminderEnabled(enabled: Boolean, context: Context) {
        preferencesManager.setDailyReminderEnabled(enabled)
        if (enabled) {
            com.example.util.DailyReminderScheduler.schedule(context)
        } else {
            com.example.util.DailyReminderScheduler.cancel(context)
        }
    }

    fun setDailyReminderTime(hour: Int, minute: Int, context: Context) {
        preferencesManager.setDailyReminderTime(hour, minute)
        if (preferencesManager.isDailyReminderEnabled.value) {
            com.example.util.DailyReminderScheduler.schedule(context)
        }
    }

    fun setDailyReminderDays(days: Set<Int>, context: Context) {
        preferencesManager.setDailyReminderDays(days)
        if (preferencesManager.isDailyReminderEnabled.value) {
            com.example.util.DailyReminderScheduler.schedule(context)
        }
    }

    fun setSkipReminderIfRecordedToday(skip: Boolean) {
        preferencesManager.setSkipReminderIfRecordedToday(skip)
    }

    fun sendTestDailyReminder(context: Context) {
        com.example.util.DailyReminderScheduler.showNotificationNow(context)
    }

    fun sendTestSummaryNotification(isWeekly: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val allStores = repository.getAllStoresDirect()
            val now = System.currentTimeMillis()
            var overdueCount = 0
            for (store in allStores) {
                if (store.dueDate != null && store.dueDate < now) {
                    val stSummary = repository.getStoreBalanceSummaryDirect(store.id)
                    if (stSummary.remainingBalance > 0) {
                        overdueCount++
                    }
                }
            }
            val summary = repository.getOverallBalanceSummaryDirect()
            val totalDebt = (summary.totalDebt - summary.totalPaid).coerceAtLeast(0.0)
            com.example.util.DebtNotificationHelper.showSmartSummaryNotification(
                context = getApplication(),
                overdueCount = overdueCount,
                totalDebt = totalDebt,
                currency = preferencesManager.currency.value,
                isWeekly = isWeekly
            )
        }
    }

    // Currency & Exchange Rate Management (Technical system for multi-currency & historical rates)
    fun setDefaultCurrency(currency: CurrencyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDefaultCurrency(currency.code)
            preferencesManager.setDefaultCurrencyCode(currency.code)
            preferencesManager.setCurrency(currency.symbol)
        }
    }

    fun addCurrency(
        code: String,
        name: String,
        symbol: String,
        exchangeRate: Double,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = CurrencyEntity(
                code = code.trim().uppercase(),
                name = name.trim(),
                symbol = symbol.trim(),
                exchangeRate = exchangeRate,
                isDefault = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertCurrency(curr)
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun updateExchangeRate(
        currencyCode: String,
        newRate: Double,
        note: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExchangeRate(currencyCode, newRate, note)
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun deleteCurrency(currencyCode: String, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!currencyCode.equals(preferencesManager.defaultCurrencyCode.value, ignoreCase = true)) {
                repository.deleteCurrency(currencyCode)
            }
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun setUseLatestRateForAll(useLatest: Boolean) {
        preferencesManager.setUseLatestRateForAll(useLatest)
    }

    fun getRateHistory(currencyCode: String): Flow<List<ExchangeRateHistoryEntity>> =
        repository.getRateHistory(currencyCode)

    fun deleteRateHistory(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRateHistoryById(id)
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    val isAutoPruneRateHistoryEnabled: StateFlow<Boolean> = preferencesManager.isAutoPruneRateHistoryEnabled

    fun setAutoPruneRateHistoryEnabled(enabled: Boolean) {
        preferencesManager.setAutoPruneRateHistoryEnabled(enabled)
    }

    fun pruneUnusedRateHistory(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.pruneOrphanedRateHistory()
            withContext(Dispatchers.Main) {
                onResult(count)
            }
        }
    }

    fun clearAllArchivedRateHistory(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.clearAllArchivedRateHistory()
            withContext(Dispatchers.Main) {
                onResult(count)
            }
        }
    }

    fun deleteTransaction(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getTransactionById(id)
            existing?.imageUri?.let { com.example.util.ImageStorageUtils.deleteImage(it) }
            repository.deleteTransaction(id)
            if (preferencesManager.isAutoPruneRateHistoryEnabled.value) {
                repository.pruneOrphanedRateHistory()
            }
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun getFilteredTransactions(
        period: ReportPeriod,
        customStart: Long,
        customEnd: Long,
        storeId: Long?
    ): Flow<List<TransactionEntity>> {
        val (start, end) = calculatePeriodDates(period, customStart, customEnd)
        return repository.getFilteredTransactions(start, end, storeId)
    }

    suspend fun getFilteredTransactionsDirect(
        period: ReportPeriod,
        customStart: Long,
        customEnd: Long,
        storeId: Long?
    ): List<TransactionEntity> = withContext(Dispatchers.IO) {
        val (start, end) = calculatePeriodDates(period, customStart, customEnd)
        repository.getFilteredTransactionsDirect(start, end, storeId)
    }

    fun calculatePeriodDates(
        period: ReportPeriod,
        customStart: Long,
        customEnd: Long
    ): Pair<Long, Long> {
        val now = Calendar.getInstance()
        return when (period) {
            ReportPeriod.THIS_MONTH -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            ReportPeriod.LAST_30_DAYS -> {
                val end = now.timeInMillis
                val startCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(startCal.timeInMillis, end)
            }
            ReportPeriod.ALL -> {
                Pair(0L, Long.MAX_VALUE)
            }
            ReportPeriod.CUSTOM -> {
                val sCal = Calendar.getInstance().apply {
                    timeInMillis = customStart
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val eCal = Calendar.getInstance().apply {
                    timeInMillis = customEnd
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(sCal.timeInMillis, eCal.timeInMillis)
            }
        }
    }

    suspend fun exportPdf(
        context: Context,
        period: ReportPeriod,
        customStart: Long,
        customEnd: Long,
        storeId: Long?,
        storeName: String?
    ): File = withContext(Dispatchers.IO) {
        val list = getFilteredTransactionsDirect(period, customStart, customEnd, storeId)
        val periodLabel = period.label
        val defaultSymbol = preferencesManager.currency.value
        val defaultCode = preferencesManager.defaultCurrencyCode.value
        val useLatest = preferencesManager.useLatestRateForAll.value
        val rateMap = repository.getAllCurrenciesDirect().associate { it.code to it.exchangeRate }
        PdfExporter.exportTransactionsToPdf(
            context = context,
            transactions = list,
            storeName = storeName,
            periodLabel = periodLabel,
            defaultCurrencySymbol = defaultSymbol,
            defaultCurrencyCode = defaultCode,
            useLatestRateForAll = useLatest,
            latestRateMap = rateMap
        )
    }

    suspend fun exportCsv(
        context: Context,
        period: ReportPeriod,
        customStart: Long,
        customEnd: Long,
        storeId: Long?,
        storeName: String?
    ): File = withContext(Dispatchers.IO) {
        val list = getFilteredTransactionsDirect(period, customStart, customEnd, storeId)
        val defaultSymbol = preferencesManager.currency.value
        val defaultCode = preferencesManager.defaultCurrencyCode.value
        val useLatest = preferencesManager.useLatestRateForAll.value
        val rateMap = repository.getAllCurrenciesDirect().associate { it.code to it.exchangeRate }
        CsvExporter.exportTransactionsToCsv(
            context = context,
            transactions = list,
            storeName = storeName,
            defaultCurrencySymbol = defaultSymbol,
            defaultCurrencyCode = defaultCode,
            useLatestRateForAll = useLatest,
            latestRateMap = rateMap
        )
    }

    suspend fun exportPdfToUri(
        context: Context,
        uri: Uri,
        period: ReportPeriod,
        customStart: Long,
        customEnd: Long,
        storeId: Long?,
        storeName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = exportPdf(context, period, customStart, customEnd, storeId, storeName)
        context.contentResolver.openOutputStream(uri)?.use { os ->
            tempFile.inputStream().use { fis ->
                fis.copyTo(os)
            }
            os.flush()
        }
        true
    }

    suspend fun exportCsvToUri(
        context: Context,
        uri: Uri,
        period: ReportPeriod,
        customStart: Long,
        customEnd: Long,
        storeId: Long?,
        storeName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = exportCsv(context, period, customStart, customEnd, storeId, storeName)
        context.contentResolver.openOutputStream(uri)?.use { os ->
            tempFile.inputStream().use { fis ->
                fis.copyTo(os)
            }
            os.flush()
        }
        true
    }

    suspend fun exportBackup(context: Context): File = withContext(Dispatchers.IO) {
        val stores = repository.getAllStoresDirect()
        val txs = repository.getAllTransactionsDirect()
        val file = BackupManager.exportBackup(
            context = context,
            stores = stores,
            transactions = txs,
            currency = currency.value
        )
        preferencesManager.updateLastBackupTime()
        loadSavedBackups()
        file
    }

    suspend fun exportBackupToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val stores = repository.getAllStoresDirect()
        val txs = repository.getAllTransactionsDirect()
        val curr = currency.value
        val success = BackupManager.exportBackupToUri(context, uri, stores, txs, curr)
        if (success) {
            preferencesManager.updateLastBackupTime()
            loadSavedBackups()
        }
        success
    }

    fun loadSavedBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = BackupManager.listSavedBackups(getApplication())
            _savedBackups.value = list
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        preferencesManager.setAutoBackupEnabled(enabled)
    }

    fun setAutoBackupIntervalDays(days: Int) {
        preferencesManager.setAutoBackupIntervalDays(days)
    }

    fun deleteSavedBackup(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            BackupManager.deleteBackupFile(file)
            loadSavedBackups()
        }
    }

    fun restoreSavedBackupFile(
        file: File,
        replaceExisting: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = BackupManager.parseBackupFromFile(file)
                repository.restoreData(data.stores, data.transactions, replaceExisting)
                if (data.currency.isNotBlank()) {
                    preferencesManager.setCurrency(data.currency)
                }
                preferencesManager.updateLastBackupTime()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "فشل في استعادة البيانات من الملف")
                }
            }
        }
    }

    fun checkAndTriggerAutoBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!preferencesManager.isAutoBackupEnabled.value) return@launch

            val lastTime = preferencesManager.lastBackupTime.value
            val intervalDays = preferencesManager.autoBackupIntervalDays.value
            val now = System.currentTimeMillis()
            val intervalMillis = intervalDays * 24L * 60 * 60 * 1000

            if (lastTime == 0L || (now - lastTime) >= intervalMillis) {
                try {
                    val stores = repository.getAllStoresDirect()
                    val txs = repository.getAllTransactionsDirect()
                    if (stores.isNotEmpty() || txs.isNotEmpty()) {
                        val file = BackupManager.createAutoBackup(
                            context = context,
                            stores = stores,
                            transactions = txs,
                            currency = currency.value
                        )
                        preferencesManager.updateLastBackupTime()
                        loadSavedBackups()
                        DebtNotificationHelper.showBackupSuccessNotification(
                            context = context,
                            fileName = file.name,
                            storesCount = stores.size,
                            txCount = txs.size
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun parseBackup(context: Context, uri: Uri, onParsed: (ParsedBackupData) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = BackupManager.parseBackupFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    onParsed(data)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "فشل في قراءة ملف النسخة الاحتياطية")
                }
            }
        }
    }

    fun restoreBackupData(
        data: ParsedBackupData,
        replaceExisting: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.restoreData(data.stores, data.transactions, replaceExisting)
                if (data.currency.isNotBlank()) {
                    preferencesManager.setCurrency(data.currency)
                }
                preferencesManager.updateLastBackupTime()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "فشل في استعادة البيانات")
                }
            }
        }
    }

    fun setCurrency(newCurrency: String) {
        preferencesManager.setCurrency(newCurrency)
    }

    fun setThemeMode(mode: String) {
        preferencesManager.setThemeMode(mode)
    }

    fun setThemePalette(palette: String) {
        preferencesManager.setThemePalette(palette)
    }

    fun togglePrivacyMode() {
        preferencesManager.togglePrivacyMode()
    }

    fun setPrivacyMode(enabled: Boolean) {
        preferencesManager.setPrivacyMode(enabled)
    }

    fun setAppLock(enabled: Boolean, pin: String = "") {
        preferencesManager.setAppLock(enabled, pin)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        preferencesManager.setBiometricEnabled(enabled)
    }

    fun unlockApp() {
        preferencesManager.unlock()
    }

    fun lockApp() {
        preferencesManager.lock()
    }

    fun verifyPin(pin: String): Boolean {
        return preferencesManager.verifyPin(pin)
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }
}
