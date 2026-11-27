package com.trueedu.tong.model.dto.kb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class KbRequestHeader(
    val ipAddr: String = "127.0.0.1",
    val macAddr: String = "00:00:00:00:00:00",
)

@Serializable
data class KbRequest(
    val dataHeader: KbRequestHeader = KbRequestHeader(),
    val dataBody: Map<String, String> = emptyMap(),
)

@Serializable
data class KbResponseHeader(
    val resultCode: String = "",
    val resultMessage: String = "",
    val processFlag: String = "",
    val processCode: String = "",
    val processMessage: String = "",
)

@Serializable
data class KbResponse(
    val dataHeader: KbResponseHeader = KbResponseHeader(),
    val dataBody: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class KbTokenBody(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0L,
)

@Serializable
data class KbTokenResponse(
    val dataHeader: KbResponseHeader = KbResponseHeader(),
    val dataBody: KbTokenBody = KbTokenBody(),
)

/**
 * KB 응답 dataBody 의 한 레코드. 값은 전부 문자열이고 숫자는 0 으로 왼쪽이 채워져 있다
 * ("000000000018.45", "   1.78") 라서 공백과 콤마를 걷어낸 뒤 변환한다.
 */
class KbFields(private val obj: JsonObject) {
    fun str(key: String): String {
        val v = obj[key] as? JsonPrimitive ?: return ""
        return if (v is JsonNull) "" else v.content.trim()
    }

    fun num(key: String): Double? = str(key).replace(",", "").toDoubleOrNull()

    fun long(key: String): Long? = num(key)?.toLong()

    fun rows(key: String): List<KbFields> =
        (obj[key] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::KbFields) } ?: emptyList()
}
