package com.trueedu.tong.utils

import java.text.DecimalFormat

object NumberFormatter {
    private val cashFormat = DecimalFormat("#,###")
    private val rateFormat = DecimalFormat("+#,##0.00;-#,##0.00")
    private val indexFormat = DecimalFormat("#,##0.00")
    private val indexSignFormat = DecimalFormat("+#,##0.00;-#,##0.00")

    fun formatCash(value: Double): String = cashFormat.format(value)
    fun formatRate(value: Double): String = rateFormat.format(value) + "%"
    fun formatCashWithSign(value: Double): String {
        val prefix = if (value > 0) "+" else ""
        return "$prefix${cashFormat.format(value)}"
    }

    // 지수(코스피/코스닥)용 소수점 2자리 포맷
    fun formatIndex(value: Double): String = indexFormat.format(value)
    fun formatIndexWithSign(value: Double): String = indexSignFormat.format(value)
}
