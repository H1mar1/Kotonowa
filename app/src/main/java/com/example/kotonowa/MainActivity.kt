package com.example.kotonowa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.kotonowa.presentation.navigation.KotonowaNavHost
import com.example.kotonowa.ui.theme.KotonowaTheme
import dagger.hilt.android.AndroidEntryPoint

/** `@AndroidEntryPoint` を付けると、この Activity 配下で Hilt が材料を配れるようになる。 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotonowaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KotonowaNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
