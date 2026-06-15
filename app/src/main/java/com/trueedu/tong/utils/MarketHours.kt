package com.trueedu.tong.utils

import java.util.Calendar

/**
 * 국내 주식 거래소 운영 시간 유틸리티.
 *
 * KRX 정규장: 09:00 ~ 15:30
 * NXT 전용 시간: 08:00 ~ 09:00, 15:30 ~ 20:00
 * SOR 적용 범위: 정규장 시간(KRX + NXT 동시 운영) → 09:00 ~ 15:30
 */
object MarketHours {

    /**
     * KRX 정규장 시간 여부 (09:00 ~ 15:30).
     * 이 시간에는 SOR(Smart Order Routing)을 사용한다.
     */
    fun isRegularMarketHour(): Boolean {
        val cal = Calendar.getInstance()
        val totalMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return totalMinutes in 9 * 60 until 15 * 60 + 30
    }

    /**
     * NXT 단독 운영 시간 여부 (08:00 ~ 09:00, 15:30 ~ 20:00).
     */
    fun isNxtOnlyHour(): Boolean {
        val cal = Calendar.getInstance()
        val totalMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return totalMinutes in 8 * 60 until 9 * 60 ||
               totalMinutes in 15 * 60 + 30 until 20 * 60
    }

    /**
     * 키움증권 dmst_stex_tp 값 반환.
     * 정규장: SOR, NXT 시간: NXT, 그 외: SOR(주문 거부되더라도 서버 판단에 맡김)
     */
    fun kiwoomExchangeType(): String = if (isRegularMarketHour()) "SOR" else "NXT"

    /**
     * KIS tr_id 반환 (매수 기준).
     * 정규장: TTTC0802U (KRX 정규), NXT 시간: TTTC0012U (SOR/NXT 통합)
     */
    fun kisBuyTrId(): String = if (isRegularMarketHour()) "TTTC0802U" else "TTTC0012U"

    /**
     * KIS tr_id 반환 (매도 기준).
     * 정규장: TTTC0801U (KRX 정규), NXT 시간: TTTC0011U (SOR/NXT 통합)
     */
    fun kisSellTrId(): String = if (isRegularMarketHour()) "TTTC0801U" else "TTTC0011U"
}
