package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "column_settings")
data class ColumnSettings(
    @PrimaryKey val columnName: String, // "numbering", "description", "price", "barcode", "quantity"
    val isChecked: Boolean = true,
    val colorHex: String = "#333333", // Default dark gray
    val fontSizePt: Int = 14,          // Default font size in pt
    val isLocked: Boolean = false       // Default unlocked
)
