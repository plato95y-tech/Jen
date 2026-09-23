package com.example.data.repository

import com.example.data.dao.BalanceSummary
import com.example.data.dao.CurrencyDao
import com.example.data.dao.StoreDao
import com.example.data.dao.StoreWithBalance
import com.example.data.dao.TransactionDao
import com.example.data.entity.CurrencyEntity
import com.example.data.entity.ExchangeRateHistoryEntity
import com.example.data.entity.StoreEntity
import com.example.data.entity.TransactionEntity
import com.example.util.CurrencyUtils
import kotlinx.coroutines.flow.Flow

class DebtRepository(
    private val storeDao: StoreDao,
    private val transactionDao: TransactionDao,
    private val currencyDao: CurrencyDao
) {
    val storesWithBalances: Flow<List<StoreWithBalance>> = storeDao.getStoresWithBalances()
    val allStores: Flow<List<StoreEntity>> = storeDao.getAllStores()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val overallSummary: Flow<BalanceSummary> = transactionDao.getOverallBalanceSummary()

    // Currencies and Exchange Rates
    val allCurrencies: Flow<List<CurrencyEntity>> = currencyDao.getAllCurrencies()
    val defaultCurrency: Flow<CurrencyEntity?> = currencyDao.getDefaultCurrency()

    suspend fun checkAndSeedDefaultCurrencies() {
        val existing = currencyDao.getAllCurrenciesDirect()
        if (existing.isEmpty()) {
            val defaults = CurrencyUtils.getDefaultCurrenciesList()
            currencyDao.insertCurrencies(defaults)
            for (c in defaults) {
                currencyDao.insertRateHistory(
                    ExchangeRateHistoryEntity(
                        currencyCode = c.code,
                        exchangeRate = c.exchangeRate,
                        effectiveFrom = c.updatedAt,
                        effectiveTo = null,
                        note = "السعر الأولي عند التهيئة"
                    )
                )
            }
        }
    }

    fun getRateHistory(currencyCode: String): Flow<List<ExchangeRateHistoryEntity>> =
        currencyDao.getRateHistory(currencyCode)

    suspend fun getRateHistoryDirect(currencyCode: String): List<ExchangeRateHistoryEntity> =
        currencyDao.getRateHistoryDirect(currencyCode)

    fun getAllRateHistory(): Flow<List<ExchangeRateHistoryEntity>> =
        currencyDao.getAllRateHistory()

    suspend fun insertCurrency(currency: CurrencyEntity) {
        currencyDao.insertCurrency(currency)
        currencyDao.insertRateHistory(
            ExchangeRateHistoryEntity(
                currencyCode = currency.code,
                exchangeRate = currency.exchangeRate,
                effectiveFrom = System.currentTimeMillis(),
                effectiveTo = null,
                note = "إضافة عملة جديدة"
            )
        )
    }

    suspend fun updateCurrency(currency: CurrencyEntity) {
        currencyDao.updateCurrency(currency)
    }

    suspend fun deleteCurrency(code: String) {
        currencyDao.deleteCurrencyByCode(code)
        currencyDao.deleteRateHistoryByCurrency(code)
    }

    suspend fun deleteRateHistoryById(id: Long) {
        currencyDao.deleteRateHistoryById(id)
    }

    suspend fun pruneOrphanedRateHistory(): Int {
        val allHistory = currencyDao.getAllRateHistoryDirect()
        val allTransactions = transactionDao.getAllTransactionsDirect()
        val usedRates = allTransactions.map { it.currencyCode.uppercase() to it.exchangeRate }.toSet()
        val activeCurrencies = currencyDao.getAllCurrenciesDirect().associate { it.code.uppercase() to it.exchangeRate }

        var deletedCount = 0
        for (history in allHistory) {
            val isCurrentActive = activeCurrencies[history.currencyCode.uppercase()] == history.exchangeRate && history.effectiveTo == null
            if (!isCurrentActive) {
                val isUsed = usedRates.contains(history.currencyCode.uppercase() to history.exchangeRate)
                if (!isUsed) {
                    currencyDao.deleteRateHistoryById(history.id)
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    suspend fun clearAllArchivedRateHistory(): Int {
        val allHistory = currencyDao.getAllRateHistoryDirect()
        val grouped = allHistory.groupBy { it.currencyCode.uppercase() }
        var deletedCount = 0
        for ((_, records) in grouped) {
            val activeRecord = records.firstOrNull { it.effectiveTo == null } ?: records.maxByOrNull { it.effectiveFrom }
            for (record in records) {
                if (record.id != activeRecord?.id) {
                    currencyDao.deleteRateHistoryById(record.id)
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    suspend fun setDefaultCurrency(code: String) {
        currencyDao.clearDefaultCurrency()
        currencyDao.setDefaultCurrencyByCode(code)
        // Record in history that rate is 1.0 for default
        val now = System.currentTimeMillis()
        currencyDao.closeActiveRateHistory(code, now)
        currencyDao.insertRateHistory(
            ExchangeRateHistoryEntity(
                currencyCode = code,
                exchangeRate = 1.0,
                effectiveFrom = now,
                effectiveTo = null,
                note = "تعيين كعملة افتراضية أساسية"
            )
        )
    }

    suspend fun updateExchangeRate(currencyCode: String, newRate: Double, note: String = "") {
        val now = System.currentTimeMillis()
        val curr = currencyDao.getCurrencyByCodeDirect(currencyCode) ?: return
        // 1. Close active history
        currencyDao.closeActiveRateHistory(currencyCode, now)
        // 2. Insert new history record (wageHistory with effectiveFrom, effectiveTo = null)
        currencyDao.insertRateHistory(
            ExchangeRateHistoryEntity(
                currencyCode = currencyCode,
                exchangeRate = newRate,
                effectiveFrom = now,
                effectiveTo = null,
                note = note.ifBlank { "تحديث سعر الصرف يدوياً" }
            )
        )
        // 3. Update CurrencyEntity
        currencyDao.updateCurrency(
            curr.copy(
                exchangeRate = newRate,
                updatedAt = now
            )
        )
    }

    suspend fun getAllCurrenciesDirect(): List<CurrencyEntity> =
        currencyDao.getAllCurrenciesDirect()

    suspend fun getDefaultCurrencyDirect(): CurrencyEntity? =
        currencyDao.getDefaultCurrencyDirect()

    fun getStoreById(id: Long): Flow<StoreEntity?> = storeDao.getStoreById(id)
    suspend fun getStoreByIdDirect(id: Long): StoreEntity? = storeDao.getStoreByIdDirect(id)

    fun getStoreBalanceSummary(storeId: Long): Flow<BalanceSummary> =
        transactionDao.getStoreBalanceSummary(storeId)

    suspend fun getStoreBalanceSummaryDirect(storeId: Long): BalanceSummary =
        transactionDao.getStoreBalanceSummaryDirect(storeId)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun getLatestTransactionDirect(): TransactionEntity? =
        transactionDao.getLatestTransactionDirect()

    fun getTransactionsByStore(storeId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByStore(storeId)

    suspend fun getTransactionsByStoreDirect(storeId: Long): List<TransactionEntity> =
        transactionDao.getTransactionsByStoreDirect(storeId)

    fun getFilteredTransactions(
        startTime: Long,
        endTime: Long,
        storeId: Long?
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsFiltered(startTime, endTime, storeId)

    suspend fun getFilteredTransactionsDirect(
        startTime: Long,
        endTime: Long,
        storeId: Long?
    ): List<TransactionEntity> =
        transactionDao.getTransactionsFilteredDirect(startTime, endTime, storeId)

    suspend fun insertStore(store: StoreEntity): Long = storeDao.insertStore(store)
    suspend fun updateStore(store: StoreEntity) = storeDao.updateStore(store)

    suspend fun deleteStore(storeId: Long) {
        transactionDao.getTransactionsByStoreDirect(storeId).forEach {
            transactionDao.deleteTransaction(it)
        }
        storeDao.deleteStoreById(storeId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteTransactionById(id)

    suspend fun getAllStoresDirect(): List<StoreEntity> = storeDao.getAllStoresDirect()
    suspend fun getAllTransactionsDirect(): List<TransactionEntity> =
        transactionDao.getAllTransactionsDirect()
    suspend fun getOverallBalanceSummaryDirect() = transactionDao.getOverallBalanceSummaryDirect()

    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        storeDao.deleteAllStores()
    }

    suspend fun restoreData(
        stores: List<StoreEntity>,
        transactions: List<TransactionEntity>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            clearAllData()
            for (s in stores) {
                storeDao.insertStore(s)
            }
            for (t in transactions) {
                transactionDao.insertTransaction(t)
            }
        } else {
            // Merge mode: map existing or add new
            for (s in stores) {
                val existing = storeDao.getStoreByName(s.name)
                val targetStoreId: Long = if (existing != null) {
                    existing.id
                } else {
                    storeDao.insertStore(s.copy(id = 0L))
                }
                val relatedTx = transactions.filter { it.storeId == s.id || it.storeName == s.name }
                for (tx in relatedTx) {
                    transactionDao.insertTransaction(
                        tx.copy(id = 0L, storeId = targetStoreId, storeName = s.name)
                    )
                }
            }
        }
    }
}
