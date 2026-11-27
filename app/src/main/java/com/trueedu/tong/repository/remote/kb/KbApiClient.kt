package com.trueedu.tong.repository.remote.kb

import com.trueedu.tong.di.KbRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.kb.KbFields
import com.trueedu.tong.model.dto.kb.KbRequest
import com.trueedu.tong.model.dto.kb.KbResponse
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KB증권 Open API 공통 호출부.
 *
 * 토큰 첨부, HTTP 401 시 재발급 후 1회 재시도, 응답 헤더 성공 여부 검사를 맡고
 * 각 레포지토리는 TR 코드와 dataBody 만 넘긴다.
 */
@Singleton
class KbApiClient @Inject constructor(
    @KbRetrofitQualifier retrofit: Retrofit,
    private val tokenManager: TokenManager,
) {
    private val service by lazy { retrofit.create(KbApiService::class.java) }

    suspend fun call(
        account: BrokerAccount,
        tr: String,
        body: Map<String, String> = emptyMap(),
    ): Result<KbFields> = runCatching {
        var resp = send(tokenManager.getValidToken(account).getOrThrow(), tr, body)
        if (resp.code() == 401) {
            tokenManager.invalidateToken(account.id)
            resp = send(tokenManager.refreshToken(account).getOrThrow(), tr, body)
        }
        val parsed = resp.body() ?: error("KB 응답 없음: ${resp.code()}")
        val header = parsed.dataHeader
        if (header.resultCode != "200" || header.processFlag == "B") {
            error("KB 오류: ${header.processMessage.ifBlank { header.resultMessage }}")
        }
        KbFields(parsed.dataBody)
    }

    /** 응답의 다음키(nxt_key)가 비어 있을 때까지 이어서 조회해 [rowsKey] 레코드를 모두 모은다. */
    suspend fun callPaged(
        account: BrokerAccount,
        tr: String,
        body: Map<String, String>,
        rowsKey: String,
        maxPages: Int = 10,
    ): Result<List<KbFields>> = runCatching {
        val rows = mutableListOf<KbFields>()
        var nextKey = ""
        repeat(maxPages) {
            val data = call(account, tr, body + ("nxt_key" to nextKey)).getOrThrow()
            rows += data.rows(rowsKey)
            val key = data.str("nxt_key")
            if (key.isBlank() || key == nextKey) return@runCatching rows
            nextKey = key
        }
        rows
    }

    private suspend fun send(token: String, tr: String, body: Map<String, String>): Response<KbResponse> =
        service.call(
            path = "api/v1/${tr.lowercase()}",
            headers = mapOf("Authorization" to "Bearer $token"),
            body = KbRequest(dataBody = body),
        )
}
