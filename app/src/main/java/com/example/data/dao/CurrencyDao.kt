package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CurrencyEntity
import com.example.data.entity.ExchangeRateHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY isDefault DESC, code ASC")
    fun getAllCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies ORDER BY isDefault DESC, code ASC")
    suspend fun getAllCurrenciesDirect(): List<CurrencyEntity>

    @Query("SELECT * FROM currencies WHERE code = :code LIMIT 1")
    fun getCurrencyByCode(code: String): Flow<CurrencyEntity?>

    @Query("SELECT * FROM currencies WHERE code = :code LIMIT 1")
    suspend fun getCurrencyByCodeDirect(code: String): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    fun getDefaultCurrency(): Flow<CurrencyEntity?>

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCurrencyDirect(): CurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(currency: CurrencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Update
    suspend fun updateCurrency(currency: CurrencyEntity)

    @Query("DELETE FROM currencies WHERE code = :code")
    suspend fun deleteCurrencyByCode(code: String)

    @Query("UPDATE currencies SET isDefault = 0")
    suspend fun clearDefaultCurrency()

    @Query("UPDATE currencies SET isDefault = 1, exchangeRate = 1.0 WHERE code = :code")
    suspend fun setDefaultCurrencyByCode(code: String)

    // Rate history
    @Query("SELECT * FROM exchange_rate_history WHERE currencyCode = :currencyCode ORDER BY effectiveFrom DESC")
    fun getRateHistory(currencyCode: String): Flow<List<ExchangeRateHistoryEntity>>

    @Query("SELECT * FROM exchange_rate_history WHERE currencyCode = :currencyCode ORDER BY effectiveFrom DESC")
    suspend fun getRateHistoryDirect(currencyCode: String): List<ExchangeRateHistoryEntity>

    @Query("SELECT * FROM exchange_rate_history ORDER BY effectiveFrom DESC")
    fun getAllRateHistory(): Flow<List<ExchangeRateHistoryEntity>>

    @Query("SELECT * FROM exchange_rate_history ORDER BY effectiveFrom DESC")
    suspend fun getAllRateHistoryDirect(): List<ExchangeRateHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRateHistory(history: ExchangeRateHistoryEntity): Long

    @Query("UPDATE exchange_rate_history SET effectiveTo = :closeTime WHERE currencyCode = :currencyCode AND effectiveTo IS NULL")
    suspend fun closeActiveRateHistory(currencyCode: String, closeTime: Long)

    @Query("DELETE FROM exchange_rate_history WHERE id = :id")
    suspend fun deleteRateHistoryById(id: Long)

    @Query("DELETE FROM exchange_rate_history WHERE currencyCode = :currencyCode")
    suspend fun deleteRateHistoryByCurrency(currencyCode: String)
}
