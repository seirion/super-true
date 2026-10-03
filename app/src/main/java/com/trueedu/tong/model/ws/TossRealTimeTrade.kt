package com.trueedu.tong.model.ws

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 토스 실시간 체결 (웹소켓 `trade:kr` / `trade:us` 프레임의 data)
 *
 * 프레임에는 가격/수량/시각/통화만 있고 전일대비·등락률·누적거래량은 없다.
 *
 * @param symbol 국내 6자리 코드 또는 미국 티커
 * @param price 체결가 (currency 기준)
 * @param volume 체결 수량
 * @param timestamp 서버가 내려준 체결 시각 (원문)
 * @param currency KRW / USD
 * @param receivedAtMs 앱이 프레임을 받은 시각 (지연 측정용)
 */
data class TossRealTimeTrade(
    val symbol: String,
    val price: Double,
    val volume: Double,
    val timestamp: String,
    val currency: String,
    val receivedAtMs: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * `{"type":"message","topic":"trade:us:AAPL","data":{...}}` 프레임을 파싱한다.
         * 체결 채널이 아니거나 가격이 없으면 null.
         */
        fun fromFrame(frame: JsonObject, receivedAtMs: Long = System.currentTimeMillis()): TossRealTimeTrade? {
            val topic = frame["topic"]?.jsonPrimitive?.contentOrNull ?: return null
            val parts = topic.split(":")
            if (parts.size != 3 || parts[0] != "trade") return null
            val data = frame["data"]?.jsonObject ?: return null
            val price = data["price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return null
            return TossRealTimeTrade(
                symbol = parts[2],
                price = price,
                volume = data["volume"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
                timestamp = data["timestamp"]?.jsonPrimitive?.contentOrNull ?: "",
                currency = data["currency"]?.jsonPrimitive?.contentOrNull
                    ?: if (parts[1] == "us") "USD" else "KRW",
                receivedAtMs = receivedAtMs,
            )
        }
    }
}
