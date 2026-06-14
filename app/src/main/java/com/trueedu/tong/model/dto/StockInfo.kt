package com.trueedu.tong.model.dto

abstract class StockInfo(
    val code: String,
    val nameKr: String,
    val attributes: String,
) {
    abstract fun getAttribute(key: String): String?
    abstract fun kospi(): Boolean
    abstract fun kosdaq(): Boolean

    abstract fun spac(): Boolean
    abstract fun halt(): Boolean
    abstract fun designated(): Boolean
    abstract fun parValue(): String?
    abstract fun listingDate(): String?
    abstract fun listingShares(): String?

    abstract fun prevPrice(): String?
    abstract fun prevVolume(): String?
    abstract fun marketCap(): String? // 전일 기준 시총
    abstract fun sales(): String?
    abstract fun operatingProfit(): String?
    abstract fun shortSellingOverheating(): Boolean // 공매도과열
    abstract fun unusualPriceSurge(): Boolean // 이상급등
}

/**
 * 데이터 형식 참고
 * kospi:
 * https://github.com/koreainvestment/open-trading-api/blob/main/stocks_info/kis_kospi_code_mst.py
 */
class StockInfoKospi(
    code: String,
    nameKr: String,
    attributes: String,
): StockInfo(code, nameKr, attributes) {

    companion object {
        fun from(str: String): StockInfoKospi {
            val code = str.substring(0, 9).trim()
            val nameKr = str.substring(21, str.length - ATTRIBUTES_LEN).trim()
            val attributes = str.takeLast(ATTRIBUTES_LEN)

            return StockInfoKospi(code, nameKr, attributes)
        }

        private const val ATTRIBUTES_LEN = 227 // 예제에서는 228 이지만 실제로 해보면 하나 적음

        // for kospi attributes
        val fieldSpecs = listOf(
            0,
            2, 3, 7, 11, 15,
            16, 17, 18, 19, 20,
            21, 22, 23, 24, 25,
            26, 27, 28, 29, 30,
            31, 32, 33, 34, 35,
            36, 37, 38, 39, 40,
            41, 50, 55, 60, 61,
            62, 63, 65, 66, 67,
            68, 70, 72, 74, 77,
            78, 81, 93, 105, 113,
            128, 149, 151, 158, 159,
            160, 161, 162, 163, 172,
            181, 190, 195, 204, 212,
            221, 224, 225, 226, 227
        )

        private val columns = listOf(
            "그룹코드", "시가총액규모", "지수업종대분류", "지수업종중분류", "지수업종소분류",
            "제조업", "저유동성", "지배구조지수종목", "KOSPI200섹터업종", "KOSPI100",
            "KOSPI50", "KRX", "ETP", "ELW발행", "KRX100",
            "KRX자동차", "KRX반도체", "KRX바이오", "KRX은행", "SPAC",
            "KRX에너지화학", "KRX철강", "단기과열", "KRX미디어통신", "KRX건설",
            "Non1", "KRX증권", "KRX선박", "KRX섹터_보험", "KRX섹터_운송",
            "SRI", "기준가", "매매수량단위", "시간외수량단위", "거래정지",
            "정리매매", "관리종목", "시장경고", "경고예고", "불성실공시",
            "우회상장", "락구분", "액면변경", "증자구분", "증거금비율",
            "신용가능", "신용기간", "전일거래량", "액면가", "상장일자",
            "상장주수", "자본금", "결산월", "공모가", "우선주",
            "공매도과열", "이상급등", "KRX300", "KOSPI", "매출액",
            "영업이익", "경상이익", "당기순이익", "ROE", "기준년월",
            "시가총액", "그룹사코드", "회사신용한도초과", "담보대출가능", "대주가능"
        )
            .mapIndexed { index, s -> s to index }
            .toMap()
    }

    // No-argument constructor required for Firebase
    constructor() : this("000000", "", "")

    override fun spac() = getAttribute("SPAC") == "Y"
    override fun halt() = getAttribute("거래정지") == "Y"
    override fun designated() = getAttribute("관리종목") == "Y"
    override fun parValue() = getAttribute("액면가")
    override fun listingDate() = getAttribute("상장일자")
    override fun listingShares() = getAttribute("상장주수")

    override fun prevPrice() = getAttribute("기준가")
    override fun prevVolume() = getAttribute("전일거래량")
    override fun marketCap() = getAttribute("시가총액")?.dropWhile { it == '0' } // 전일 기준
    override fun sales() = getAttribute("매출액")
    override fun operatingProfit() = getAttribute("영업이익")
    override fun shortSellingOverheating() = getAttribute("공매도과열") == "Y"
    override fun unusualPriceSurge() = getAttribute("이상급등") == "Y"

    fun kospi100() = getAttribute("KOSPI100")
    fun kospi50() = getAttribute("KOSPI50")
    fun capitalStock() = getAttribute("자본금")


    // stock attributes
    override fun getAttribute(key: String): String? {
        return columns[key]?.let { index ->
            return attributes.substring(fieldSpecs[index], fieldSpecs[index + 1])
        }
    }

    override fun kospi() = true
    override fun kosdaq() = false
}

