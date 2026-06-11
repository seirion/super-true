package com.trueedu.`super`

import androidx.lifecycle.ViewModel
import com.trueedu.`super`.repository.local.Local
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val local: Local,
) : ViewModel() {

    fun init() {
        // TODO: 초기화 로직
    }
}
