package com.trueedu.tong.model.ws

import kotlin.math.abs

/**
 * KIS 해외주식 실시간 체결 (HDFSCNT0, "^" 구분)
 *
 * [0]RSYM [1]SYMB [2]ZDIV [3]TYMD [4]XYMD [5]XHMS [6]KYMD [7]KHMS
 * [8]OPEN [9]HIGH [10]LOW [11]LAST [12]SIGN [13]DIFF [14]RATE ... [26]MTYP
 * SIGN: 1 상한 / 2 상승 / 3 보합 / 4 하한 / 5 하락. 미국은 0분 지연 무료 시세(문서 기준).
 */
data class KisOverseasTrade(
    val symbol: String,
    val price: Double,
    val delta: Double,   // 전일대비 (부호 반영)
    val rate: Double,    // 등락률 % (부호 반영)
    val localTime: String,  // 현지시간 HHmmss
    val koreaTime: String,  // 한국시간 HHmmss
    val receivedAtMs: Long = System.currentTimeMillis(),
) {
    companion object {
        /** 체결 데이터 본문(`0|HDFSCNT0|001|` 뒤)을 파싱한다. 가격이 없으면 null. */
        fun from(raw: String, receivedAtMs: Long = System.currentTimeMillis()): KisOverseasTrade? {
            val f = raw.split("^")
            val symbol = f.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
            val price = f.getOrNull(11)?.toDoubleOrNull() ?: return null
            // 부호 표기가 값에 이미 들어 있어도 중복 적용되지 않도록 절대값에 SIGN 을 적용
            val negative = f.getOrNull(12) in setOf("4", "5")
            val flat = f.getOrNull(12) == "3"
            fun signed(v: Double?): Double {
                val a = abs(v ?: 0.0)
                return if (flat) 0.0 else if (negative) -a else a
            }
            return KisOverseasTrade(
                symbol = symbol,
                price = price,
                delta = signed(f.getOrNull(13)?.toDoubleOrNull()),
                rate = signed(f.getOrNull(14)?.toDoubleOrNull()),
                localTime = f.getOrNull(5) ?: "",
                koreaTime = f.getOrNull(7) ?: "",
                receivedAtMs = receivedAtMs,
            )
        }
    }
}
