package com.trueedu.tong.repository.remote

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.repository.remote.kis.KisOrderRepository
import com.trueedu.tong.repository.remote.kiwoom.KiwoomOrderRepository
import com.trueedu.tong.repository.remote.ls.LsOrderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderUseCase @Inject constructor(
    private val kisRepo: KisOrderRepository,
    private val kiwoomRepo: KiwoomOrderRepository,
    private val lsRepo: LsOrderRepository,
) {
    suspend fun placeOrder(account: BrokerAccount, request: OrderRequest): Result<OrderResult> =
        when (account.brokerType) {
            BrokerType.KIS    -> kisRepo.placeOrder(account, request)
            BrokerType.KIWOOM -> kiwoomRepo.placeOrder(account, request)
            BrokerType.LS     -> lsRepo.placeOrder(account, request)
            BrokerType.TOSS   -> Result.failure(UnsupportedOperationException("토스증권 미지원"))
        }
}
