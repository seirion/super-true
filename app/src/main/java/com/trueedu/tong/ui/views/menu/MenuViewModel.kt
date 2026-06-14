package com.trueedu.tong.ui.views.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.repository.local.StockLocal
import com.trueedu.tong.utils.StockInfoDownloader
import com.trueedu.tong.utils.logE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val stockInfoDownloader: StockInfoDownloader,
    private val stockLocal: StockLocal,
) : ViewModel() {

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    fun downloadStockInfo(onComplete: (Boolean) -> Unit) {
        if (_downloading.value) return

        _downloading.value = true
        viewModelScope.launch {
            try {
                val stocks = stockInfoDownloader.getStockInfoList()
                val locals = stocks.map {
                    StockInfoLocal(
                        code = it.code,
                        nameKr = it.nameKr,
                        attributes = it.attributes,
                        kospi = it.kospi(),
                    )
                }
                stockLocal.deleteAllStocks()
                stockLocal.setAllStocks(locals)
                onComplete(true)
            } catch (e: Exception) {
                logE("종목파일 다운로드 실패: $e")
                onComplete(false)
            } finally {
                _downloading.value = false
            }
        }
    }
}
