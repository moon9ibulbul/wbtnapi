package com.astral.wbtn

import com.astral.wbtn.crypto.WebtoonCrypto
import com.astral.wbtn.ui.RangeParser
import org.junit.Assert.*
import org.junit.Test

class AppUnitTest {

    @Test
    fun testDeviceKeyGeneration() {
        val key1 = WebtoonCrypto.generateDeviceKey()
        val key2 = WebtoonCrypto.generateDeviceKey()
        assertEquals(32, key1.length)
        assertEquals(32, key2.length)
        assertNotEquals(key1, key2)
    }

    @Test
    fun testSignatureBuild() {
        val unsignedUrl = "https://global.apis.naver.com/lineWebtoon/webtoon/getRsaKey?v=1"
        val currentTime = "1791542425046"
        val signedUrl = WebtoonCrypto.buildSignedUrl(unsignedUrl, currentTime)

        assertTrue(signedUrl.contains("msgpad=1791542425046"))
        assertTrue(signedUrl.contains("&md="))
    }

    @Test
    fun testEncryptCredentials() {
        val sessionKey = "Lmu4JvJTjmNClrAJ"
        val email = "test@example.com"
        val password = "secretpassword"
        val nHex = "010001"
        val eHex = "ee760504c77eb4a478a4cf95679f6db96917d17409204b8b801272a3965e79061806dcd7228443717c5673b4cc6c2f6f43f48395eac35baab04a717a4c852c63004b5caf8b97ae38f420665d1dfc25a9ecdfb91826c1337a16be63f798345b896b6c079f0fa6b0505eb565b64ae1914672278932fc12f8ca8a44314120c1b6a558955c49cf0009a984d03fa3d2109f2f67b88555430f70a5c68e6bfd249d5a9edc02b99bb2e5d673ec6ee6d291368e6d63aedc729bda6f9df4a74d330c59554646981c345b87b1950d9b78b71169f81d791985671a3049b4a43e1a66daf3cb2faf124308a720397e3c9dc49cb09c2259744a1f2cd38e1b4dd0f07f9be64c08e9"

        val encrypted = WebtoonCrypto.encryptCredentials(sessionKey, email, password, nHex, eHex)
        assertNotNull(encrypted)
        assertTrue(encrypted.isNotEmpty())
    }

    @Test
    fun testRangeParser() {
        val rangeStr = "1-5, 8, 10-12"
        val parsed = RangeParser.parseRangeString(rangeStr)
        val expected = setOf(1, 2, 3, 4, 5, 8, 10, 11, 12)
        assertEquals(expected, parsed)
    }

    @Test
    fun testRangeParserWithSpacesAndInvalid() {
        val rangeStr = " 1 - 3 , invalid, 7 "
        val parsed = RangeParser.parseRangeString(rangeStr)
        val expected = setOf(1, 2, 3, 7)
        assertEquals(expected, parsed)
    }
}
