package com.trueedu.tong.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebSocketUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppVersion

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppVersionCode

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KisRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KisOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KisRetrofitQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KisWsOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KiwoomRetrofitQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LsRetrofitQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KiwoomOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KiwoomWsOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LsOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TossRetrofitQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TossOkHttp
