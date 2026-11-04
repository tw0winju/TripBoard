package com.tw0winju.tripboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tw0winju.tripboard.core.designsystem.theme.TripBoardTheme
import com.tw0winju.tripboard.navigation.TripBoardNavHost
import dagger.hilt.android.AndroidEntryPoint

/** 싱글 액티비티. 화면 전환은 Fragment 대신 Navigation Compose가 맡는다. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripBoardTheme {
                TripBoardNavHost()
            }
        }
    }
}
