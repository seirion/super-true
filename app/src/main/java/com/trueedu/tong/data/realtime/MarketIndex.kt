package com.trueedu.tong.data.realtime

/**
 * 코스피/코스닥 등 업종지수 실시간 데이터.
 *
 * [code] 는 매니저별 원본 종목코드를 그대로 담을 수 있으나,
 * [MarketIndexManager] 를 통해 노출될 때는 KIS 기준("0001"/"1001")으로 정규화된다.
 */
data class MarketIndex(
    val code: String,    // "0001"(코스피) / "1001"(코스닥) — KIS 기준 정규화 코드
    val price: Double,    // 현재지수
    val delta: Double,    // 전일대비
    val rate: Double,     // 등락률(%)
) {
    companion object {
        // KIS 종목코드
        const val KIS_KOSPI = "0001"
        const val KIS_KOSDAQ = "1001"

        // 키움 종목코드
        const val KIWOOM_KOSPI = "001"
        const val KIWOOM_KOSDAQ = "101"

        /** 임의 코드를 KIS 기준 코스피 코드("0001")로 정규화. 코스피가 아니면 null */
        fun isKospi(code: String): Boolean = code == KIS_KOSPI || code == KIWOOM_KOSPI
        fun isKosdaq(code: String): Boolean = code == KIS_KOSDAQ || code == KIWOOM_KOSDAQ

        /**
         * KIS 업종현재가(H0UPCNT0 / H0NXUPC0) 데이터 1건 파싱.
         * 원본 메시지 "0|H0UPCNT0|001|데이터" 에서 데이터 부분("^" 구분)을 받는다.
         * [0]업종코드 [2]현재지수 [4]전일대비 [5]등락률 [10]누적거래량
         */
        fun fromKis(rawData: String): MarketIndex {
            val f = rawData.split("^")
            return MarketIndex(
                code = f.getOrNull(0) ?: "",
                price = f.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                delta = f.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                rate = f.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
            )
        }

        /**
         * 키움 업종현재가(OPK20001) 실시간 데이터 1건 파싱.
         * 스펙상 "|" 구분 데이터를 받는다.
         * [0]업종코드 [2]현재지수 [4]전일대비 [5]등락률
         */
        fun fromKiwoom(rawData: String): MarketIndex {
            val f = rawData.split("|")
            return MarketIndex(
                code = f.getOrNull(0) ?: "",
                price = f.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                delta = f.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                rate = f.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
            )
        }
    }
}
