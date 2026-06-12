package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.di.KisWsOkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logI
import com.trueedu.tong.utils.logW
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisWebSocketService @Inject constructor(
    @KisWsOkHttp private val okHttpClient: OkHttpClient,
) {
    private val wsUrl = "ws://ops.koreainvestment.com:21000"
    private var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        logD("KisWebSocketService: connect")
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun send(message: String) {
        logD("KisWebSocketService: send $message")
        webSocket?.send(message)
    }

    fun disconnect() {
        logD("KisWebSocketService: disconnect")
        webSocket?.cancel()
        webSocket = null
    }
}
