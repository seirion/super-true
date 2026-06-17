package com.trueedu.tong.model.ws

/**
 * KIS 실시간 체결 데이터 ("|" 구분, 인덱스 3 이후 "^" 구분)
 *
 * H0STCNT0(실시간 체결) 응답 필드:
 * [0]종목코드 [1]체결시간 [2]현재가 [3]전일대비부호 [4]전일대비 [5]등락율
 * [7]시가 [8]고가 [9]저가 [13]누적거래량
 *
 * H0STEXP0(동시호가 예상체결) 응답 필드:
 * [0]종목코드 [1]예상체결시간 [2]예상체결가격 [3]전일대비부호 [4]전일대비 [5]등락율
 * [6]예상체결수량 [7]예상체결량(누적)
 *
 * 인덱스 [0]~[5]는 두 TR이 동일하므로 공유하고, 시가/고가/저가는 예상체결에 없어
 * nullable(0.0 fallback) 처리한다. [isExpected]로 예상체결 여부를 구분한다.
 */
class KisRealTimeTrade(
    val data: List<String>,
    val isExpected: Boolean = false,
) {
    companion object {
        fun from(rawData: String) = KisRealTimeTrade(rawData.split("^"))

        /** 동시호가 예상체결(H0STEXP0) 파싱 */
        fun fromExpected(rawData: String) =
            KisRealTimeTrade(rawData.split("^"), isExpected = true)
    }
    val code = data[0]
    val time = data.getOrNull(1) ?: ""         // HHmmss (체결시간 / 예상체결시간)
    val price = data.getOrNull(2)?.toDoubleOrNull() ?: 0.0   // 현재가 / 예상체결가격
    val delta = data.getOrNull(4)?.toDoubleOrNull() ?: 0.0   // 전일대비
    val rate = data.getOrNull(5)?.toDoubleOrNull() ?: 0.0    // 등락율
    // 시가/고가/저가는 예상체결(H0STEXP0)에는 없음 → 0.0
    val open = if (isExpected) 0.0 else data.getOrNull(7)?.toDoubleOrNull() ?: 0.0   // 시가
    val high = if (isExpected) 0.0 else data.getOrNull(8)?.toDoubleOrNull() ?: 0.0   // 고가
    val low = if (isExpected) 0.0 else data.getOrNull(9)?.toDoubleOrNull() ?: 0.0    // 저가
    // 누적거래량: 실시간 체결은 [13], 예상체결은 [7](예상체결 누적량)
    val volume = if (isExpected) {
        data.getOrNull(7)?.toDoubleOrNull() ?: 0.0
    } else {
        data.getOrNull(13)?.toDoubleOrNull() ?: 0.0
    }
}
