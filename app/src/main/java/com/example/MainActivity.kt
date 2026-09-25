package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.InventoryRepository
import com.example.ui.InventoryApp
import com.example.ui.InventoryViewModel
import com.example.ui.InventoryViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get database and repository instance
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = InventoryRepository(database.inventoryDao())

        // Instantiate ViewModel with Custom Factory
        val factory = InventoryViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[InventoryViewModel::class.java]

        setContent {
            MyApplicationTheme {
                InventoryApp(viewModel = viewModel)
            }
        }
    }
}
