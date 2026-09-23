package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class BalanceSummary(
    val totalDebt: Double,
    val totalPaid: Double,
    val remainingBalance: Double,
    val transactionCount: Int
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE storeId = :storeId ORDER BY timestamp DESC, id DESC")
    fun getTransactionsByStore(storeId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE storeId = :storeId ORDER BY timestamp DESC, id DESC")
    suspend fun getTransactionsByStoreDirect(storeId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestTransactionDirect(): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) AS totalDebt,
            COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0) AS totalPaid,
            (COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) - 
             COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0)) AS remainingBalance,
            COUNT(id) AS transactionCount
        FROM transactions
    """)
    fun getOverallBalanceSummary(): Flow<BalanceSummary>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) AS totalDebt,
            COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0) AS totalPaid,
            (COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) - 
             COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0)) AS remainingBalance,
            COUNT(id) AS transactionCount
        FROM transactions
    """)
    suspend fun getOverallBalanceSummaryDirect(): BalanceSummary

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) AS totalDebt,
            COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0) AS totalPaid,
            (COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) - 
             COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0)) AS remainingBalance,
            COUNT(id) AS transactionCount
        FROM transactions
        WHERE storeId = :storeId
    """)
    fun getStoreBalanceSummary(storeId: Long): Flow<BalanceSummary>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) AS totalDebt,
            COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0) AS totalPaid,
            (COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0.0 END), 0.0) - 
             COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0.0 END), 0.0)) AS remainingBalance,
            COUNT(id) AS transactionCount
        FROM transactions
        WHERE storeId = :storeId
    """)
    suspend fun getStoreBalanceSummaryDirect(storeId: Long): BalanceSummary

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND (:storeId IS NULL OR storeId = :storeId)
        ORDER BY timestamp DESC, id DESC
    """)
    fun getTransactionsFiltered(
        startTime: Long,
        endTime: Long,
        storeId: Long?
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND (:storeId IS NULL OR storeId = :storeId)
        ORDER BY timestamp ASC, id ASC
    """)
    suspend fun getTransactionsFilteredDirect(
        startTime: Long,
        endTime: Long,
        storeId: Long?
    ): List<TransactionEntity>

    @Query("""
        SELECT COUNT(id) FROM transactions 
        WHERE (timestamp >= :startOfDay AND timestamp <= :endOfDay)
           OR (createdAt >= :startOfDay AND createdAt <= :endOfDay)
    """)
    suspend fun getTransactionCountBetween(
        startOfDay: Long,
        endOfDay: Long
    ): Int
}
