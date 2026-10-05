package com.trueedu.tong.model.account

/**
 * 모든 증권사 계좌 자산 정보를 통합하는 공통 모델
 *
 * @param accountId Room DB의 BrokerAccount.id
 * @param totalAsset 총 자산 (주식 평가금액 + 예수금)
 * @param deposit 예수금 (D+0)
 * @param depositD1 D+1 예수금 (null이면 미지원)
 * @param depositD2 D+2 예수금 (null이면 미지원)
 * @param totalProfitAmount 평가손익 합계 금액
 * @param totalProfitRate 평가손익 수익률 (%)
 * @param holdings 보유 종목 목록
 * @param buyingPowerKrw 원화 매수 가능 금액 (토스처럼 예수금 API 가 없는 증권사. 총자산에는 더하지 않음)
 * @param buyingPowerUsd 달러 매수 가능 금액
 * @param usdKrwRate USD→KRW 환율 (USD 종목을 원화로 환산할 때 사용, null 이면 환율 미조회)
 */
data class AccountSummary(
    val accountId: Long,
    val totalAsset: Double,
    val deposit: Double,
    val depositD1: Double?,
    val depositD2: Double?,
    val totalProfitAmount: Double,
    val totalProfitRate: Double,
    val holdings: List<HoldingStock>,
    val usdKrwRate: Double? = null,
    val buyingPowerKrw: Double? = null,
    val buyingPowerUsd: Double? = null,
) {
    /** 종목 금액(거래 통화)을 원화로 환산하는 배수. 환율을 모르는 USD 종목은 0 (합계에서 제외) */
    fun krwFactor(holding: HoldingStock): Double =
        if (holding.isUsd) usdKrwRate ?: 0.0 else 1.0
}

/**
 * 보유 종목 공통 모델
 *
 * @param code 종목코드 (KIS: 6자리, 키움: 6자리, LS: expcode)
 * @param name 종목명
 * @param quantity 보유 수량 (미국 주식은 소수점 수량 가능)
 * @param avgPrice 평균 매입 단가
 * @param currentPrice 현재가 (null이면 미조회)
 * @param evaluationAmount 평가금액
 * @param profitAmount 평가손익 금액
 * @param profitRate 평가손익 수익률 (%)
 * @param currency 거래 통화 (KRW/USD). 가격·금액 필드는 이 통화 기준
 * @param prevClose 전일 종가 (거래 통화, 증권사가 제공하는 경우만). 실시간 체결가로 일간 등락을 계산할 때 사용
 */
data class HoldingStock(
    val code: String,
    val name: String,
    val quantity: Double,
    val avgPrice: Double,
    val currentPrice: Double?,
    val evaluationAmount: Double,
    val profitAmount: Double,
    val profitRate: Double,
    val currency: String = "KRW",
    val prevClose: Double? = null,
) {
    val isUsd: Boolean get() = currency == "USD"

    /**
     * 실시간 시세 맵을 조회하는 키.
     * 국내 코드는 키움의 "A" 접두사를 떼지만, 미국 티커는 그대로 쓴다
     * (티커가 A 로 시작하면 "AAPL" → "APL" 처럼 잘못 잘린다).
     */
    val quoteKey: String get() = if (isUsd) code else code.removePrefix("A")

    /** 전일 종가 대비 등락 (거래 통화). 전일 종가나 가격을 모르면 null */
    fun deltaFrom(price: Double?): Double? =
        if (prevClose != null && price != null) price - prevClose else null

    /** 전일 종가 대비 등락률(%). 전일 종가나 가격을 모르면 null */
    fun rateFrom(price: Double?): Double? =
        if (prevClose != null && prevClose > 0 && price != null) (price - prevClose) / prevClose * 100 else null
}
