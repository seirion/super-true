package com.trueedu.tong.utils

import java.time.LocalTime
import java.time.ZoneId

private val seoulZone = ZoneId.of("Asia/Seoul")

/** KRX 애프터마켓(16:00~20:00)은 정규장과 주문구분 코드가 다르다 */
fun isKrxAfterMarket(): Boolean {
    val now = LocalTime.now(seoulZone)
    return now >= LocalTime.of(16, 0) && now < LocalTime.of(20, 0)
}

/** NXT 와 KRX 애프터마켓은 시장가 주문구분이 없다 */
fun isMarketOrderAllowed(exchangeId: String): Boolean =
    exchangeId != "NXT" && !(exchangeId == "KRX" && isKrxAfterMarket())

/** KIS 주문구분: 지정가 정규장 00 / KRX 애프터마켓 41, 시장가 01 */
fun kisOrdDvsn(exchangeId: String, isMarket: Boolean): String = when {
    isMarket -> "01"
    exchangeId == "KRX" && isKrxAfterMarket() -> "41"
    else -> "00"
}
