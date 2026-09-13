package com.example

import com.example.util.OtpExtractor
import org.junit.Assert.*
import org.junit.Test

class OtpExtractorTest {

    @Test
    fun extractOtp_standardIndianBankFormat() {
        val body = "Dear Customer, 529757 is your OTP for transaction of INR 500 at Amazon. Do not share this with anyone."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("529757", result.otp)
        assertEquals("5  2  9  7  5  7", result.formattedOtp)
    }

    @Test
    fun extractOtp_prefixOtpFormat() {
        val body = "Your OTP is 492019. Valid for 10 minutes."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("492019", result.otp)
        assertEquals("4  9  2  0  1  9", result.formattedOtp)
    }

    @Test
    fun extractOtp_verificationCodeFormat() {
        val body = "Use verification code 839201 to verify your WhatsApp account."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("839201", result.otp)
    }

    @Test
    fun extractOtp_googleServiceFormat() {
        val body = "G-482910 is your Google verification code."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("482910", result.otp)
    }

    @Test
    fun extractOtp_pinFormat() {
        val body = "Your security PIN is 7492. Do not disclose."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("7492", result.otp)
        assertEquals("7  4  9  2", result.formattedOtp)
    }

    @Test
    fun extractOtp_useCodeFormat() {
        val body = "Please use 192834 to log in to your account."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("192834", result.otp)
    }

    @Test
    fun extractOtp_ignoresAmountsAndDates() {
        val body = "Rs. 5000 debited from A/C 9876 on 12/09/2026. Available balance: INR 12500."
        val result = OtpExtractor.extractOtp(body)
        assertFalse(result.isOtp)
        assertEquals("", result.otp)
    }

    @Test
    fun extractOtp_mixedWithAmountAndDate() {
        val body = "SBI: INR 4,500.00 debited for txn on 12-Sep-26. OTP for verification is 849201. Do not share."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("849201", result.otp)
    }

    @Test
    fun extractOtp_withMobileNumber() {
        val body = "Dear user, OTP for your mobile 9876543210 is 492102. Valid for 5 mins."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("492102", result.otp)
    }

    @Test
    fun extractOtp_eightDigitOtp() {
        val body = "Your security code is: 82910482. Use this to login."
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("82910482", result.otp)
    }

    @Test
    fun extractOtp_emptyOrBlank() {
        val r1 = OtpExtractor.extractOtp("")
        val r2 = OtpExtractor.extractOtp(null)
        val r3 = OtpExtractor.extractOtp("Hello, how are you doing today?")
        val r4 = OtpExtractor.extractOtp("Call me at +918927408840")
        assertFalse(r1.isOtp)
        assertFalse(r2.isOtp)
        assertFalse(r3.isOtp)
        assertFalse(r4.isOtp)
    }

    @Test
    fun extractOtp_userSpecificMessageWithTopTypo() {
        val body = "hi your top is this 456789"
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("456789", result.otp)
        assertEquals("4  5  6  7  8  9", result.formattedOtp)
    }

    @Test
    fun extractOtp_userSpecificMessageStandaloneSixDigits() {
        val body = "hi 456456"
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("456456", result.otp)
        assertEquals("4  5  6  4  5  6", result.formattedOtp)
    }

    @Test
    fun extractOtp_pureDigits() {
        val body = "456456"
        val result = OtpExtractor.extractOtp(body)
        assertTrue(result.isOtp)
        assertEquals("456456", result.otp)
    }

    @Test
    fun extractOtp_topSuffixAndPrefixVariants() {
        val r1 = OtpExtractor.extractOtp("456789 is your top")
        assertTrue(r1.isOtp)
        assertEquals("456789", r1.otp)

        val r2 = OtpExtractor.extractOtp("top: 456789")
        assertTrue(r2.isOtp)
        assertEquals("456789", r2.otp)

        val r3 = OtpExtractor.extractOtp("your top 456789")
        assertTrue(r3.isOtp)
        assertEquals("456789", r3.otp)
    }
}
