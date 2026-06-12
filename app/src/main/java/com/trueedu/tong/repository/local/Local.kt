package com.trueedu.tong.repository.local

import android.content.SharedPreferences
import com.trueedu.tong.extensions.boolean
import com.trueedu.tong.extensions.int
import com.trueedu.tong.extensions.long
import com.trueedu.tong.extensions.string
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences 래퍼.
 * 로컬에 저장하는 간단한 값들을 프로퍼티 위임으로 노출한다.
 */
@Singleton
class Local @Inject constructor(private val preferences: SharedPreferences) {
    private val latestVersion = 1
    private var currentVersion by preferences.int(latestVersion)

    // 앱 실행 횟수
    var launchingCount by preferences.long(0)
        private set

    fun migrate() {
        launchingCount++
    }

    // 인증 토큰
    var accessToken by preferences.string("")
    var accessTokenExpiredAt by preferences.long(0L)

    // 확인한 notice 마지막 id
    var appNoticeId by preferences.int(0)

    // UI 설정
    var forceDark by preferences.boolean(true)
    var theme by preferences.int(1)
    var keepScreenOn by preferences.boolean(false)

    // 홈화면 시세/평가 모드 (true=시세, false=평가)
    var marketPriceMode by preferences.boolean(true)

    // 홈화면 요약 섹션 펼침 여부
    var summaryExpanded by preferences.boolean(true)

    // 주문 탭에서 선택된 종목코드/계좌ID
    var selectedOrderCode by preferences.string("")
    var selectedOrderAccountId by preferences.long(-1L)
}
