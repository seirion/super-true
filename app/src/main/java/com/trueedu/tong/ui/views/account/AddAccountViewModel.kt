package com.trueedu.tong.ui.views.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.ui.main.AddAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val repo: BrokerAccountRepository,
    private val credentialStorage: CredentialStorage,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val accountId: Long = savedStateHandle.toRoute<AddAccount>().accountId
    val isEditMode: Boolean get() = accountId != -1L

    init {
        if (isEditMode) {
            viewModelScope.launch {
                repo.getAll().first().find { it.id == accountId }?.let { account ->
                    name = account.name
                    brokerType = account.brokerType
                    accountNum = account.accountNum
                    appKey = credentialStorage.getAppKey(accountId)
                    appSecret = credentialStorage.getAppSecret(accountId)
                    password = credentialStorage.getPassword(accountId)
                }
            }
        }
    }

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
    var password by mutableStateOf("")
        private set

    var saved by mutableStateOf(false)
        private set

    val isValid: Boolean
        get() = name.isNotBlank() &&
            accountNum.isNotBlank() &&
            appKey.isNotBlank() &&
            appSecret.isNotBlank() &&
            (brokerType != BrokerType.KIWOOM || password.isNotBlank()) &&
            (brokerType != BrokerType.KIWOOM || accountNum.length == 10)

    fun onNameChange(value: String) { name = value }
    fun onBrokerTypeChange(value: BrokerType) { brokerType = value }
    fun onAccountNumChange(value: String) { accountNum = value }
    fun onAppKeyChange(value: String) { appKey = value }
    fun onAppSecretChange(value: String) { appSecret = value }
    fun onPasswordChange(value: String) { password = value }

    suspend fun saveAccount() {
        if (!isValid) return
        if (isEditMode) {
            val account = BrokerAccount(
                id = accountId,
                name = name.trim(),
                brokerType = brokerType,
                accountNum = accountNum.trim(),
                isSelected = false,
                isActive = true,
            )
            repo.update(account, appKey.trim(), appSecret.trim(), password.trim())
        } else {
            val account = BrokerAccount(
                name = name.trim(),
                brokerType = brokerType,
                accountNum = accountNum.trim(),
            )
            val id = repo.insert(account, appKey.trim(), appSecret.trim(), password.trim())
            repo.select(id)  // 새로 추가한 계좌를 바로 선택
        }
        saved = true
    }
}
