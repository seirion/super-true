package com.trueedu.tong.model

/**
 * 종목 상세 정보 (증권사 공통 모델).
 *
 * 각 증권사 API 응답을 이 공통 모델로 변환한다.
 * 제공되지 않는 값은 null 로 두며, UI 에서는 null 값을 숨긴다.
 *
 * 주의: dto 패키지의 StockInfo(코스피/코스닥 기준정보 파싱용)와는 다른 클래스다.
 */
data class StockInfo(
    val code: String,
    val name: String,
    val currentPrice: Double,
    val delta: Double,       // 전일대비
    val rate: Double,        // 등락률 %
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val marketCap: Double?,  // 시가총액 (억원)
    val per: Double?,
    val pbr: Double?,
    val eps: Double?,
    val bps: Double?,
    val roe: Double?,
    val ev: Double?,
    val salesAmount: Double?,     // 매출액 (억원)
    val operatingProfit: Double?, // 영업이익 (억원)
    val netProfit: Double?,       // 당기순이익 (억원)
    val parValue: Double?,        // 액면가
    val capital: Double?,         // 자본금 (억원)
    val listedShares: Long?,      // 상장주식수
    val settlementMonth: String?, // 결산월
    val week52High: Double?,
    val week52Low: Double?,
    val upperLimit: Double?,      // 상한가
    val lowerLimit: Double?,      // 하한가
    val foreignExhaustionRate: Double?, // 외인소진률
    val creditRate: Double?,      // 신용비율
)
