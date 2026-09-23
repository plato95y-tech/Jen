package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val debtLimit: Double = 0.0, // 0.0 means no limit
    val dueDate: Long? = null, // timestamp in millis if payment reminder set
    val createdAt: Long = System.currentTimeMillis()
)
