package com.trueedu.tong.utils

import java.text.DecimalFormat

object NumberFormatter {
    private val cashFormat = DecimalFormat("#,###")
    private val rateFormat = DecimalFormat("+#,##0.00;-#,##0.00")

    fun formatCash(value: Double): String = cashFormat.format(value)
    fun formatRate(value: Double): String = rateFormat.format(value) + "%"
    fun formatCashWithSign(value: Double): String {
        val prefix = if (value > 0) "+" else ""
        return "$prefix${cashFormat.format(value)}"
    }
}
