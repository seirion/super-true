package com.trueedu.tong

import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.data.realtime.MarketIndexManager
import com.trueedu.tong.repository.local.Local
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.internal.Contexts
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

@HiltAndroidApp
class App : Application(), LifecycleEventObserver {
    companion object {
        private var foreground = false
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface InjectModule {
        fun getLocal(): Local
        fun getKisRealPriceManager(): KisRealPriceManager
        fun getMarketIndexManager(): MarketIndexManager
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val injector = entryPointInjector(InjectModule::class.java)
        injector.getLocal().migrate()
        kisRealPriceManager = injector.getKisRealPriceManager()
        marketIndexManager = injector.getMarketIndexManager()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private lateinit var kisRealPriceManager: KisRealPriceManager
    private lateinit var marketIndexManager: MarketIndexManager

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                foreground = true
                Timber.d("app onStart")
                // 포그라운드 복귀 시 실시간 시세 재개 (마지막 구독 종목으로)
                kisRealPriceManager.resume()
                marketIndexManager.resume()
            }

            Lifecycle.Event.ON_STOP -> {
                foreground = false
                Timber.d("app onStop")
                // 백그라운드 진입 시 WebSocket 연결 해제
                kisRealPriceManager.pause()
                marketIndexManager.pause()
            }

            else -> {}
        }
    }
}

fun <T> Context.entryPointInjector(clazz: Class<T>): T {
    return EntryPoints.get(Contexts.getApplication(applicationContext), clazz)
}
