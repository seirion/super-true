package com.trueedu.tong.model.ws

/**
 * KIS 실시간 체결 데이터 ("|" 구분, 인덱스 3 이후 "^" 구분)
 * H0STCNT0 응답 필드:
 * [0]종목코드 [1]체결시간 [2]현재가 [3]전일대비부호 [4]전일대비 [5]등락율
 * [7]시가 [8]고가 [9]저가 [13]누적거래량
 */
class KisRealTimeTrade(val data: List<String>) {
    companion object {
        fun from(rawData: String) = KisRealTimeTrade(rawData.split("^"))
    }
    val code = data[0]
    val time = data.getOrNull(1) ?: ""         // HHmmss (체결시간)
    val price = data[2].toDoubleOrNull() ?: 0.0
    val delta = data[4].toDoubleOrNull() ?: 0.0
    val rate = data[5].toDoubleOrNull() ?: 0.0
    val open = data.getOrNull(7)?.toDoubleOrNull() ?: 0.0   // 시가
    val high = data.getOrNull(8)?.toDoubleOrNull() ?: 0.0   // 고가
    val low = data.getOrNull(9)?.toDoubleOrNull() ?: 0.0    // 저가
    val volume = data.getOrNull(13)?.toDoubleOrNull() ?: 0.0 // 누적거래량
}
