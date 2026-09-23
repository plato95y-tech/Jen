package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.StoreEntity
import kotlinx.coroutines.flow.Flow

data class StoreWithBalance(
    val id: Long,
    val name: String,
    val phone: String,
    val notes: String,
    val debtLimit: Double,
    val dueDate: Long?,
    val createdAt: Long,
    val totalDebt: Double,
    val totalPaid: Double,
    val remainingBalance: Double,
    val transactionCount: Int
)

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores ORDER BY name ASC")
    suspend fun getAllStoresDirect(): List<StoreEntity>

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    fun getStoreById(id: Long): Flow<StoreEntity?>

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    suspend fun getStoreByIdDirect(id: Long): StoreEntity?

    @Query("SELECT * FROM stores WHERE name = :name LIMIT 1")
    suspend fun getStoreByName(name: String): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<StoreEntity>): List<Long>

    @Update
    suspend fun updateStore(store: StoreEntity)

    @Delete
    suspend fun deleteStore(store: StoreEntity)

    @Query("DELETE FROM stores WHERE id = :id")
    suspend fun deleteStoreById(id: Long)

    @Query("DELETE FROM stores")
    suspend fun deleteAllStores()

    @Query("""
        SELECT 
            s.id AS id,
            s.name AS name,
            s.phone AS phone,
            s.notes AS notes,
            s.debtLimit AS debtLimit,
            s.dueDate AS dueDate,
            s.createdAt AS createdAt,
            COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE 0.0 END), 0.0) AS totalDebt,
            COALESCE(SUM(CASE WHEN t.type = 'PAYMENT' THEN t.amount ELSE 0.0 END), 0.0) AS totalPaid,
            (COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE 0.0 END), 0.0) - 
             COALESCE(SUM(CASE WHEN t.type = 'PAYMENT' THEN t.amount ELSE 0.0 END), 0.0)) AS remainingBalance,
            COUNT(t.id) AS transactionCount
        FROM stores s
        LEFT JOIN transactions t ON s.id = t.storeId
        GROUP BY s.id
        ORDER BY remainingBalance DESC, s.name ASC
    """)
    fun getStoresWithBalances(): Flow<List<StoreWithBalance>>
}
