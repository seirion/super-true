package com.trueedu.tong.ui.views.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchPageTest {
    @Test
    fun `저장된 페이지가 범위 안이면 그대로 쓴다`() {
        assertEquals(0, WatchViewModel.normalizePage(0))
        assertEquals(1, WatchViewModel.normalizePage(1))
    }

    @Test
    fun `범위를 벗어난 값은 한국 페이지로 되돌린다`() {
        assertEquals(0, WatchViewModel.normalizePage(-1))
        assertEquals(0, WatchViewModel.normalizePage(2))
        assertEquals(0, WatchViewModel.normalizePage(99))
    }
}
