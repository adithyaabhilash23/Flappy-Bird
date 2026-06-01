package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.game.GameScreen
import com.example.game.GameViewModel
import com.example.ui.theme.MyApplicationTheme

/**
 * Immersive Entry Point Activity for the Flappy Bird clone game.
 * Sets up full screen layout context and initializes the unified Compose game view.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable modern Edge-To-Edge drawing context, spanning colors fully to physical screen boundaries
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Initialize the central state coordination engine with application context
                val gameViewModel = remember {
                    GameViewModel(applicationContext)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Set up full bleed game screen layout ignores innerPadding
                    // to achieve perfect console full-screen immersion.
                    GameScreen(
                        viewModel = gameViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
