package com.trueedu.tong.model.ws

/**
 * KIS 실시간 체결 데이터 ("|" 구분, 인덱스 3 이후 "^" 구분)
 *
 * H0STCNT0 / H0STANC0(동시호가 예상체결 KRX) 응답 필드 — 동일한 구조:
 * [0]종목코드 [1]체결시간 [2]현재가/예상체결가 [3]전일대비부호 [4]전일대비 [5]등락율
 * [7]시가 [8]고가 [9]저가 [13]누적거래량
 */
class KisRealTimeTrade(
    val data: List<String>,
    val isExpected: Boolean = false,
) {
    companion object {
        fun from(rawData: String) = KisRealTimeTrade(rawData.split("^"))

        /** 동시호가 예상체결(H0STANC0) 파싱 — H0STCNT0와 필드 구조 동일 */
        fun fromExpected(rawData: String) =
            KisRealTimeTrade(rawData.split("^"), isExpected = true)
    }
    val code = data[0]
    val time = data.getOrNull(1) ?: ""         // HHmmss (체결시간 / 예상체결시간)
    val price = data.getOrNull(2)?.toDoubleOrNull() ?: 0.0   // 현재가 / 예상체결가격
    val delta = data.getOrNull(4)?.toDoubleOrNull() ?: 0.0   // 전일대비
    val rate = data.getOrNull(5)?.toDoubleOrNull() ?: 0.0    // 등락율
    val open = data.getOrNull(7)?.toDoubleOrNull() ?: 0.0    // 시가
    val high = data.getOrNull(8)?.toDoubleOrNull() ?: 0.0    // 고가
    val low = data.getOrNull(9)?.toDoubleOrNull() ?: 0.0     // 저가
    val volume = data.getOrNull(13)?.toDoubleOrNull() ?: 0.0 // 누적거래량 [13]
}
