package com.trueedu.tong.utils

import timber.log.Timber

// 호출 위치를 찾기 위한 오프셋
private const val CALL_STACK_OFFSET = 2

// Basic logging with lambda
fun logV(message: () -> String) = Timber.tag(getCallerClassName()).v(message())
fun logD(message: () -> String) = Timber.tag(getCallerClassName()).d(message())
fun logI(message: () -> String) = Timber.tag(getCallerClassName()).i(message())
fun logW(message: () -> String) = Timber.tag(getCallerClassName()).w(message())
fun logE(message: () -> String) = Timber.tag(getCallerClassName()).e(message())

// String overloads
fun logV(message: String) = Timber.tag(getCallerClassName()).v(message)
fun logD(message: String) = Timber.tag(getCallerClassName()).d(message)
fun logI(message: String) = Timber.tag(getCallerClassName()).i(message)
fun logW(message: String) = Timber.tag(getCallerClassName()).w(message)
fun logE(message: String) = Timber.tag(getCallerClassName()).e(message)

// With throwable
fun logV(t: Throwable, message: String) = Timber.tag(getCallerClassName()).v(t, message)
fun logD(t: Throwable, message: String) = Timber.tag(getCallerClassName()).d(t, message)
fun logI(t: Throwable, message: String) = Timber.tag(getCallerClassName()).i(t, message)
fun logW(t: Throwable, message: String) = Timber.tag(getCallerClassName()).w(t, message)
fun logE(t: Throwable, message: String) = Timber.tag(getCallerClassName()).e(t, message)

// With format
fun logV(format: String, vararg args: Any?) = Timber.tag(getCallerClassName()).v(format, *args)
fun logD(format: String, vararg args: Any?) = Timber.tag(getCallerClassName()).d(format, *args)
fun logI(format: String, vararg args: Any?) = Timber.tag(getCallerClassName()).i(format, *args)
fun logW(format: String, vararg args: Any?) = Timber.tag(getCallerClassName()).w(format, *args)
fun logE(format: String, vararg args: Any?) = Timber.tag(getCallerClassName()).e(format, *args)

// 호출자의 클래스 이름을 가져오는 함수
private fun getCallerClassName(): String {
    return Throwable().stackTrace
        .getOrNull(CALL_STACK_OFFSET)
        ?.let { element ->
            element.className.substringAfterLast('.').substringBefore('$')
        } ?: "Unknown"
}
