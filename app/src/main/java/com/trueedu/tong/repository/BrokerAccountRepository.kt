package com.trueedu.tong.repository

import com.trueedu.tong.db.BrokerAccountDao
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.repository.local.CredentialStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BrokerAccountRepository @Inject constructor(
    private val dao: BrokerAccountDao,
    private val credentialStorage: CredentialStorage
) {
    fun getAll(): Flow<List<BrokerAccount>> = dao.getAll()

    suspend fun insert(account: BrokerAccount, appKey: String, appSecret: String, password: String = ""): Long {
        val id = dao.insert(account)
        credentialStorage.clearToken(id)
        credentialStorage.saveCredentials(id, appKey, appSecret)
        if (password.isNotBlank()) credentialStorage.savePassword(id, password)
        return id
    }

    suspend fun update(account: BrokerAccount, appKey: String, appSecret: String, password: String = "") {
        dao.update(account)
        credentialStorage.clearToken(account.id)
        credentialStorage.saveCredentials(account.id, appKey, appSecret)
        if (password.isNotBlank()) credentialStorage.savePassword(account.id, password)
    }

    suspend fun delete(account: BrokerAccount) {
        dao.delete(account)
        credentialStorage.deleteCredentials(account.id)
    }

    suspend fun select(id: Long) {
        dao.clearSelection()
        dao.select(id)
    }

    fun getAppKey(accountId: Long) = credentialStorage.getAppKey(accountId)
    fun getAppSecret(accountId: Long) = credentialStorage.getAppSecret(accountId)
}
