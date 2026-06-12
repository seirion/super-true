package com.trueedu.tong.ui.views.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.repository.BrokerAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val repo: BrokerAccountRepository,
) : ViewModel() {

    var name by mutableStateOf("")
        private set
    var brokerType by mutableStateOf(BrokerType.KIS)
        private set
    var accountNum by mutableStateOf("")
        private set
    var appKey by mutableStateOf("")
        private set
    var appSecret by mutableStateOf("")
        private set

    val isValid: Boolean
        get() = name.isNotBlank() &&
            accountNum.isNotBlank() &&
            appKey.isNotBlank() &&
            appSecret.isNotBlank()

    fun onNameChange(value: String) { name = value }
    fun onBrokerTypeChange(value: BrokerType) { brokerType = value }
    fun onAccountNumChange(value: String) { accountNum = value }
    fun onAppKeyChange(value: String) { appKey = value }
    fun onAppSecretChange(value: String) { appSecret = value }

    suspend fun saveAccount() {
        if (!isValid) return
        val account = BrokerAccount(
            name = name.trim(),
            brokerType = brokerType,
            accountNum = accountNum.trim(),
        )
        repo.insert(account, appKey.trim(), appSecret.trim())
    }
}
