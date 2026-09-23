package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey
    val code: String, // e.g. "SAR", "YER", "USD"
    val name: String, // e.g. "ريال سعودي", "ريال يمني"
    val symbol: String, // e.g. "ر.س", "ر.ي", "$"
    val exchangeRate: Double, // 1 unit of this currency = exchangeRate units of default currency
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
