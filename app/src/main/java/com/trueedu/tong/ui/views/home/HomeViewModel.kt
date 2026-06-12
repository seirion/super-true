package com.trueedu.tong.ui.views.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.remote.AccountSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val accountSummaryUseCase: AccountSummaryUseCase,
) : ViewModel() {

    // 선택된 계좌
    val selectedAccount: StateFlow<BrokerAccount?> = brokerAccountRepo.getAll()
        .map { it.firstOrNull { acc -> acc.isSelected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI 상태
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val summary: AccountSummary) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // 선택된 계좌가 바뀌면 자동으로 데이터 로딩
        viewModelScope.launch {
            selectedAccount.collectLatest { account ->
                if (account != null) loadSummary(account)
                else _uiState.value = UiState.Idle
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            selectedAccount.value?.let { loadSummary(it) }
        }
    }

    private suspend fun loadSummary(account: BrokerAccount) {
        _uiState.value = UiState.Loading
        accountSummaryUseCase.fetch(account)
            .onSuccess { _uiState.value = UiState.Success(it) }
            .onFailure { _uiState.value = UiState.Error(it.message ?: "오류가 발생했습니다") }
    }
}
