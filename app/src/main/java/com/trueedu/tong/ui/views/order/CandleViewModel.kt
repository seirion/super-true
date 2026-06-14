package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.CandleData
import com.trueedu.tong.model.CandlePeriod
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.CandleRepository
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CandleViewModel @Inject constructor(
    private val candleRepo: CandleRepository,
    private val brokerAccountRepo: BrokerAccountRepository,
    private val credentialStorage: CredentialStorage,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val candles: List<CandleData>, val broker: BrokerType) : State()
        object NoAccount : State()
        data class Error(val msg: String) : State()
    }

    var state by mutableStateOf<State>(State.Idle); private set

    // 현재 선택된 기간 (기간 변경 시 재조회에 사용)
    var currentPeriod by mutableStateOf(CandlePeriod.DAY); private set

    // 마지막으로 로드한 종목코드 (중복 호출 방지)
    private var loadedCode: String? = null

    // 외부에서 현재 종목코드 접근 (기간 변경 재조회 등)
    val currentCode: String get() = loadedCode ?: ""

    // 키움 → LS → KIS 우선순위
    private val priority = listOf(BrokerType.KIWOOM, BrokerType.LS, BrokerType.KIS)

    /**
     * 캔들 데이터 로드. 우선순위대로 appKey 가 있는 계좌를 찾아 호출한다.
     * 기간이 바뀌면 강제로 재조회한다.
     */
    fun load(code: String, period: CandlePeriod = currentPeriod, force: Boolean = false) {
        val target = code.removePrefix("A")
        if (target.isBlank()) {
            state = State.Idle
            return
        }
        val periodChanged = period != currentPeriod
        currentPeriod = period
        val mustReload = force || periodChanged
        if (!mustReload && target == loadedCode && state is State.Success) return
        loadedCode = target

        viewModelScope.launch {
            state = State.Loading
            val accounts = brokerAccountRepo.getAll().first()
                .filter { it.isActive && credentialStorage.getAppKey(it.id).isNotBlank() }

            if (accounts.isEmpty()) {
                state = State.NoAccount
                return@launch
            }

            var lastError: String? = null
            for (broker in priority) {
                val account = accounts.firstOrNull { it.brokerType == broker } ?: continue
                logD("CandleViewModel: $target 조회 시도 - ${broker.displayName}")
                val result = fetch(broker, account, target, currentPeriod)
                result
                    .onSuccess {
                        logD("CandleViewModel: ${broker.displayName} 성공 - ${it.size}개 캔들")
                        state = State.Success(it, broker)
                        return@launch
                    }
                    .onFailure {
                        lastError = it.message
                        logW("CandleViewModel: ${broker.displayName} 실패 - ${it.message}")
                    }
            }
            state = State.Error(lastError ?: "캔들 데이터를 가져오지 못했습니다")
        }
    }

    /**
     * 추가(과거) 데이터 로드. 차트 스크롤이 끝에 도달했을 때 호출된다.
     *
     * TODO: 실제 연속조회는 증권사별 연속조회 키(키움 next-key, LS tr_cont_key,
     *  KIS cts_date 등) 관리가 필요하다. 현재는 중복 호출만 방지하는 stub.
     */
    fun loadMore() {
        if (state is State.Loading) return
        // 연속조회 미구현: 추가 데이터 없음
        logD("CandleViewModel: loadMore 호출 (연속조회 미구현)")
    }

    private suspend fun fetch(
        broker: BrokerType,
        account: BrokerAccount,
        code: String,
        period: CandlePeriod,
    ): Result<List<CandleData>> = when (broker) {
        BrokerType.KIWOOM -> candleRepo.fetchKiwoom(account, code, period)
        BrokerType.LS -> candleRepo.fetchLs(account, code, period)
        BrokerType.KIS -> candleRepo.fetchKis(account, code, period)
        BrokerType.TOSS -> Result.failure(UnsupportedOperationException("토스증권 미지원"))
    }
}
