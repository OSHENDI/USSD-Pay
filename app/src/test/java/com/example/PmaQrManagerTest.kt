package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PmaQrManagerTest {

    @Test
    fun testParseQrCode_rawPhoneNumber() {
        val parsed = PmaQrManager.parseQrCode("0599123456")
        assertNotNull(parsed)
        assertEquals("0599123456", parsed?.first)
        assertEquals(PaymentType.FRIEND, parsed?.second)
    }

    @Test
    fun testParseQrCode_pipeDelimitedFriend() {
        val parsed = PmaQrManager.parseQrCode("0599123456|friend")
        assertNotNull(parsed)
        assertEquals("0599123456", parsed?.first)
        assertEquals(PaymentType.FRIEND, parsed?.second)
    }

    @Test
    fun testParseQrCode_pipeDelimitedMerchant() {
        val parsed = PmaQrManager.parseQrCode("0598765432|merchant")
        assertNotNull(parsed)
        assertEquals("0598765432", parsed?.first)
        assertEquals(PaymentType.MERCHANT, parsed?.second)
    }

    @Test
    fun testGenerateAndParsePmaQrPayload_friend() {
        val phone = "0599123456"
        val payload = PmaQrManager.generatePmaQrPayload(phone, PaymentType.FRIEND)
        assertTrue(payload.startsWith("000201"))
        assertTrue(payload.contains("52044444")) // MCC 4444 for Friend

        val parsed = PmaQrManager.parseQrCode(payload)
        assertNotNull(parsed)
        assertEquals(phone, parsed?.first)
        assertEquals(PaymentType.FRIEND, parsed?.second)
    }

    @Test
    fun testGenerateAndParsePmaQrPayload_merchant() {
        val phone = "0598765432"
        val payload = PmaQrManager.generatePmaQrPayload(phone, PaymentType.MERCHANT)
        assertTrue(payload.startsWith("000201"))
        assertTrue(payload.contains("52045411")) // MCC 5411 for Merchant

        val parsed = PmaQrManager.parseQrCode(payload)
        assertNotNull(parsed)
        assertEquals(phone, parsed?.first)
        assertEquals(PaymentType.MERCHANT, parsed?.second)
    }
}
