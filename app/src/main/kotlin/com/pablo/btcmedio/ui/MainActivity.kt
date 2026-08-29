package com.pablo.btcmedio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.pablo.btcmedio.ui.theme.BtcMedioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BtcMedioTheme {
                Surface { Text("Preço Médio BTC") }
            }
        }
    }
}
