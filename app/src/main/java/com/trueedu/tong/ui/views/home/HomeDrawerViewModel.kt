package com.trueedu.tong.ui.views.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.repository.BrokerAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeDrawerViewModel @Inject constructor(
    private val repo: BrokerAccountRepository,
) : ViewModel() {
    val accounts: StateFlow<List<BrokerAccount>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(id: Long) {
        viewModelScope.launch { repo.select(id) }
    }

    fun deleteAccount(account: BrokerAccount) {
        viewModelScope.launch { repo.delete(account) }
    }
}
