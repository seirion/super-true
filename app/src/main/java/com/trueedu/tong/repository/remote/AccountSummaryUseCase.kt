package com.trueedu.tong.repository.remote

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.repository.remote.kis.KisAccountRepository
import com.trueedu.tong.repository.remote.kiwoom.KiwoomAccountRepository
import com.trueedu.tong.repository.remote.ls.LsAccountRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모든 증권사 계좌 자산 조회 진입점.
 * 계좌 타입에 따라 적절한 Repository로 위임한다.
 *
 * 토큰 발급/갱신은 호출자 책임이며, 발급된 Bearer 토큰을 전달받는다.
 */
@Singleton
class AccountSummaryUseCase @Inject constructor(
    private val kisRepo: KisAccountRepository,
    private val kiwoomRepo: KiwoomAccountRepository,
    private val lsRepo: LsAccountRepository,
) {
    suspend fun fetch(
        account: BrokerAccount,
        accessToken: String,
    ): Result<AccountSummary> = when (account.brokerType) {
        BrokerType.KIS    -> kisRepo.getAccountSummary(account, accessToken)
        BrokerType.KIWOOM -> kiwoomRepo.getAccountSummary(account, accessToken)
        BrokerType.LS     -> lsRepo.getAccountSummary(account, accessToken)
        BrokerType.TOSS   -> Result.failure(UnsupportedOperationException("토스증권 미지원"))
    }
}
