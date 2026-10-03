package com.trueedu.tong.data.realtime

import android.content.Context
import android.content.SharedPreferences
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.kis.KisOverseasProduct
import com.trueedu.tong.model.dto.kis.KisOverseasProductResponse
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.repository.remote.kis.KisPriceService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit

class KisOverseasExchangeResolverTest {
    private class FakePrefs : SharedPreferences {
        val map = mutableMapOf<String, String>()
        override fun getString(key: String, defValue: String?) = map[key] ?: defValue
        override fun edit(): SharedPreferences.Editor {
            val outer = this
            return object : SharedPreferences.Editor {
                override fun putString(key: String, value: String?) = apply { outer.map[key] = value ?: "" }
                override fun apply() {}
                override fun commit() = true
                override fun clear() = this
                override fun remove(key: String) = this
                override fun putStringSet(key: String, values: MutableSet<String>?) = this
                override fun putInt(key: String, value: Int) = this
                override fun putLong(key: String, value: Long) = this
                override fun putFloat(key: String, value: Float) = this
                override fun putBoolean(key: String, value: Boolean) = this
            }
        }
        override fun getAll() = map.toMutableMap<String, Any?>()
        override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun getBoolean(key: String, defValue: Boolean) = defValue
        override fun contains(key: String) = map.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private fun resolver(service: KisPriceService, prefs: SharedPreferences): KisOverseasExchangeResolver {
        val retrofit = mockk<Retrofit>()
        every { retrofit.create(KisPriceService::class.java) } returns service
        val token = mockk<TokenManager>()
        coEvery { token.getValidToken(any()) } returns Result.success("t")
        val cred = mockk<CredentialStorage>()
        every { cred.getAppKey(any()) } returns "k"
        every { cred.getAppSecret(any()) } returns "s"
        val ctx = mockk<Context>()
        every { ctx.getSharedPreferences(any(), any()) } returns prefs
        return KisOverseasExchangeResolver(retrofit, cred, token, ctx)
    }

    private fun found() = Response.success(
        KisOverseasProductResponse(output = KisOverseasProduct(stdPdno = "US123", exchangeCode = "NYSE"), rtCd = "0")
    )
    private fun notFound() = Response.success(
        KisOverseasProductResponse(rtCd = "7", msg1 = "조회된 데이터가 없습니다.(해외주식기본)")
    )

    @Test
    fun `나스닥에 없으면 뉴욕 아멕스 순서로 조회하고 결과를 캐시한다`() = runBlocking {
        val service = mockk<KisPriceService>()
        coEvery { service.getOverseasProduct(any(), any()) } answers {
            when (secondArg<Map<String, String>>()["PRDT_TYPE_CD"]) {
                "512" -> notFound()
                "513" -> found()
                else -> notFound()
            }
        }
        val prefs = FakePrefs()
        val r = resolver(service, prefs)
        val account = mockk<BrokerAccount>(relaxed = true)

        val result = r.resolve(account, listOf("CPNG"))

        assertEquals(mapOf("CPNG" to "NYS"), result)
        assertEquals("NYS", r.cached("CPNG"))
    }

    @Test
    fun `어디서도 찾지 못한 티커는 결과와 캐시에서 빠진다`() = runBlocking {
        val service = mockk<KisPriceService>()
        coEvery { service.getOverseasProduct(any(), any()) } returns notFound()
        val r = resolver(service, FakePrefs())

        val result = r.resolve(mockk(relaxed = true), listOf("ZZZZ"))

        assertEquals(emptyMap<String, String>(), result)
        assertEquals(null, r.cached("ZZZZ"))
    }
}
