package com.trueedu.tong.repository

import com.trueedu.tong.db.BrokerAccountDao
import com.trueedu.tong.model.BrokerAccount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BrokerAccountRepository @Inject constructor(
    private val dao: BrokerAccountDao
) {
    fun getAll(): Flow<List<BrokerAccount>> = dao.getAll()
    suspend fun insert(account: BrokerAccount) = dao.insert(account)
    suspend fun delete(account: BrokerAccount) = dao.delete(account)
    suspend fun select(id: Long) {
        dao.clearSelection()
        dao.select(id)
    }
}
