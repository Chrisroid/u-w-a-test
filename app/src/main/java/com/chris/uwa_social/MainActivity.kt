package com.chris.uwa_social

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chris.uwa_social.presentation.feed.FeedScreen
import com.chris.uwa_social.ui.theme.Uwa_socialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Uwa_socialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FeedScreen()
                }
            }
        }
    }
}