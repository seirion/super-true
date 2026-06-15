package com.trueedu.tong

import androidx.lifecycle.ViewModel
import com.trueedu.tong.repository.local.Local
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val local: Local,
) : ViewModel() {

    val keepScreenOn: Boolean get() = local.keepScreenOn

    fun init() {
        // TODO: 초기화 로직
    }
}
