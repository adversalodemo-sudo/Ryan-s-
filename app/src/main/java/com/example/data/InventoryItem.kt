package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val numbering: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val barcode: String = "",
    val quantity: Int = 0,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
