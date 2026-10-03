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

    private val quantityFormat = DecimalFormat("#,##0.######")

    /** 수량: 정수는 그대로, 소수점 보유분은 최대 6자리까지 (1.123707) */
    fun formatQuantity(value: Double): String = quantityFormat.format(value)

    private val usdFormat = DecimalFormat("#,##0.00")
    private val usdSignFormat = DecimalFormat("+#,##0.00;-#,##0.00")

    /** 거래 통화에 맞춘 금액 표기: KRW "1,234원", USD "$1,234.56" */
    fun formatMoney(value: Double, currency: String): String =
        if (currency == "USD") "$" + usdFormat.format(value) else formatCash(value) + "원"

    /** 부호 포함 금액 (통화 기호 없음): KRW "+1,234", USD "+1,234.56" */
    fun formatMoneyWithSign(value: Double, currency: String): String =
        if (currency == "USD") usdSignFormat.format(value) else formatCashWithSign(value)

    // 지수(코스피/코스닥)용 소수점 2자리 포맷
    fun formatIndex(value: Double): String = indexFormat.format(value)
    fun formatIndexWithSign(value: Double): String = indexSignFormat.format(value)
}
