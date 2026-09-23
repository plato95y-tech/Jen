package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exchange_rate_history",
    indices = [
        Index(value = ["currencyCode"]),
        Index(value = ["effectiveFrom"])
    ]
)
data class ExchangeRateHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val currencyCode: String,
    val exchangeRate: Double, // Rate relative to default currency
    val effectiveFrom: Long = System.currentTimeMillis(),
    val effectiveTo: Long? = null, // Closed timestamp, or null if active
    val note: String = ""
)
