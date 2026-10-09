package com.astral.wbtn.crypto

import java.util.Base64
import java.math.BigInteger
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebtoonCrypto {

    val API_KEY = "gUtPzJFZch4ZyAGviiyH94P99lQ3pFdRTwpJWDlSGFfwgpr6ses5ALOxWHOIT7R1".toByteArray(Charsets.UTF_8)
    const val USER_AGENT = "nApps (Android 14; SM-A236B; linewebtoon; 3.3.2)"
    private const val DEVICE_KEY_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

    fun generateDeviceKey(): String {
        return (1..32)
            .map { DEVICE_KEY_CHARS.random() }
            .joinToString("")
    }

    fun generateSignature(url: String, currentTime: String): String {
        val urlBytes = url.toByteArray(Charsets.UTF_8)
        val timeBytes = currentTime.toByteArray(Charsets.UTF_8)

        val prefixLength = if (urlBytes.size > 255) 255 else urlBytes.size
        val dataToSign = ByteArray(prefixLength + timeBytes.size)

        System.arraycopy(urlBytes, 0, dataToSign, 0, prefixLength)
        System.arraycopy(timeBytes, 0, dataToSign, prefixLength, timeBytes.size)

        val sha1Hmac = Mac.getInstance("HmacSHA1")
        val secretKey = SecretKeySpec(API_KEY, "HmacSHA1")
        sha1Hmac.init(secretKey)

        val macData = sha1Hmac.doFinal(dataToSign)
        return Base64.getEncoder().encodeToString(macData).trim()
    }

    fun buildSignedUrl(unsignedUrl: String, currentTime: String): String {
        val signature = generateSignature(unsignedUrl, currentTime)
        val encodedSig = URLEncoder.encode(signature, "UTF-8")
        val separator = if (unsignedUrl.contains("?")) "&" else "?"
        return "$unsignedUrl${separator}msgpad=$currentTime&md=$encodedSig"
    }

    fun encryptCredentials(
        sessionKey: String,
        email: String,
        password: String,
        nHex: String,
        eHex: String
    ): String {
        val message = "${sessionKey.length.toChar()}$sessionKey${email.length.toChar()}$email${password.length.toChar()}$password"
        val modulus = BigInteger(nHex, 16)
        val exponent = BigInteger(eHex, 16)

        val keySpec = RSAPublicKeySpec(modulus, exponent)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return encryptedBytes.joinToString("") { "%02x".format(it) }
    }
}
