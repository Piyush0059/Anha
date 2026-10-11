package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.TranslationRepository
import com.example.ui.view.AnhaApp
import com.example.ui.viewmodel.TranslationViewModel
import com.example.ui.viewmodel.TranslationViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database, DAO, Repository and ViewModel securely
        val database = AppDatabase.getDatabase(this)
        val repository = TranslationRepository(database.appDao(), applicationContext)
        
        val viewModel = ViewModelProvider(
            this,
            TranslationViewModelFactory(repository)
        )[TranslationViewModel::class.java]

        setContent {
            AnhaApp(viewModel)
        }
    }
}
