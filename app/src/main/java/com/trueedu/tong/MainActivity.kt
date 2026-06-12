package com.trueedu.tong

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.trueedu.tong.ui.main.MainNavigation
import com.trueedu.tong.ui.main.MainScreen
import com.trueedu.tong.ui.theme.TrueSuperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val vm by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            TrueSuperTheme {
                MainScreen { navController, innerPadding ->
                    MainNavigation(navController = navController, innerPadding = innerPadding)
                }
            }
        }
    }
}
