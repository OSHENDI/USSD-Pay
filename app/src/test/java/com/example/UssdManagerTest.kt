package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UssdManagerTest {

    @Test
    fun testNormalizePhone_localFormat() {
        val input = "0599123456"
        val result = UssdManager.normalizePhone(input)
        assertEquals("0599123456", result)
    }

    @Test
    fun testNormalizePhone_internationalPrefix970() {
        val input = "+970599123456"
        val result = UssdManager.normalizePhone(input)
        assertEquals("0599123456", result)
    }

    @Test
    fun testNormalizePhone_internationalPrefix00970() {
        val input = "00970599123456"
        val result = UssdManager.normalizePhone(input)
        assertEquals("0599123456", result)
    }

    @Test
    fun testNormalizePhone_internationalPrefix00972() {
        val input = "00972599123456"
        val result = UssdManager.normalizePhone(input)
        assertEquals("0599123456", result)
    }

    @Test
    fun testNormalizePhone_withDashesAndSpaces() {
        val input = "059-912 34 56"
        val result = UssdManager.normalizePhone(input)
        assertEquals("0599123456", result)
    }

    @Test
    fun testNormalizePhone_missingLeadingZero() {
        val input = "599123456"
        val result = UssdManager.normalizePhone(input)
        assertEquals("0599123456", result)
    }

    @Test
    fun testBuildPaymentString_friend() {
        val ussd = UssdManager.buildPaymentString(
            paymentType = PaymentType.FRIEND,
            pin = "1234",
            phone = "0599123456",
            amount = "50"
        )
        assertEquals("*268*1*0599123456*50*1234#", ussd)
    }

    @Test
    fun testBuildPaymentString_merchant() {
        val ussd = UssdManager.buildPaymentString(
            paymentType = PaymentType.MERCHANT,
            pin = "9876",
            phone = "0598765432",
            amount = "120.5"
        )
        assertEquals("*268*2*0598765432*120.5*9876#", ussd)
    }

    @Test
    fun testBuildBalanceString() {
        val ussd = UssdManager.buildBalanceString()
        assertEquals("*110*3#", ussd)
    }

    @Test
    fun testExtractBalanceAmount_arabicFormat() {
        val raw = "الرصيد الحالي: 67.69 شيكل"
        val extracted = UssdManager.extractBalanceAmount(raw)
        assertEquals("67.69", extracted)
    }

    @Test
    fun testExtractBalanceAmount_englishFormat() {
        val raw = "Current Balance: 150.00 ILS"
        val extracted = UssdManager.extractBalanceAmount(raw)
        assertEquals("150.00", extracted)
    }

    @Test
    fun testExtractBalanceAmount_fallbackNumber() {
        val raw = "Your balance is 42.50 NIS"
        val extracted = UssdManager.extractBalanceAmount(raw)
        assertEquals("42.50", extracted)
    }

    @Test
    fun testTranslateResponse_successEnglish() {
        val raw = "Payment of 50.00 NIS has been successfully transferred"
        val res = UssdManager.translateResponse(raw, AppLanguage.EN)
        assertTrue(res.isSuccess)
        assertFalse(res.isError)
    }

    @Test
    fun testTranslateResponse_successArabic() {
        val raw = "تمت عملية تحويل 50 شيكل بنجاح"
        val res = UssdManager.translateResponse(raw, AppLanguage.AR)
        assertTrue(res.isSuccess)
        assertFalse(res.isError)
    }

    @Test
    fun testTranslateResponse_insufficientBalanceArabic() {
        val raw = "الرصيد غير كافٍ لإتمام العملية"
        val res = UssdManager.translateResponse(raw, AppLanguage.AR)
        assertTrue(res.isError)
        assertFalse(res.isSuccess)
        assertEquals("الرصيد غير كافٍ لإتمام العملية", res.text)
    }
}
