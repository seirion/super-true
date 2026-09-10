package com.trueedu.tong.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val yyyyMMddFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

fun LocalDate.yyyyMMdd(): String = format(yyyyMMddFormatter)

fun LocalDate.isHoliday(): Boolean = when (dayOfWeek) {
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> true
    else -> holidays.contains(this)
}

/**
 * 예약주문 종료일(RSVN_ORD_END_DT).
 * 값을 비우면 KIS 는 다음 영업일 1회만 처리하고 예약을 종료하므로,
 * 기본 유효기간을 [days] 만큼 두고 휴장일이면 직전 영업일로 당긴다.
 */
fun scheduleEndDate(days: Long = 30): String {
    var date = LocalDate.now().plusDays(days)
    while (date.isHoliday()) {
        date = date.minusDays(1)
    }
    return date.yyyyMMdd()
}

/** yyyyMMdd -> yyyy.MM.dd */
fun formatYmd(raw: String): String =
    if (raw.length == 8) "${raw.substring(0, 4)}.${raw.substring(4, 6)}.${raw.substring(6, 8)}" else raw

// 주식 장이 열리지 않는 날 (건수가 적어 하드 코딩)
private val holidays = setOf(
    LocalDate.of(2026, 1, 1),
    LocalDate.of(2026, 2, 16),
    LocalDate.of(2026, 2, 17),
    LocalDate.of(2026, 2, 18),
    LocalDate.of(2026, 3, 2),
    LocalDate.of(2026, 5, 1),
    LocalDate.of(2026, 5, 5),
    LocalDate.of(2026, 5, 25),
    LocalDate.of(2026, 7, 17),
    LocalDate.of(2026, 8, 17),
    LocalDate.of(2026, 9, 24),
    LocalDate.of(2026, 9, 25),
    LocalDate.of(2026, 10, 9),
    LocalDate.of(2026, 12, 25),
    LocalDate.of(2026, 12, 31),
    LocalDate.of(2027, 1, 1),   // 신정
    LocalDate.of(2027, 2, 8),   // 설날 연휴
    LocalDate.of(2027, 2, 9),   // 대체공휴일(설날)
    LocalDate.of(2027, 3, 1),   // 3·1절
    LocalDate.of(2027, 5, 5),   // 어린이날
    LocalDate.of(2027, 5, 13),  // 부처님 오신날
    LocalDate.of(2027, 8, 16),  // 대체공휴일(광복절)
    LocalDate.of(2027, 9, 14),  // 추석 연휴
    LocalDate.of(2027, 9, 15),  // 추석
    LocalDate.of(2027, 9, 16),  // 추석 연휴
    LocalDate.of(2027, 10, 4),  // 대체공휴일(개천절)
    LocalDate.of(2027, 10, 11), // 대체공휴일(한글날)
    LocalDate.of(2027, 12, 27), // 대체공휴일(크리스마스)
)
