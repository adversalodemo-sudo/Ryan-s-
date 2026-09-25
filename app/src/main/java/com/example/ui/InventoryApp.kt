package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ColumnSettings
import com.example.data.InventoryItem
import java.io.File
import java.io.FileWriter
import java.util.Locale

// Predefined available colors for columns
val COLUMN_COLORS = listOf(
    "#212121", // Charcoal Black
    "#1E88E5", // Blue
    "#2E7D32", // Green
    "#E65100", // Orange
    "#C2185B", // Pink
    "#8E24AA", // Purple
    "#00ACC1", // Cyan
    "#7CB342", // Lime
    "#6D4C41", // Brown
    "#757575"  // Grey
)

// Predefined available font sizes
val FONT_SIZES = listOf(6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40)

// Extension function to safely convert hex string to Color
fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Black
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryApp(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen

    // Modals state
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Floating messages / Toast triggers
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Handle Hardware/System Back Presses based on active screens
    BackHandler(enabled = currentScreen != Screen.List) {
        viewModel.navigateTo(Screen.List)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "FlexiStock",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    // Import Symbol
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("import_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import symbol",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Save / Export Symbol
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save symbol",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentScreen is Screen.List,
                    onClick = { viewModel.navigateTo(Screen.List) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Inventory list") },
                    label = { Text("Inventory") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Settings,
                    onClick = { viewModel.navigateTo(Screen.Settings) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Column styling") },
                    label = { Text("Styling") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.CloudSync,
                    onClick = { viewModel.navigateTo(Screen.CloudSync) },
                    icon = {
                        val isSyncing = viewModel.isCurrentlySyncing
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = "Cloud backup")
                        }
                    },
                    label = { Text("Cloud Sync") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                is Screen.List -> {
                    InventoryListScreen(
                        viewModel = viewModel,
                        onAddClick = { viewModel.navigateTo(Screen.AddItem) },
                        onEditClick = { item -> viewModel.startEditingItem(item) }
                    )
                }
                is Screen.AddItem -> {
                    AddEditItemScreen(
                        viewModel = viewModel,
                        itemId = null,
                        onBack = { viewModel.navigateTo(Screen.List) }
                    )
                }
                is Screen.EditItem -> {
                    AddEditItemScreen(
                        viewModel = viewModel,
                        itemId = currentScreen.itemId,
                        onBack = { viewModel.navigateTo(Screen.List) }
                    )
                }
                is Screen.Settings -> {
                    ColumnStylingScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.List) }
                    )
                }
                is Screen.CloudSync -> {
                    CloudSyncDashboardScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.List) }
                    )
                }
            }

            // Export / Save Dialog
            if (showExportDialog) {
                ExportFormatDialog(
                    onDismiss = { showExportDialog = false },
                    onExportSelect = { format ->
                        showExportDialog = false
                        val exportData = viewModel.generateExportContent(format)
                        val fileName = exportData.first
                        val content = exportData.second

                        // Write to cache directory to enable sharing
                        try {
                            val cacheFile = File(context.cacheDir, fileName)
                            FileWriter(cacheFile).use { it.write(content) }

                            // Register in Cloud Sync History logger
                            viewModel.registerBackupLog(fileName, format.uppercase(), cacheFile.length())

                            showToast("Exported $fileName successfully!")

                            // Launch Share intent to let users save, email, upload to real cloud apps
                            val authority = "${context.packageName}.fileprovider"
                            val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = when (format.lowercase()) {
                                    "csv" -> "text/csv"
                                    "excel" -> "application/vnd.ms-excel"
                                    else -> "text/html"
                                }
                                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "FlexiStock Catalog Export")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Save / Sync to Cloud"))
                        } catch (e: Exception) {
                            showToast("Failed to write export file: ${e.message}")
                        }
                    }
                )
            }

            // Import Dialog
            if (showImportDialog) {
                ImportFormatDialog(
                    onDismiss = { showImportDialog = false },
                    onImportTextSubmit = { csvText ->
                        showImportDialog = false
                        val res = viewModel.importFromCsvContent(csvText)
                        if (res.isSuccess) {
                            showToast("Successfully imported ${res.getOrNull()} items!")
                        } else {
                            showToast("Import failed: ${res.exceptionOrNull()?.message}")
                        }
                    },
                    onDemoImport = {
                        showImportDialog = false
                        val demoCsv = """
                            Numbering,Description,Price,Barcode,Quantity
                            SKU-201,Mini Bluetooth Speaker,890.0,480020101,40
                            SKU-202,Smart Fitness Bracelet,1450.0,480020102,18
                            SKU-203,Portable Powerbank 20k mAh,1990.0,480020103,12
                            SKU-204,Magnetic Phone Stand,350.0,480020104,75
                            SKU-205,True Wireless Earbuds,2490.0,480020105,22
                        """.trimIndent()
                        val res = viewModel.importFromCsvContent(demoCsv)
                        if (res.isSuccess) {
                            showToast("Loaded ${res.getOrNull()} demo items!")
                        }
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// INVENTORY LIST SCREEN
// ----------------------------------------------------------------------------------
@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel,
    onAddClick: () -> Unit,
    onEditClick: (InventoryItem) -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val settingsMap by viewModel.settingsMap.collectAsStateWithLifecycle()
    val searchQuery = viewModel.searchQuery

    val filteredItems = items.filter {
        it.description.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true) ||
                it.numbering.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image Graphic
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_inventory_hero),
                    contentDescription = "FlexiStock banner illustration",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Overlay clean design tint & title
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(
                            text = "Flexible Ledger Control",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Custom styling, lock state security, and automated cloud back-up sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Search Bar & Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = { Text("Search catalog...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            FloatingActionButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("add_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add inventory item")
            }
        }

        // Quick Settings Info Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (viewModel.isCloudSyncEnabled) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = "Sync state",
                        tint = if (viewModel.isCloudSyncEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (viewModel.isCloudSyncEnabled) "Cloud Sync: Enabled" else "Cloud Sync: Disabled",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = "${filteredItems.size} of ${items.size} Items Shown",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (filteredItems.isEmpty()) {
            // Beautiful Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory,
                        contentDescription = "Empty icon",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "No Items Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Touch '+' or Import CSV/Excel data to populate your editable ledger catalog.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Scrollable List of Styled Cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        settingsMap = settingsMap,
                        onCardClick = { onEditClick(item) },
                        onDeleteClick = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// INDIVIDUAL INVENTORY CARD COMPONENT WITH CUSTOM DYNAMIC STYLING
// ----------------------------------------------------------------------------------
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    settingsMap: Map<String, ColumnSettings>,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Read individual column settings
    val sNum = settingsMap["numbering"] ?: ColumnSettings("numbering")
    val sDesc = settingsMap["description"] ?: ColumnSettings("description")
    val sPrice = settingsMap["price"] ?: ColumnSettings("price")
    val sBar = settingsMap["barcode"] ?: ColumnSettings("barcode")
    val sQty = settingsMap["quantity"] ?: ColumnSettings("quantity")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image container
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.imageUri != null) {
                        AsyncImage(
                            model = File(item.imageUri),
                            contentDescription = "Item photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "No image placeholder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                // Details block
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 1: Numbering & Lock status indicators
                    if (sNum.isChecked) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = item.numbering.ifEmpty { "N/A" },
                                color = sNum.colorHex.toComposeColor(),
                                fontSize = sNum.fontSizePt.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (sNum.isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked SKU",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    // Row 2: Description
                    if (sDesc.isChecked) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = item.description.ifEmpty { "No description" },
                                color = sDesc.colorHex.toComposeColor(),
                                fontSize = sDesc.fontSizePt.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (sDesc.isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked description",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    // Row 3: Barcode
                    if (sBar.isChecked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = "Barcode logo", modifier = Modifier.size(12.dp), tint = Color.Gray)
                            Text(
                                text = item.barcode.ifEmpty { "No barcode" },
                                color = sBar.colorHex.toComposeColor(),
                                fontSize = sBar.fontSizePt.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            if (sBar.isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked barcode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    // Row 4: Pricing and quantity metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (sPrice.isChecked) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "₱${String.format(Locale.US, "%,.2f", item.price)}",
                                    color = sPrice.colorHex.toComposeColor(),
                                    fontSize = sPrice.fontSizePt.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (sPrice.isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked price",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        if (sQty.isChecked) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Badge(
                                    containerColor = if (item.quantity <= 5) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (item.quantity <= 5) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "Qty: ${item.quantity}",
                                        color = sQty.colorHex.toComposeColor(),
                                        fontSize = sQty.fontSizePt.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                if (sQty.isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked quantity",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Delete quick button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item button",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// ADD / EDIT SCREEN COMPONENT
// ----------------------------------------------------------------------------------
@Composable
fun AddEditItemScreen(
    viewModel: InventoryViewModel,
    itemId: Int?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsStateWithLifecycle()
    val settingsMap by viewModel.settingsMap.collectAsStateWithLifecycle()

    // Find target item if editing
    val itemToEdit = remember(itemId, items) {
        items.find { it.id == itemId }
    }

    // Read Settings to enforce Read-only/Lock configurations
    val sNum = settingsMap["numbering"] ?: ColumnSettings("numbering")
    val sDesc = settingsMap["description"] ?: ColumnSettings("description")
    val sPrice = settingsMap["price"] ?: ColumnSettings("price")
    val sBar = settingsMap["barcode"] ?: ColumnSettings("barcode")
    val sQty = settingsMap["quantity"] ?: ColumnSettings("quantity")

    // Form fields
    var numbering by remember { mutableStateOf(itemToEdit?.numbering ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var priceStr by remember { mutableStateOf(itemToEdit?.price?.toString() ?: "") }
    var barcode by remember { mutableStateOf(itemToEdit?.barcode ?: "") }
    var quantityStr by remember { mutableStateOf(itemToEdit?.quantity?.toString() ?: "0") }
    var localImageUri by remember { mutableStateOf(itemToEdit?.imageUri) }

    // Setup photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val copiedPath = viewModel.copyImageToLocal(context, uri)
                if (copiedPath != null) {
                    localImageUri = copiedPath
                } else {
                    Toast.makeText(context, "Failed to copy image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    fun tryShowLockToast(fieldName: String) {
        Toast.makeText(context, "Column '$fieldName' is currently LOCKED. Go to styling tab to unlock.", Toast.LENGTH_LONG).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Return to catalog list")
                }
                Text(
                    text = if (itemId == null) "Create Inventory Item" else "Modify Item Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Image Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (localImageUri != null) {
                        AsyncImage(
                            model = File(localImageUri!!),
                            contentDescription = "Uploaded preview image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Upload trigger icon", tint = MaterialTheme.colorScheme.primary)
                            Text("Upload Image", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (localImageUri != null) {
                    TextButton(
                        onClick = { localImageUri = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove photo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove Photo")
                    }
                }
            }
        }

        // Form fields block
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Field 1: Numbering
                OutlinedTextField(
                    value = numbering,
                    onValueChange = { if (!sNum.isLocked) numbering = it },
                    label = { Text("Numbering / SKU") },
                    placeholder = { Text("e.g., SKU-100") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_numbering")
                        .clickable(enabled = sNum.isLocked) { tryShowLockToast("Numbering") },
                    enabled = !sNum.isLocked,
                    trailingIcon = {
                        if (sNum.isLocked) {
                            IconButton(onClick = { tryShowLockToast("Numbering") }) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked field symbol")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Field 2: Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (!sDesc.isLocked) description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Ergonomic computer accessory") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_description")
                        .clickable(enabled = sDesc.isLocked) { tryShowLockToast("Description") },
                    enabled = !sDesc.isLocked,
                    trailingIcon = {
                        if (sDesc.isLocked) {
                            IconButton(onClick = { tryShowLockToast("Description") }) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked field symbol")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Field 3: Price
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { if (!sPrice.isLocked) priceStr = it },
                    label = { Text("Price (PHP ₱)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_price")
                        .clickable(enabled = sPrice.isLocked) { tryShowLockToast("Price") },
                    enabled = !sPrice.isLocked,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        if (sPrice.isLocked) {
                            IconButton(onClick = { tryShowLockToast("Price") }) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked field symbol")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Field 4: Barcode
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { if (!sBar.isLocked) barcode = it },
                    label = { Text("Barcode") },
                    placeholder = { Text("e.g., UPC standard sequence") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_barcode")
                        .clickable(enabled = sBar.isLocked) { tryShowLockToast("Barcode") },
                    enabled = !sBar.isLocked,
                    trailingIcon = {
                        if (sBar.isLocked) {
                            IconButton(onClick = { tryShowLockToast("Barcode") }) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked field symbol")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Field 5: Quantity
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { if (!sQty.isLocked) quantityStr = it },
                    label = { Text("Quantity") },
                    placeholder = { Text("0") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_quantity")
                        .clickable(enabled = sQty.isLocked) { tryShowLockToast("Quantity") },
                    enabled = !sQty.isLocked,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        if (sQty.isLocked) {
                            IconButton(onClick = { tryShowLockToast("Quantity") }) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked field symbol")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                        val finalQty = quantityStr.toIntOrNull() ?: 0
                        
                        viewModel.saveItem(
                            id = itemId ?: 0,
                            numbering = numbering,
                            description = description,
                            price = finalPrice,
                            barcode = barcode,
                            quantity = finalQty,
                            imageUri = localImageUri
                        )
                        Toast.makeText(context, "Item Saved Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_item_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save diskette symbol", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// COLUMN STYLING & CUSTOMIZATION SCREEN
// ----------------------------------------------------------------------------------
@Composable
fun ColumnStylingScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val settingsList by viewModel.columnSettings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configure Column Styles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBack) {
                    Text("Done")
                }
            }
            Text(
                text = "Show or hide catalog columns, toggle secure locks, adjust colors, and set font point sizes dynamically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(settingsList) { setting ->
            val prettyName = setting.columnName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("styling_card_${setting.columnName}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title and Visibility Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(setting.colorHex.toComposeColor())
                            )
                            Text(
                                text = prettyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (setting.isChecked) "Shown" else "Hidden",
                                fontSize = 12.sp,
                                color = if (setting.isChecked) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = setting.isChecked,
                                onCheckedChange = { viewModel.updateColumnVisibility(setting.columnName, it) },
                                modifier = Modifier.scale(0.85f).testTag("switch_${setting.columnName}")
                            )
                        }
                    }

                    // Column styling settings controls (Visible only when checked/shown)
                    AnimatedVisibility(
                        visible = setting.isChecked,
                        enter = fadeIn(animationSpec = tween(150)),
                        exit = fadeOut(animationSpec = tween(150))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Lock/Unlock Control
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = if (setting.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Lock indicator style",
                                        tint = if (setting.isLocked) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Secure Field Edit Lock", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (setting.isLocked) "Locked" else "Unlocked",
                                        fontSize = 11.sp,
                                        color = if (setting.isLocked) MaterialTheme.colorScheme.error else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateColumnLock(setting.columnName, !setting.isLocked) }
                                    ) {
                                        Icon(
                                            imageVector = if (setting.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Toggle secure lock",
                                            tint = if (setting.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Dynamic Font Point Slider (6pt to 40pt)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Adjust Font Size", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${setting.fontSizePt} pt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = setting.fontSizePt.toFloat(),
                                    onValueChange = { size ->
                                        // Snap to nearest integer font size from the catalog list
                                        val rounded = size.toInt()
                                        if (rounded in FONT_SIZES) {
                                            viewModel.updateColumnFontSize(setting.columnName, rounded)
                                        } else {
                                            // Find closest point
                                            val closest = FONT_SIZES.minByOrNull { Math.abs(it - rounded) } ?: 14
                                            viewModel.updateColumnFontSize(setting.columnName, closest)
                                        }
                                    },
                                    valueRange = 6f..40f,
                                    steps = 17, // Fits 18 intervals between 6 and 40 step size 2
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Color Selector Palette Row
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Adjust Column Highlight Color", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(COLUMN_COLORS) { hexColor ->
                                        val color = hexColor.toComposeColor()
                                        val isSelected = setting.colorHex.lowercase() == hexColor.lowercase()

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.updateColumnColor(setting.columnName, hexColor) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Active color indicator",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// CLOUD SYNC DASHBOARD SCREEN
// ----------------------------------------------------------------------------------
@Composable
fun CloudSyncDashboardScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val cloudLogs by viewModel.cloudBackupLogs.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cloud Synchronization",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBack) {
                    Text("Done")
                }
            }
        }

        // Live connection indicator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (viewModel.isCloudSyncEnabled) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = "Cloud state big logo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (viewModel.isCloudSyncEnabled) "Cloud Sync Connected" else "Cloud Sync Suspended",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Last Synced: ${viewModel.lastSyncTime}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Account / Email configurations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Configuration Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = viewModel.cloudAccountEmail,
                        onValueChange = { viewModel.cloudAccountEmail = it },
                        label = { Text("Active Google Drive Sync Account") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = "User email logo") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Auto Cloud synchronization", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Sync exports to Drive on save", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = viewModel.isCloudSyncEnabled,
                            onCheckedChange = { viewModel.isCloudSyncEnabled = it },
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }
        }

        // Active trigger synchronization row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.triggerForceSync() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = "Force backup trigger icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup DB now")
                }

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset triggers icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Sync History")
                }
            }
        }

        // File sync history logs
        item {
            Text(
                text = "File Synchronization Ledger",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (cloudLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No backup logs captured yet. Press 'Export' in catalog view to trigger synchronization.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(cloudLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (log.fileType.lowercase()) {
                                            "excel" -> Color(0xFFE8F5E9)
                                            "csv" -> Color(0xFFE1F5FE)
                                            else -> Color(0xFFFFEBEE)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.fileType.lowercase()) {
                                        "excel" -> Icons.Default.Description
                                        "csv" -> Icons.Default.Analytics
                                        else -> Icons.Default.PictureAsPdf
                                    },
                                    contentDescription = "Format type thumbnail representation",
                                    tint = when (log.fileType.lowercase()) {
                                        "excel" -> Color(0xFF2E7D32)
                                        "csv" -> Color(0xFF1E88E5)
                                        else -> Color(0xFFC2185B)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(log.fileName, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(180.dp))
                                Text("${log.timestamp} • ${log.fileSize}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // State label badge
                        Badge(
                            containerColor = when (log.syncStatus.lowercase()) {
                                "synced" -> Color(0xFFE8F5E9)
                                "syncing" -> Color(0xFFFFF3E0)
                                else -> Color(0xFFECEFF1)
                            },
                            contentColor = when (log.syncStatus.lowercase()) {
                                "synced" -> Color(0xFF2E7D32)
                                "syncing" -> Color(0xFFE65100)
                                else -> Color(0xFF37474F)
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(4.dp)) {
                                if (log.syncStatus.lowercase() == "syncing") {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = Color(0xFFE65100))
                                } else {
                                    Icon(
                                        imageVector = if (log.syncStatus.lowercase() == "synced") Icons.Default.Done else Icons.Default.Schedule,
                                        contentDescription = "Sync micro symbol",
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Text(log.syncStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Clear Synchronization History") },
            text = { Text("Are you sure you want to clear the logs of synced files? The actual files on your storage remain untouched.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSyncHistory()
                        showResetDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------------
// MODAL SELECTION POPUPS FOR SAVE/IMPORT
// ----------------------------------------------------------------------------------
@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onExportSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudSync, contentDescription = "Sync export icon", tint = MaterialTheme.colorScheme.primary)
                    Text("Export & Auto-Sync Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    text = "Select your preferred format below. Your catalog will export, trigger a sharing chooser, and automatically synchronize with your cloud vault.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Option 1: Excel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportSelect("excel") }
                            .testTag("export_excel"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Excel spreadsheet logo", tint = Color(0xFF2E7D32))
                            Column {
                                Text("Excel Spreadsheet (.xls)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Preserves styled cells, headers, and coloring.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Option 2: CSV
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportSelect("csv") }
                            .testTag("export_csv"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = "CSV document logo", tint = Color(0xFF1E88E5))
                            Column {
                                Text("CSV File (.csv)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Universal data structure compatible with all systems.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Option 3: PDF / Printable HTML
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportSelect("pdf") }
                            .testTag("export_pdf"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF print logo", tint = Color(0xFFC2185B))
                            Column {
                                Text("PDF Catalog Report (.html/pdf)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Beautifully stylized report printable to PDF.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportFormatDialog(
    onDismiss: () -> Unit,
    onImportTextSubmit: (String) -> Unit,
    onDemoImport: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var tabIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import arrow icon", tint = MaterialTheme.colorScheme.primary)
                    Text("Import Ledger Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Paste CSV", fontSize = 12.sp) })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Load Samples", fontSize = 12.sp) })
                }

                if (tabIndex == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste your CSV list formatted as below (first line headers can be omitted):", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = "Numbering,Description,Price,Barcode,Quantity\nSKU-501,Item Name,999.5,48003312,12",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                                .fillMaxWidth(),
                            fontSize = 10.sp
                        )

                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            placeholder = { Text("Paste here...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("import_text_field"),
                            maxLines = 10,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = { onImportTextSubmit(rawText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_submit"),
                            enabled = rawText.trim().isNotEmpty()
                        ) {
                            Text("Import Text Ledger")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Don't have custom data? Load a professionally curated mini product stock inventory with 5 pre-configured products, pricing, and quantities.", fontSize = 12.sp, textAlign = TextAlign.Center, color = Color.Gray)
                        
                        Button(
                            onClick = onDemoImport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_demo_submit")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run demo load", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Populate Professional Demo Catalog")
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}


