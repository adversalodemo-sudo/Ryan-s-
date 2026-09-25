package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [InventoryItem::class, ColumnSettings::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.inventoryDao())
                }
            }
        }

        suspend fun populateDatabase(dao: InventoryDao) {
            val defaultSettings = listOf(
                ColumnSettings("numbering", isChecked = true, colorHex = "#1E88E5", fontSizePt = 14, isLocked = false),
                ColumnSettings("description", isChecked = true, colorHex = "#2E7D32", fontSizePt = 14, isLocked = false),
                ColumnSettings("price", isChecked = true, colorHex = "#E65100", fontSizePt = 14, isLocked = false),
                ColumnSettings("barcode", isChecked = true, colorHex = "#757575", fontSizePt = 14, isLocked = false),
                ColumnSettings("quantity", isChecked = true, colorHex = "#C2185B", fontSizePt = 14, isLocked = false)
            )
            dao.insertAllColumnSettings(defaultSettings)
            
            // Add some starting sample inventory items for better out-of-the-box experience
            val sampleItems = listOf(
                InventoryItem(numbering = "SKU-001", description = "Wireless Ergonomic Mouse", price = 1250.0, barcode = "480011002201", quantity = 15),
                InventoryItem(numbering = "SKU-002", description = "Mechanical RGB Keyboard", price = 3499.50, barcode = "480011002202", quantity = 8),
                InventoryItem(numbering = "SKU-003", description = "UltraWide Quad-HD Monitor 34\"", price = 18990.0, barcode = "480011002203", quantity = 4),
                InventoryItem(numbering = "SKU-004", description = "USB-C Multi-port Adapter Dock", price = 1850.0, barcode = "480011002204", quantity = 25),
                InventoryItem(numbering = "SKU-005", description = "Noise-Cancelling Wireless Headphones", price = 5990.0, barcode = "480011002205", quantity = 10)
            )
            sampleItems.forEach { dao.insertItem(it) }
        }
    }
}
