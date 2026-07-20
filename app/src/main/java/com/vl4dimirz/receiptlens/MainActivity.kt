package com.vl4dimirz.receiptlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.vl4dimirz.receiptlens.ui.ScanScreen
import com.vl4dimirz.receiptlens.ui.theme.ReceiptLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReceiptLensTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScanScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
