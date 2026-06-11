package com.trueedu.tong.analytics

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

val LocalTrueAnalytics = staticCompositionLocalOf<TrueAnalytics> {
    error("No TrueAnalytics provided")
}

/**
 * 애널리틱스 파사드 (stub).
 * 현재는 Timber 로그로만 동작하며, 추후 Amplitude/Firebase 등을 연결한다.
 */
@Singleton
class TrueAnalytics @Inject constructor(application: Application) {

    fun setUserId(userId: String) {
        Timber.d("setUserId: $userId")
    }

    fun setUserProperties(properties: Map<String, Any>) {
        Timber.d("setUserProperties: $properties")
    }

    fun clickButton(buttonName: String, params: Map<String, Any> = emptyMap()) {
        log(buttonName, params)
    }

    fun enterView(event: String, params: Map<String, Any> = emptyMap()) {
        log(event, params)
    }

    fun log(event: String, params: Map<String, Any> = emptyMap()) {
        Timber.d("event: $event, params: $params")
    }

    fun shutdown() {
        Timber.d("shutdown")
    }
}
