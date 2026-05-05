package com.dantech.dreams

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dantech.dreams.ui.playground.PlaygroundApp
import com.dantech.dreams.ui.theme.DreamsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DreamsTheme {
                PlaygroundApp()
            }
        }
    }
}
