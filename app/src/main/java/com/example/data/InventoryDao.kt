package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    fun getItemById(id: Int): Flow<InventoryItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Update
    suspend fun updateItem(item: InventoryItem): Int

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("SELECT * FROM column_settings")
    fun getAllColumnSettings(): Flow<List<ColumnSettings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumnSettings(settings: ColumnSettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllColumnSettings(settings: List<ColumnSettings>)
}