/**
 * 데이터 형식 참고
 * kosdaq:
 * https://github.com/koreainvestment/open-trading-api/blob/main/stocks_info/kis_kosdaq_code_mst.py
 */
class StockInfoKosdaq(
    code: String,
    nameKr: String,
    attributes: String,
): StockInfo(code, nameKr, attributes) {

    companion object {
        fun from(str: String): StockInfoKosdaq {
            val code = str.substring(0, 9).trim()
            val nameKr = str.substring(21, str.length - ATTRIBUTES_LEN).trim()
            val attributes = str.takeLast(ATTRIBUTES_LEN)

            return StockInfoKosdaq(code, nameKr, attributes)
        }

        private const val ATTRIBUTES_LEN = 221 // 예제에서는 222 이지만 실제로 해보면 하나 적음

        val fieldSpecs = listOf(
            0,
            2, 3, 7, 11, 15,
            16, 17, 18, 19, 20,
            21, 22, 23, 24, 25,
            26, 27, 28, 29, 30,
            31, 32, 33, 34, 35,
            36, 45, 50, 55, 56,
            57, 58, 60, 61, 62,
            63, 65, 67, 69, 72,
            73, 76, 88, 100, 108,
            123, 144, 146, 153, 154,
            155, 156, 157, 166, 175,
            184, 189, 198, 206, 215,
            218, 219, 220, 221
        )

        /**
         * 혼란을 방지하기 위해 예제 코드의 텍스트를 그대로 옮김
         */
        val columns = listOf(
            "증권그룹구분코드", "시가총액 규모 구분 코드 유가", "지수업종 대분류 코드", "지수 업종 중분류 코드", "지수업종 소분류 코드",
            "벤처기업 여부 (Y/N)", "저유동성종목 여부", "KRX 종목 여부", "ETP 상품구분코드", "KRX100 종목 여부 (Y/N)",
            "KRX 자동차 여부", "KRX 반도체 여부", "KRX 바이오 여부", "KRX 은행 여부", "기업인수목적회사여부",
            "KRX 에너지 화학 여부", "KRX 철강 여부", "단기과열종목구분코드", "KRX 미디어 통신 여부", "KRX 건설 여부",
            "(코스닥)투자주의환기종목여부", "KRX 증권 구분", "KRX 선박 구분", "KRX섹터지수 보험여부", "KRX섹터지수 운송여부",
            "KOSDAQ150지수여부 (Y,N)", "주식 기준가", "정규 시장 매매 수량 단위", "시간외 시장 매매 수량 단위", "거래정지 여부",
            "정리매매 여부", "관리 종목 여부", "시장 경고 구분 코드", "시장 경고위험 예고 여부", "불성실 공시 여부",
            "우회 상장 여부", "락구분 코드", "액면가 변경 구분 코드", "증자 구분 코드", "증거금 비율",
            "신용주문 가능 여부", "신용기간", "전일 거래량", "주식 액면가", "주식 상장 일자",
            "상장 주수(천)", "자본금", "결산 월", "공모 가격", "우선주 구분 코드",
            "공매도과열종목여부", "이상급등종목여부", "KRX300 종목 여부 (Y/N)", "매출액", "영업이익",
            "경상이익", "단기순이익", "ROE(자기자본이익률)", "기준년월", "전일기준 시가총액 (억)",
            "그룹사 코드", "회사신용한도초과여부", "담보대출가능여부", "대주가능여부"
        )
            .mapIndexed { index, s -> s to index }
            .toMap()
    }

    // No-argument constructor required for Firebase
    constructor() : this("000000", "", "")

    // stock attributes
    override fun getAttribute(key: String): String? {
        return columns[key]?.let { index ->
            return attributes.substring(fieldSpecs[index], fieldSpecs[index + 1])
        }
    }

    fun kosdaq150() = getAttribute("KOSDAQ150지수여부 (Y,N)")

    override fun spac() = nameKr.contains("스팩")
    override fun halt() = getAttribute("거래정지 여부") == "Y"
    override fun designated() = getAttribute("관리 종목 여부") == "Y"
    override fun parValue() = getAttribute("주식 액면가")
    override fun listingDate() = getAttribute("주식 상장 일자")
    override fun listingShares() = getAttribute("상장 주수(천)")

    override fun prevPrice() = getAttribute("주식 기준가")
    override fun prevVolume() = getAttribute("전일 거래량")
    override fun marketCap() = getAttribute("전일기준 시가총액 (억)")?.dropWhile { it == '0' }
    override fun sales() = getAttribute("매출액")
    override fun operatingProfit() = getAttribute("영업이익")

    override fun shortSellingOverheating() = getAttribute("공매도과열종목여부") == "Y"
    override fun unusualPriceSurge() = getAttribute("이상급등종목여부") == "Y"



    override fun kospi() = false
    override fun kosdaq() = true
}
