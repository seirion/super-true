package com.trueedu.tong.ui.views.order

import kotlinx.serialization.Serializable

@Serializable
data class OrderRoute(val code: String, val accountId: Long)
