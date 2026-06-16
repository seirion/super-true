package com.trueedu.tong.data.realtime

import com.trueedu.tong.di.KiwoomWsOkHttp
import com.trueedu.tong.utils.logD
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 키움증권 실시간 WebSocket 연결 서비스.
 */
@Singleton
class KiwoomWebSocketService @Inject constructor(
    @KiwoomWsOkHttp private val okHttpClient: OkHttpClient,
) {
    private val wsUrl = "wss://openapi.kiwoom.com/api/dostk/websocket"
    private var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        logD("KiwoomWebSocketService: connect")
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun send(message: String) {
        logD("KiwoomWebSocketService: send $message")
        webSocket?.send(message)
    }

    fun disconnect() {
        logD("KiwoomWebSocketService: disconnect")
        webSocket?.cancel()
        webSocket = null
    }
}
