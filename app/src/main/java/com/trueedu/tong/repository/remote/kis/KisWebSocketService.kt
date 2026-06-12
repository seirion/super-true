package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.di.KisWsOkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisWebSocketService @Inject constructor(
    @KisWsOkHttp private val okHttpClient: OkHttpClient,
) {
    private val wsUrl = "ws://ops.koreainvestment.com:21000"
    private var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        Timber.d("KisWebSocketService: connect")
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun send(message: String) {
        Timber.d("KisWebSocketService: send $message")
        webSocket?.send(message)
    }

    fun disconnect() {
        Timber.d("KisWebSocketService: disconnect")
        webSocket?.cancel()
        webSocket = null
    }
}
