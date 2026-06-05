package com.example.proyectofinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.proyectofinal.data.repository.GamerRepository
import com.example.proyectofinal.navigation.NavGraph
import com.example.proyectofinal.ui.theme.GamerTheme

class MainActivity : ComponentActivity() {
    lateinit var repository: GamerRepository
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = GamerRepository(applicationContext)
        setContent {
            GamerTheme {
                NavGraph(navController = rememberNavController(), repository = repository)
            }
        }
    }
}
