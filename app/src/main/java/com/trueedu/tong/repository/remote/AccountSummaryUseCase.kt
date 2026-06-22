package com.trueedu.tong.repository.remote

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.repository.remote.kis.KisAccountRepository
import com.trueedu.tong.repository.remote.kiwoom.KiwoomAccountRepository
import com.trueedu.tong.repository.remote.ls.LsAccountRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모든 증권사 계좌 자산 조회 진입점.
 * 계좌 타입에 따라 적절한 Repository로 위임한다.
 *
 * 토큰 발급/캐시/갱신은 [TokenManager]가 내부에서 자동 처리한다.
 */
@Singleton
class AccountSummaryUseCase @Inject constructor(
    private val tokenManager: TokenManager,
    private val kisRepo: KisAccountRepository,
    private val kiwoomRepo: KiwoomAccountRepository,
    private val lsRepo: LsAccountRepository,
) {
    suspend fun fetch(account: BrokerAccount): Result<AccountSummary> {
        val tokenResult = tokenManager.getValidToken(account)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull()!!)
        }
        val token = tokenResult.getOrThrow()

        return when (account.brokerType) {
            BrokerType.KIS    -> kisRepo.getAccountSummary(account, token)
            BrokerType.KIWOOM -> kiwoomRepo.getAccountSummary(account)
            BrokerType.LS     -> lsRepo.getAccountSummary(account, token)
            BrokerType.TOSS   -> Result.failure(UnsupportedOperationException("토스증권 미지원"))
        }
    }
}
