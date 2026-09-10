package com.trueedu.tong.repository.remote

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.dto.order.ScheduleOrderItem
import com.trueedu.tong.model.dto.order.ScheduleOrderRequest
import com.trueedu.tong.repository.remote.kis.KisScheduleOrderRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 예약주문 진입점.
 *
 * 현재 예약주문 API 는 KIS 만 확인돼 있어 나머지 증권사는 미지원으로 반환한다.
 */
@Singleton
class ScheduleOrderUseCase @Inject constructor(
    private val kisRepo: KisScheduleOrderRepository,
) {
    fun isSupported(brokerType: BrokerType) = brokerType == BrokerType.KIS

    suspend fun getList(account: BrokerAccount): Result<List<ScheduleOrderItem>> =
        if (isSupported(account.brokerType)) kisRepo.getScheduleOrders(account)
        else unsupported(account)

    suspend fun place(account: BrokerAccount, request: ScheduleOrderRequest): Result<String> =
        if (isSupported(account.brokerType)) kisRepo.placeScheduleOrder(account, request)
        else unsupported(account)

    suspend fun cancel(account: BrokerAccount, seq: String): Result<Unit> =
        if (isSupported(account.brokerType)) kisRepo.cancelScheduleOrder(account, seq)
        else unsupported(account)

    suspend fun modify(
        account: BrokerAccount,
        seq: String,
        request: ScheduleOrderRequest,
    ): Result<Unit> =
        if (isSupported(account.brokerType)) kisRepo.modifyScheduleOrder(account, seq, request)
        else unsupported(account)

    private fun <T> unsupported(account: BrokerAccount): Result<T> = Result.failure(
        UnsupportedOperationException("${account.brokerType.displayName}은 예약주문을 지원하지 않습니다")
    )
}
