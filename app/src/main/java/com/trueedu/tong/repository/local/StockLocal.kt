package com.trueedu.tong.repository.local

import com.trueedu.tong.db.StockInfoLocalDao
import com.trueedu.tong.model.StockInfoLocal
import javax.inject.Inject

class StockLocal @Inject constructor(
    private val stockInfoLocalDao: StockInfoLocalDao
) {
    suspend fun getAllStocks(): List<StockInfoLocal> {
        return stockInfoLocalDao.getAllStocks()
    }

    suspend fun setAllStocks(stocks: List<StockInfoLocal>)  {
        return stockInfoLocalDao.insertAll(stocks)
    }

    suspend fun deleteAllStocks()  {
        return stockInfoLocalDao.deleteAllStocks()
    }
}
