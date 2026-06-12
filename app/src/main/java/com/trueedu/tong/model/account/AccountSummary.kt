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
)

/**
 * 보유 종목 공통 모델
 *
 * @param code 종목코드 (KIS: 6자리, 키움: 6자리, LS: expcode)
 * @param name 종목명
 * @param quantity 보유 수량
 * @param avgPrice 평균 매입 단가
 * @param currentPrice 현재가 (null이면 미조회)
 * @param evaluationAmount 평가금액
 * @param profitAmount 평가손익 금액
 * @param profitRate 평가손익 수익률 (%)
 */
data class HoldingStock(
    val code: String,
    val name: String,
    val quantity: Long,
    val avgPrice: Double,
    val currentPrice: Double?,
    val evaluationAmount: Double,
    val profitAmount: Double,
    val profitRate: Double,
)
