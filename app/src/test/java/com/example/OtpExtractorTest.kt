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
    fun extractOtp_emptyOrBlank() {
        val r1 = OtpExtractor.extractOtp("")
        val r2 = OtpExtractor.extractOtp(null)
        val r3 = OtpExtractor.extractOtp("Hello, how are you doing today?")
        assertFalse(r1.isOtp)
        assertFalse(r2.isOtp)
        assertFalse(r3.isOtp)
    }
}
