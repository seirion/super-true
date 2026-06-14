package com.trueedu.tong.ui.chart

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 캔들차트 제스처 처리.
 * - 핀치 줌 (0.5f ~ 4.0f), 중심점 기준 scrollOffset 보정
 * - 수평 드래그로 스크롤
 * - 롱프레스 후 드래그로 십자선 이동, 탭으로 십자선 해제
 *
 * @param candleWidth 현재 줌 적용된 캔들 너비(px)
 * @param viewportWidth 캔들이 그려지는 영역 너비(px)
 */
@Composable
fun Modifier.candleChartGestures(
    state: CandleChartState,
    candleWidth: Float,
    viewportWidth: Float,
    onLoadMore: () -> Unit,
): Modifier = this
    .pointerInput(state.candles.size, viewportWidth) {
        detectTransformGestures { centroid, pan, zoomChange, _ ->
            val old = state.zoom
            val newZoom = (old * zoomChange).coerceIn(0.5f, 4f)
            state.zoom = newZoom
            val ratio = if (old != 0f) newZoom / old else 1f

            // 줌 후 캔들 너비 근사값으로 스크롤 범위 재계산
            val cw = candleWidth * ratio
            val totalWidth = cw * state.candles.size
            val maxOffset = maxOf(0f, totalWidth - viewportWidth)

            // 중심점 기준 보정 + 수평 패닝 (pan.x > 0 = 오른쪽 드래그 = 과거 보기)
            val contentX = state.scrollOffset + centroid.x
            val newOffset = (contentX * ratio - centroid.x - pan.x).coerceIn(0f, maxOffset)
            state.scrollOffset = newOffset

            // 과거(왼쪽) 끝에 가까우면 추가 로드
            if (newOffset < cw * 20) onLoadMore()
        }
    }
    .pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.crosshairPos = it },
            onDrag = { change, _ ->
                state.crosshairPos = change.position
                change.consume()
            },
            onDragEnd = {},
            onDragCancel = {},
        )
    }
    .pointerInput(Unit) {
        detectTapGestures(onTap = { state.crosshairPos = null })
    }
