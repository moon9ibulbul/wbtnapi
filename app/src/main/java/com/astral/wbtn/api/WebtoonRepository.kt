package com.astral.wbtn.api

import android.content.Context
import com.astral.wbtn.crypto.WebtoonCrypto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebtoonRepository(private val context: Context? = null) {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val gson = Gson()

    private val prefs = context?.getSharedPreferences("wbtn_session", Context.MODE_PRIVATE)

    var neoSes: String = prefs?.getString("neo_ses", "") ?: ""
        private set
    var neoChk: String = prefs?.getString("neo_chk", "") ?: ""
        private set
    var deviceKey: String = prefs?.getString("device_key", "")?.ifEmpty { null }
        ?: WebtoonCrypto.generateDeviceKey().also { key ->
            prefs?.edit()?.putString("device_key", key)?.apply()
        }
        private set

    fun saveSession(ses: String, chk: String) {
        neoSes = ses
        neoChk = chk
        prefs?.edit()
            ?.putString("neo_ses", ses)
            ?.putString("neo_chk", chk)
            ?.putString("device_key", deviceKey)
            ?.apply()
    }

    fun clearSession() {
        neoSes = ""
        neoChk = ""
        prefs?.edit()
            ?.remove("neo_ses")
            ?.remove("neo_chk")
            ?.apply()
    }

    val isLoggedIn: Boolean
        get() = neoSes.isNotEmpty()

    suspend fun getCurrentTime(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://global.apis.naver.com/currentTime")
            .header("User-Agent", WebtoonCrypto.USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch server time")
            return@withContext response.body?.string()?.trim() ?: throw IOException("Empty time response")
        }
    }

    private suspend fun <T> sendRequest(
        unsignedUrl: String,
        method: String = "GET",
        jsonBody: Any? = null,
        typeToken: TypeToken<WebtoonApiResponse<T>>
    ): T? = withContext(Dispatchers.IO) {
        val currTime = getCurrentTime()
        val signedUrl = WebtoonCrypto.buildSignedUrl(unsignedUrl, currTime)

        val requestBuilder = Request.Builder()
            .url(signedUrl)
            .header("User-Agent", WebtoonCrypto.USER_AGENT)
            .header("wtu", deviceKey)
            .header("Content-Length", "0")

        if (neoSes.isNotEmpty() && neoChk.isNotEmpty()) {
            requestBuilder.header("Cookie", "NEO_SES=\"$neoSes\"; NEO_CHK=\"$neoChk\"")
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                val bodyStr = if (jsonBody != null) gson.toJson(jsonBody) else ""
                requestBuilder.post(bodyStr.toRequestBody(jsonMediaType))
            }
            "PUT" -> {
                val bodyStr = if (jsonBody != null) gson.toJson(jsonBody) else ""
                requestBuilder.put(bodyStr.toRequestBody(jsonMediaType))
            }
            "DELETE" -> requestBuilder.delete()
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseText = response.body?.string() ?: ""
            val parsed = gson.fromJson<WebtoonApiResponse<T>>(responseText, typeToken.type)
            return@withContext parsed?.message?.result
        }
    }

    suspend fun login(email: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        val rsaUrl = "https://global.apis.naver.com/lineWebtoon/webtoon/getRsaKey?v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val rsaType = object : TypeToken<WebtoonApiResponse<RsaKeyResult>>() {}
        val rsaKeys = sendRequest(rsaUrl, "GET", null, rsaType) ?: return@withContext false

        val encryptedPw = WebtoonCrypto.encryptCredentials(
            rsaKeys.sessionKey, email, pass, rsaKeys.nvalue, rsaKeys.evalue
        )

        val loginUrl = "https://global.apis.naver.com/lineWebtoon/webtoon/loginById?method=POST&serviceZone=GLOBAL&encpw=$encryptedPw&loginType=EMAIL&v=3&language=en&encnm=${rsaKeys.keyName}&locale=en&platform=APP_ANDROID"
        val loginType = object : TypeToken<WebtoonApiResponse<Map<String, Any>>>() {}
        val loginResp = sendRequest(loginUrl, "POST", null, loginType)

        val ses = loginResp?.get("ses")?.toString() ?: ""
        if (ses.isEmpty()) return@withContext false

        var chk = ""
        val devInfoUrl = "https://global.apis.naver.com/lineWebtoon/webtoon/setDeviceInfo?deviceKey=$deviceKey&appType=LINEWEBTOON&pushToken=&pushCode=FCMV1&serviceZone=GLOBAL&v=1&language=en&locale=en&platform=APP_ANDROID"

        val currTime = getCurrentTime()
        val signedUrl = WebtoonCrypto.buildSignedUrl(devInfoUrl, currTime)
        val devReq = Request.Builder()
            .url(signedUrl)
            .header("User-Agent", WebtoonCrypto.USER_AGENT)
            .header("wtu", deviceKey)
            .header("Cookie", "NEO_SES=\"$ses\"")
            .get()
            .build()

        client.newCall(devReq).execute().use { resp ->
            val headers = resp.headers
            val setCookies = headers.values("Set-Cookie")
            for (c in setCookies) {
                if (c.contains("NEO_CHK=")) {
                    val start = c.indexOf("NEO_CHK=\"")
                    if (start != -1) {
                        val sub = c.substring(start + 9)
                        val end = sub.indexOf("\"")
                        if (end != -1) chk = sub.substring(0, end)
                    }
                }
            }
        }

        saveSession(ses, chk)
        return@withContext true
    }

    suspend fun getMemberInfo(): MemberInfoResult? {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/getMemberInfo?v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<MemberInfoResult>>() {}
        return sendRequest(url, "GET", null, type)
    }

    suspend fun getCoinBalance(): Int {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/coinBalance?v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<CoinBalanceWrapper>>() {}
        val res = sendRequest(url, "GET", null, type)
        return res?.balance?.amount ?: -1
    }

    suspend fun titleInfo(titleNo: Int): TitleInfoDetail? {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/titleInfo?titleNo=$titleNo&v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<TitleInfoWrapper>>() {}
        return sendRequest(url, "GET", null, type)?.titleInfo
    }

    suspend fun productRightList(titleNo: Int): List<ProductRightItem> {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/productRightList?titleNo=$titleNo&v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<ProductRightListWrapper>>() {}
        return sendRequest(url, "GET", null, type)?.rightList ?: emptyList()
    }

    suspend fun episodeList(titleNo: Int, totalEpisodes: Int): List<EpisodeItem> {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/episodeList?titleNo=$titleNo&startIndex=0&pageSize=$totalEpisodes&v=6&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<EpisodeListWrapper>>() {}
        return sendRequest(url, "GET", null, type)?.episodeList?.episode ?: emptyList()
    }

    suspend fun getProduct(titleNo: Int, episodeNo: Int): ProductDetail? {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/getProduct?titleNo=$titleNo&episodeNo=$episodeNo&v=3&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<ProductWrapper>>() {}
        return sendRequest(url, "GET", null, type)?.product
    }

    suspend fun productRight(titleNo: Int, episodeNo: Int): ProductRightItem? {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/productRight?titleNo=$titleNo&episodeNo=$episodeNo&v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<ProductRightItem>>() {}
        return sendRequest(url, "GET", null, type)
    }

    suspend fun getImageSecureToken() {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/getImageSecureToken?v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<Any>>() {}
        sendRequest(url, "GET", null, type)
    }

    suspend fun buyProduct(productId: String, saleUnitId: String, price: Int) {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/buyProduct?v=1&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val body = mapOf(
            "method" to "POST",
            "productId" to productId,
            "productSaleUnitId" to saleUnitId,
            "price" to price
        )
        val type = object : TypeToken<WebtoonApiResponse<Any>>() {}
        sendRequest(url, "POST", body, type)
    }

    suspend fun episodeInfoWithLogin(titleNo: Int, episodeNo: Int): EpisodeInfoDetail? {
        val url = "https://global.apis.naver.com/lineWebtoon/webtoon/episodeInfoWithLogin?titleNo=$titleNo&episodeNo=$episodeNo&v=4&serviceZone=GLOBAL&language=en&locale=en&platform=APP_ANDROID"
        val type = object : TypeToken<WebtoonApiResponse<EpisodeInfoWrapper>>() {}
        return sendRequest(url, "GET", null, type)?.episodeInfo
    }

    suspend fun getStaticContent(imageUrl: String): ByteArray = withContext(Dispatchers.IO) {
        val fullUrl = if (imageUrl.startsWith("http")) imageUrl else "https://webtoon-phinf.pstatic.net$imageUrl"
        val request = Request.Builder()
            .url(fullUrl)
            .header("Referer", "http://m.webtoons.com/")
            .header("User-Agent", WebtoonCrypto.USER_AGENT)
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to download image: ${resp.code}")
            return@withContext resp.body?.bytes() ?: throw IOException("Empty image content")
        }
    }
}
