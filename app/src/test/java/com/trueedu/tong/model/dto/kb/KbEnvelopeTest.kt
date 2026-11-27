package com.trueedu.tong.model.dto.kb

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KbEnvelopeTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun `요청은 dataHeader 와 dataBody 로 감싸 직렬화된다`() {
        val encoded = json.encodeToString(
            KbRequest.serializer(),
            KbRequest(dataBody = mapOf("is_cd" to "005930")),
        )
        assertEquals(
            """{"dataHeader":{"ipAddr":"127.0.0.1","macAddr":"00:00:00:00:00:00"},"dataBody":{"is_cd":"005930"}}""",
            encoded,
        )
    }

    @Test
    fun `토큰 응답을 파싱한다`() {
        val body = """
            {"dataHeader":{"resultCode":"200","resultMessage":"성공","processFlag":"A","processCode":"0000"},
             "dataBody":{"access_token":"abc","token_type":"Bearer","expires_in":86400}}
        """.trimIndent()
        val parsed = json.decodeFromString(KbTokenResponse.serializer(), body)
        assertEquals("200", parsed.dataHeader.resultCode)
        assertEquals("abc", parsed.dataBody.accessToken)
        assertEquals(86400L, parsed.dataBody.expiresIn)
    }

    @Test
    fun `0 으로 채워진 숫자 문자열과 레코드 배열을 읽는다`() {
        val body = """
            {"dataHeader":{"resultCode":"200"},
             "dataBody":{"val_yld_sum":"000000000018.45","up_dwn_r_p2":"   1.78","nxt_key":"  ",
               "ordr_q":"000000000000000005","empty":"","nullable":null,
               "Record1":[{"is_cd":"A005930","ec_q":"000000000000001"}]}}
        """.trimIndent()
        val data = KbFields(json.decodeFromString(KbResponse.serializer(), body).dataBody)

        assertEquals(18.45, data.num("val_yld_sum")!!, 0.0)
        assertEquals(1.78, data.num("up_dwn_r_p2")!!, 0.0)
        assertEquals(5L, data.long("ordr_q"))
        assertEquals("", data.str("nxt_key"))
        assertNull(data.num("empty"))
        assertEquals("", data.str("nullable"))
        assertEquals("", data.str("missing"))

        val row = data.rows("Record1").single()
        assertEquals("A005930", row.str("is_cd"))
        assertEquals(1L, row.long("ec_q"))
        assertEquals(emptyList<KbFields>(), data.rows("missing").toList())
    }
}
