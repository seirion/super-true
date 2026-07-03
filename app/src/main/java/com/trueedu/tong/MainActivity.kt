package com.trueedu.tong

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 저장된 keepScreenOn 설정 적용
        if (vm.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        enableEdgeToEdge()

        setContent {
            TrueSuperTheme {
                MainScreen { backStack, innerPadding ->
                    MainNavigation(backStack = backStack, innerPadding = innerPadding)
                }
            }
        }
    }
}
