package com.example.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val inventoryDao: InventoryDao) {
    val allItems: Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    val allColumnSettings: Flow<List<ColumnSettings>> = inventoryDao.getAllColumnSettings()

    fun getItemById(id: Int): Flow<InventoryItem?> {
        return inventoryDao.getItemById(id)
    }

    suspend fun insertItem(item: InventoryItem): Long {
        return inventoryDao.insertItem(item)
    }

    suspend fun updateItem(item: InventoryItem): Int {
        return inventoryDao.updateItem(item)
    }

    suspend fun deleteItem(item: InventoryItem) {
        inventoryDao.deleteItem(item)
    }

    suspend fun deleteItemById(id: Int) {
        inventoryDao.deleteItemById(id)
    }

    suspend fun updateColumnSettings(settings: ColumnSettings) {
        inventoryDao.insertColumnSettings(settings)
    }

    suspend fun insertAllColumnSettings(settings: List<ColumnSettings>) {
        inventoryDao.insertAllColumnSettings(settings)
    }
}
