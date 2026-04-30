package com.qualitytool.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.qualitytool.app.ui.TaskViewModel
import com.qualitytool.app.ui.screens.TaskListScreen
import com.qualitytool.app.ui.theme.QualityToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        setContent {
            QualityToolTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TaskListScreen(viewModel)
                }
            }
        }
    }
}