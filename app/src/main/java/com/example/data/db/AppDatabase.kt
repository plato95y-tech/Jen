package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.CurrencyDao
import com.example.data.dao.StoreDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.CurrencyEntity
import com.example.data.entity.ExchangeRateHistoryEntity
import com.example.data.entity.StoreEntity
import com.example.data.entity.TransactionEntity

@Database(
    entities = [
        StoreEntity::class,
        TransactionEntity::class,
        CurrencyEntity::class,
        ExchangeRateHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun transactionDao(): TransactionDao
    abstract fun currencyDao(): CurrencyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debt_tracker.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
