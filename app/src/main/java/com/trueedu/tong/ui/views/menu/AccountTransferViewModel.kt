package com.trueedu.tong.ui.views.menu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.CredentialStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class AccountTransferViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val credentialStorage: CredentialStorage,
    private val json: Json,
) : ViewModel() {

    var exportJson by mutableStateOf(""); private set
    var importPreview by mutableStateOf<List<AccountExportItem>>(emptyList()); private set
    var message by mutableStateOf<String?>(null); private set

    @Serializable
    data class AccountExportItem(
        val name: String,
        val brokerType: String,
        val accountNum: String,
        val appKey: String,
        val appSecret: String,
        val password: String = "",
    )

    fun loadExport() {
        viewModelScope.launch {
            val accounts = brokerAccountRepo.getAll().first()
            val items = accounts.map { acc ->
                AccountExportItem(
                    name = acc.name,
                    brokerType = acc.brokerType.name,
                    accountNum = acc.accountNum,
                    appKey = credentialStorage.getAppKey(acc.id),
                    appSecret = credentialStorage.getAppSecret(acc.id),
                    password = credentialStorage.getPassword(acc.id),
                )
            }
            exportJson = json.encodeToString(items)
        }
    }

    fun parseImport(jsonText: String) {
        try {
            importPreview = json.decodeFromString(jsonText)
            message = "가져올 계좌 ${importPreview.size}개 확인됨"
        } catch (e: Exception) {
            importPreview = emptyList()
            message = "JSON 파싱 오류: ${e.message}"
        }
    }

    fun executeImport() {
        viewModelScope.launch {
            var added = 0
            var updated = 0
            val existing = brokerAccountRepo.getAll().first()
            importPreview.forEach { item ->
                try {
                    val brokerType = BrokerType.valueOf(item.brokerType)
                    // 동일 증권사 + 계좌번호 조합이 있으면 업데이트, 없으면 신규
                    val duplicate = existing.firstOrNull {
                        it.brokerType == brokerType && it.accountNum == item.accountNum
                    }
                    if (duplicate != null) {
                        // 기존 계좌 업데이트 (이름 + credentials)
                        val updated_account = duplicate.copy(name = item.name)
                        brokerAccountRepo.update(updated_account, item.appKey, item.appSecret, item.password)
                        updated++
                    } else {
                        val account = BrokerAccount(
                            name = item.name,
                            brokerType = brokerType,
                            accountNum = item.accountNum,
                        )
                        brokerAccountRepo.insert(account, item.appKey, item.appSecret, item.password)
                        added++
                    }
                } catch (e: Exception) {
                    // 스킵 (잘못된 brokerType 등)
                }
            }
            importPreview = emptyList()
            message = buildString {
                if (added > 0) append("${added}개 신규 추가")
                if (added > 0 && updated > 0) append(", ")
                if (updated > 0) append("${updated}개 업데이트")
                append(" 완료")
            }
        }
    }

    fun clearMessage() { message = null }
}
