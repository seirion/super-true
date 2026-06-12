package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import com.trueedu.tong.repository.local.CredentialStorage
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisAccountRepository @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
) {
    private val service: KisAccountService by lazy {
        retrofit.create(KisAccountService::class.java)
    }

    /**
     * 계좌 잔고 조회
     * @param account Room DB BrokerAccount
     * @param accessToken 발급된 Bearer 토큰
     */
    suspend fun getAccountSummary(
        account: BrokerAccount,
        accessToken: String,
    ): Result<AccountSummary> = runCatching {
        val headers = mapOf(
            "authorization" to "Bearer $accessToken",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "TTTC8434R",  // 실전투자 잔고조회
            "custtype" to "P",
        )
        val queries = mapOf(
            "CANO" to account.accountNum.take(8),
            "ACNT_PRDT_CD" to account.accountNum.drop(8),
            "AFHR_FLPR_YN" to "N",
            "OFL_YN" to "",
            "INQR_DVSN" to "02",
            "UNPR_DVSN" to "01",
            "FUND_STTL_ICLD_YN" to "N",
            "FNCG_AMT_AUTO_RDPT_YN" to "N",
            "PRCS_DVSN" to "01",
            "CTX_AREA_FK100" to "",
            "CTX_AREA_NK100" to "",
        )

        val response = service.getBalance(headers, queries)
        val body = response.body() ?: error("KIS 잔고조회 응답 없음")
        if (body.rtCd != "0") error("KIS API 오류: ${body.msg1}")

        val detail = body.summary.firstOrNull() ?: error("KIS 계좌 상세 없음")
        val holdings = body.holdings
            .filter { it.holdingQty.toDoubleOrNull()?.let { q -> q > 0 } == true }
            .map { h ->
                HoldingStock(
                    code = h.code,
                    name = h.name,
                    quantity = h.holdingQty.toLongOrNull() ?: 0L,
                    avgPrice = h.avgPrice.toDoubleOrNull() ?: 0.0,
                    currentPrice = h.currentPrice.toDoubleOrNull(),
                    evaluationAmount = h.evaluationAmount.toDoubleOrNull() ?: 0.0,
                    profitAmount = h.profitLossAmount.toDoubleOrNull() ?: 0.0,
                    profitRate = h.profitLossRate.toDoubleOrNull() ?: 0.0,
                )
            }

        val purchaseTotal = detail.purchaseTotalAmount.toDoubleOrNull() ?: 0.0
        val evalTotal = detail.evaluationTotalAmount.toDoubleOrNull() ?: 0.0
        val profitRate = if (purchaseTotal > 0) (evalTotal - purchaseTotal) / purchaseTotal * 100 else 0.0

        AccountSummary(
            accountId = account.id,
            totalAsset = detail.totalAsset.toDoubleOrNull() ?: 0.0,
            deposit = detail.deposit.toDoubleOrNull() ?: 0.0,
            depositD1 = detail.depositD1.toDoubleOrNull(),
            depositD2 = detail.depositD2.toDoubleOrNull(),
            totalProfitAmount = detail.profitLossTotalAmount.toDoubleOrNull() ?: 0.0,
            totalProfitRate = profitRate,
            holdings = holdings,
        )
    }
}
