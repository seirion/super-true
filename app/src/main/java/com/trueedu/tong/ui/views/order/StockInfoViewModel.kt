package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.StockInfo
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.StockInfoRepository
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockInfoViewModel @Inject constructor(
    private val stockInfoRepo: StockInfoRepository,
    private val brokerAccountRepo: BrokerAccountRepository,
    private val credentialStorage: CredentialStorage,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val info: StockInfo, val broker: BrokerType) : State()
        object NoAccount : State()
        data class Error(val msg: String) : State()
    }

    var state by mutableStateOf<State>(State.Idle); private set

    // 마지막으로 로드한 종목코드 (중복 호출 방지)
    private var loadedCode: String? = null

    // 키움 → KIS → LS 우선순위
    private val priority = listOf(BrokerType.KIWOOM, BrokerType.KIS, BrokerType.LS)

    /** 종목정보 로드. 우선순위대로 appKey 가 있는 계좌를 찾아 호출한다. */
    fun load(code: String, force: Boolean = false) {
        val target = code.removePrefix("A")
        if (target.isBlank()) {
            state = State.Idle
            return
        }
        if (!force && target == loadedCode && state is State.Success) return
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
                logD("StockInfoViewModel: $target 조회 시도 - ${broker.displayName}")
                val result = fetch(broker, account, target)
                result
                    .onSuccess {
                        state = State.Success(it, broker)
                        return@launch
                    }
                    .onFailure {
                        lastError = it.message
                        logW("StockInfoViewModel: ${broker.displayName} 실패 - ${it.message}")
                    }
            }
            state = State.Error(lastError ?: "종목정보를 가져오지 못했습니다")
        }
    }

    private suspend fun fetch(
        broker: BrokerType,
        account: BrokerAccount,
        code: String,
    ): Result<StockInfo> = when (broker) {
        BrokerType.KIWOOM -> stockInfoRepo.fetchKiwoom(account, code)
        BrokerType.KIS -> stockInfoRepo.fetchKis(account, code)
        BrokerType.LS -> stockInfoRepo.fetchLs(account, code)
        BrokerType.TOSS -> stockInfoRepo.fetchToss(account, code)
    }
}
