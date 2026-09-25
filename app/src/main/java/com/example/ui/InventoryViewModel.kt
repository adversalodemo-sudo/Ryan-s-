package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ColumnSettings
import com.example.data.InventoryItem
import com.example.data.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sealed class for active screens
sealed class Screen {
    object List : Screen()
    object AddItem : Screen()
    data class EditItem(val itemId: Int) : Screen()
    object Settings : Screen()
    object CloudSync : Screen()
}

// Data class for representing Cloud Backup file history
data class CloudBackupLog(
    val id: String,
    val fileName: String,
    val fileType: String, // "Excel", "CSV", "PDF"
    val timestamp: String,
    val fileSize: String,
    val syncStatus: String // "Synced", "Pending", "Syncing", "Failed"
)

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    // Active screen navigation
    var currentScreen by mutableStateOf<Screen>(Screen.List)
        private set

    // Selected item for edit or view
    var editingItem by mutableStateOf<InventoryItem?>(null)
        private set

    // Observable list of inventory items
    val items: StateFlow<List<InventoryItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observable column settings
    val columnSettings: StateFlow<List<ColumnSettings>> = repository.allColumnSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Helper map to quickly read column settings by name
    val settingsMap: StateFlow<Map<String, ColumnSettings>> = repository.allColumnSettings
        .combine(MutableStateFlow(emptyMap<String, ColumnSettings>())) { list, _ ->
            list.associateBy { it.columnName }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Search query state
    var searchQuery by mutableStateOf("")

    // Cloud Synchronization logs & status
    private val _cloudBackupLogs = MutableStateFlow<List<CloudBackupLog>>(emptyList())
    val cloudBackupLogs: StateFlow<List<CloudBackupLog>> = _cloudBackupLogs.asStateFlow()

    var isCloudSyncEnabled by mutableStateOf(true)
    var cloudAccountEmail by mutableStateOf("adversalodemo@gmail.com")
    var lastSyncTime by mutableStateOf("Just now")
    var isCurrentlySyncing by mutableStateOf(false)

    init {
        // Initialize with default backup history log
        loadDefaultSyncLogs()
    }

    private fun loadDefaultSyncLogs() {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        _cloudBackupLogs.value = listOf(
            CloudBackupLog("1", "flexistock_export_20260925.csv", "CSV", formatter.format(Date(System.currentTimeMillis() - 3600000)), "1.4 KB", "Synced"),
            CloudBackupLog("2", "flexistock_export_20260925.xls", "Excel", formatter.format(Date(System.currentTimeMillis() - 7200000)), "12.5 KB", "Synced"),
            CloudBackupLog("3", "flexistock_catalog_20260925.pdf", "PDF", formatter.format(Date(System.currentTimeMillis() - 14400000)), "48.2 KB", "Synced")
        )
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun startEditingItem(item: InventoryItem) {
        editingItem = item
        navigateTo(Screen.EditItem(item.id))
    }

    // CRUD Operations
    fun saveItem(
        id: Int = 0,
        numbering: String,
        description: String,
        price: Double,
        barcode: String,
        quantity: Int,
        imageUri: String?
    ) {
        viewModelScope.launch {
            val item = InventoryItem(
                id = id,
                numbering = numbering,
                description = description,
                price = price,
                barcode = barcode,
                quantity = quantity,
                imageUri = imageUri,
                timestamp = System.currentTimeMillis()
            )
            if (id == 0) {
                repository.insertItem(item)
            } else {
                repository.updateItem(item)
            }
            navigateTo(Screen.List)
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    // Column settings actions
    fun updateColumnVisibility(columnName: String, isVisible: Boolean) {
        viewModelScope.launch {
            val current = settingsMap.value[columnName] ?: ColumnSettings(columnName)
            repository.updateColumnSettings(current.copy(isChecked = isVisible))
        }
    }

    fun updateColumnColor(columnName: String, colorHex: String) {
        viewModelScope.launch {
            val current = settingsMap.value[columnName] ?: ColumnSettings(columnName)
            repository.updateColumnSettings(current.copy(colorHex = colorHex))
        }
    }

    fun updateColumnFontSize(columnName: String, sizePt: Int) {
        viewModelScope.launch {
            val current = settingsMap.value[columnName] ?: ColumnSettings(columnName)
            repository.updateColumnSettings(current.copy(fontSizePt = sizePt))
        }
    }

    fun updateColumnLock(columnName: String, isLocked: Boolean) {
        viewModelScope.launch {
            val current = settingsMap.value[columnName] ?: ColumnSettings(columnName)
            repository.updateColumnSettings(current.copy(isLocked = isLocked))
        }
    }

    // Image Copy Utility to local storage
    fun copyImageToLocal(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Export Excel, CSV, PDF
    fun generateExportContent(format: String): Pair<String, String> {
        val itemList = items.value
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val filename = "flexistock_export_${formatter.format(Date())}"

        return when (format.lowercase()) {
            "csv" -> {
                val csv = StringBuilder()
                csv.append("Numbering,Description,Price (PHP),Barcode,Quantity\n")
                for (item in itemList) {
                    csv.append("\"${escapeCsv(item.numbering)}\",")
                    csv.append("\"${escapeCsv(item.description)}\",")
                    csv.append("${item.price},")
                    csv.append("\"${escapeCsv(item.barcode)}\",")
                    csv.append("${item.quantity}\n")
                }
                Pair("$filename.csv", csv.toString())
            }
            "excel" -> {
                // Return stylized HTML Table containing correct table formats, which Excel opens perfectly and with styles!
                val html = StringBuilder()
                html.append("<html>\n<head>\n<meta charset=\"utf-8\">\n<style>\n")
                html.append("table { border-collapse: collapse; width: 100%; font-family: sans-serif; }\n")
                html.append("th { background-color: #1E88E5; color: white; padding: 10px; border: 1px solid #ddd; }\n")
                html.append("td { padding: 8px; border: 1px solid #ddd; text-align: left; }\n")
                html.append("tr:nth-child(even) { background-color: #f2f2f2; }\n")
                html.append("</style>\n</head>\n<body>\n")
                html.append("<h2>FlexiStock Inventory Export</h2>\n")
                html.append("<table>\n")
                html.append("<thead>\n<tr>\n<th>Numbering</th>\n<th>Description</th>\n<th>Price (PHP)</th>\n<th>Barcode</th>\n<th>Quantity</th>\n</tr>\n</thead>\n<tbody>\n")
                for (item in itemList) {
                    html.append("<tr>\n")
                    html.append("<td>${escapeHtml(item.numbering)}</td>\n")
                    html.append("<td>${escapeHtml(item.description)}</td>\n")
                    html.append("<td>₱${String.format(Locale.US, "%,.2f", item.price)}</td>\n")
                    html.append("<td>${escapeHtml(item.barcode)}</td>\n")
                    html.append("<td>${item.quantity}</td>\n")
                    html.append("</tr>\n")
                }
                html.append("</tbody>\n</table>\n</body>\n</html>")
                Pair("$filename.xls", html.toString())
            }
            "pdf" -> {
                // Return beautifully stylized HTML representing high-quality print PDF layout
                val html = StringBuilder()
                html.append("<!DOCTYPE html>\n<html>\n<head>\n<title>Inventory Catalog</title>\n")
                html.append("<style>\n")
                html.append("@page { size: A4; margin: 20mm; }\n")
                html.append("body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; line-height: 1.4; }\n")
                html.append(".header { text-align: center; margin-bottom: 30px; border-bottom: 3px solid #1E88E5; padding-bottom: 10px; }\n")
                html.append(".header h1 { margin: 0; color: #1E88E5; font-size: 28px; }\n")
                html.append(".header p { margin: 5px 0 0; color: #666; font-size: 14px; }\n")
                html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n")
                html.append("th { border-bottom: 2px solid #1E88E5; color: #1E88E5; text-transform: uppercase; font-size: 12px; font-weight: bold; padding: 12px 8px; text-align: left; }\n")
                html.append("td { padding: 12px 8px; border-bottom: 1px solid #eee; font-size: 14px; }\n")
                html.append(".price { font-weight: bold; color: #E65100; }\n")
                html.append(".barcode { font-family: monospace; color: #757575; }\n")
                html.append(".qty { font-weight: bold; text-align: right; }\n")
                html.append(".qty-low { color: #d32f2f; }\n")
                html.append(".footer { text-align: center; font-size: 11px; color: #999; margin-top: 50px; border-top: 1px solid #eee; padding-top: 10px; }\n")
                html.append("</style>\n</head>\n<body>\n")
                html.append("<div class=\"header\">\n<h1>FLEXISTOCK INVENTORY CATALOG</h1>\n")
                html.append("<p>Report Generated: ${SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())} | Total Items: ${itemList.size}</p>\n")
                html.append("</div>\n")
                html.append("<table>\n")
                html.append("<thead>\n<tr>\n<th>Numbering</th>\n<th>Description</th>\n<th>Price (PHP)</th>\n<th>Barcode</th>\n<th style=\"text-align:right;\">Quantity</th>\n</tr>\n</thead>\n<tbody>\n")
                for (item in itemList) {
                    val qtyClass = if (item.quantity <= 5) "qty qty-low" else "qty"
                    html.append("<tr>\n")
                    html.append("<td><strong>${escapeHtml(item.numbering)}</strong></td>\n")
                    html.append("<td>${escapeHtml(item.description)}</td>\n")
                    html.append("<td class=\"price\">₱${String.format(Locale.US, "%,.2f", item.price)}</td>\n")
                    html.append("<td class=\"barcode\">${escapeHtml(item.barcode)}</td>\n")
                    html.append("<td class=\"$qtyClass\">${item.quantity}</td>\n")
                    html.append("</tr>\n")
                }
                html.append("</tbody>\n</table>\n")
                html.append("<div class=\"footer\">\n<p>FlexiStock Cloud Protected Ledger App. Private & Confidential.</p>\n</div>\n")
                html.append("</body>\n</html>")
                Pair("$filename.html", html.toString()) // HTML represents standard printable/PDF container
            }
            else -> Pair("$filename.txt", "No items")
        }
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    // Triggered after successful export to register inside the cloud sync panel
    fun registerBackupLog(fileName: String, fileType: String, sizeInBytes: Long) {
        val sizeStr = when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", sizeInBytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", sizeInBytes / (1024.0 * 1024.0))
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = formatter.format(Date())

        val newLog = CloudBackupLog(
            id = (cloudBackupLogs.value.size + 1).toString(),
            fileName = fileName,
            fileType = fileType,
            timestamp = dateStr,
            fileSize = sizeStr,
            syncStatus = if (isCloudSyncEnabled) "Syncing" else "Pending"
        )

        // Add to history list
        _cloudBackupLogs.value = listOf(newLog) + _cloudBackupLogs.value

        if (isCloudSyncEnabled) {
            // Trigger background sync simulation
            viewModelScope.launch {
                isCurrentlySyncing = true
                kotlinx.coroutines.delay(2000) // Realistic synchronization delay
                _cloudBackupLogs.value = _cloudBackupLogs.value.map {
                    if (it.id == newLog.id) it.copy(syncStatus = "Synced") else it
                }
                lastSyncTime = dateStr
                isCurrentlySyncing = false
            }
        }
    }

    // Bulk Import logic
    fun importFromCsvContent(csvContent: String): Result<Int> {
        return try {
            val lines = csvContent.lines()
            if (lines.isEmpty()) return Result.failure(Exception("File is empty"))

            var importedCount = 0
            // Detect header or match directly
            val headerIndex = if (lines[0].contains("number", ignoreCase = true) || lines[0].contains("desc", ignoreCase = true)) 1 else 0

            viewModelScope.launch {
                for (i in headerIndex until lines.size) {
                    val line = lines[i].trim()
                    if (line.isEmpty()) continue

                    // Parse CSV line simply
                    val tokens = parseCsvLine(line)
                    if (tokens.size >= 2) {
                        val numbering = tokens.getOrNull(0) ?: "IMPORT-${i}"
                        val description = tokens.getOrNull(1) ?: ""
                        val price = tokens.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                        val barcode = tokens.getOrNull(3) ?: ""
                        val quantity = tokens.getOrNull(4)?.toIntOrNull() ?: 0

                        val newItem = InventoryItem(
                            numbering = numbering,
                            description = description,
                            price = price,
                            barcode = barcode,
                            quantity = quantity,
                            timestamp = System.currentTimeMillis()
                        )
                        repository.insertItem(newItem)
                        importedCount++
                    }
                }
            }
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(currentToken.toString().trim())
                currentToken = StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString().trim())
        return result
    }

    fun triggerForceSync() {
        if (isCurrentlySyncing) return
        viewModelScope.launch {
            isCurrentlySyncing = true
            // Mark all pending as syncing, then synced
            _cloudBackupLogs.value = _cloudBackupLogs.value.map {
                if (it.syncStatus != "Synced") it.copy(syncStatus = "Syncing") else it
            }
            kotlinx.coroutines.delay(2500)
            _cloudBackupLogs.value = _cloudBackupLogs.value.map {
                it.copy(syncStatus = "Synced")
            }
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            lastSyncTime = formatter.format(Date())
            isCurrentlySyncing = false
        }
    }

    fun clearSyncHistory() {
        _cloudBackupLogs.value = emptyList()
    }
}

// ViewModelFactory class to supply parameters to the ViewModel in standard Android Compose pattern
class InventoryViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
