package com.example.util

import java.util.regex.Pattern

data class OtpResult(
    val otp: String,
    val formattedOtp: String,
    val isOtp: Boolean
)

object OtpExtractor {

    // Regex for checking if text relates to verification / OTP
    private val OTP_KEYWORD_PATTERN = Pattern.compile(
        "(?i)\\b(otp|code|verification|verify|pin|password|passcode|secret|one[- ]?time|tac|v-code|auth|valid\\s+for)\\b"
    )

    // Pattern 1: Explicit keyword followed by number (e.g. "OTP is 529757", "code: 123456", "PIN is 8291")
    private val PREFIX_PATTERN = Pattern.compile(
        "(?i)(?:otp|code|pin|verification(?:\\s+code)?|password|passcode|secret|tac|auth|v-code)(?:\\s+(?:is|for|to|as))?[\\s:=#-]+([0-9]{4,8})\\b"
    )

    // Pattern 2: Number followed by keyword (e.g. "529757 is your OTP", "492019 is the verification code")
    private val SUFFIX_PATTERN = Pattern.compile(
        "\\b([0-9]{4,8})\\b\\s*(?:is\\s+(?:your|the)?\\s*)?(?i)(?:otp|code|pin|verification(?:\\s+code)?|password|passcode|secret|one[- ]?time)"
    )

    // Pattern 3: Standard prefix formats like G-482910 or VK-123456
    private val SERVICE_PREFIX_PATTERN = Pattern.compile(
        "\\b[A-Za-z]-([0-9]{4,8})\\b"
    )

    // Pattern 4: "Use 123456 to verify/login"
    private val USE_CODE_PATTERN = Pattern.compile(
        "(?i)\\b(?:use|enter|with)\\s+([0-9]{4,8})\\s+(?:to|for|as)\\b"
    )

    // General 4-8 digit standalone number
    private val STANDALONE_NUMBER_PATTERN = Pattern.compile(
        "\\b([0-9]{4,8})\\b"
    )

    // Disqualification patterns for amounts, accounts, dates
    private val CURRENCY_PRECEDING_PATTERN = Pattern.compile(
        "(?i)(?:rs\\.?|inr|₹|\\$|usd|eur|aud|gbp|amount|debited|credited|balance)[\\s:]*$"
    )

    private val ACCOUNT_PRECEDING_PATTERN = Pattern.compile(
        "(?i)(?:a/c|acct|account|card|ending(?:\\s+with)?)[\\s:]*$"
    )

    fun extractOtp(messageBody: String?): OtpResult {
        if (messageBody.isNullOrBlank()) {
            return OtpResult(otp = "", formattedOtp = "", isOtp = false)
        }

        val cleanBody = messageBody.trim()

        // 1. Try Prefix Pattern (e.g. "OTP is 529757", "Verification code: 492019")
        val prefixMatcher = PREFIX_PATTERN.matcher(cleanBody)
        if (prefixMatcher.find()) {
            val code = prefixMatcher.group(1)
            if (code != null && isValidOtpCode(code, cleanBody, prefixMatcher.start(1))) {
                return buildResult(code)
            }
        }

        // 2. Try Suffix Pattern (e.g. "529757 is your OTP")
        val suffixMatcher = SUFFIX_PATTERN.matcher(cleanBody)
        if (suffixMatcher.find()) {
            val code = suffixMatcher.group(1)
            if (code != null && isValidOtpCode(code, cleanBody, suffixMatcher.start(1))) {
                return buildResult(code)
            }
        }

        // 3. Try Service Prefix Pattern (e.g. "G-482910")
        val serviceMatcher = SERVICE_PREFIX_PATTERN.matcher(cleanBody)
        if (serviceMatcher.find()) {
            val code = serviceMatcher.group(1)
            if (code != null && isValidOtpCode(code, cleanBody, serviceMatcher.start(1))) {
                return buildResult(code)
            }
        }

        // 4. Try "Use 123456 to verify" pattern
        val useMatcher = USE_CODE_PATTERN.matcher(cleanBody)
        if (useMatcher.find()) {
            val code = useMatcher.group(1)
            if (code != null && isValidOtpCode(code, cleanBody, useMatcher.start(1))) {
                return buildResult(code)
            }
        }

        // 5. Fallback: If the message contains OTP keywords, find any standalone 4-8 digit number
        if (OTP_KEYWORD_PATTERN.matcher(cleanBody).find()) {
            val standaloneMatcher = STANDALONE_NUMBER_PATTERN.matcher(cleanBody)
            while (standaloneMatcher.find()) {
                val candidate = standaloneMatcher.group(1)
                val start = standaloneMatcher.start(1)
                if (candidate != null && isValidOtpCode(candidate, cleanBody, start)) {
                    return buildResult(candidate)
                }
            }
        }

        return OtpResult(otp = "", formattedOtp = "", isOtp = false)
    }

    private fun isValidOtpCode(code: String?, text: String, startIdx: Int): Boolean {
        if (code == null || code.length !in 4..8) return false

        // Filter out obvious years (2020..2035) if exactly 4 digits
        if (code.length == 4) {
            val yearVal = code.toIntOrNull()
            if (yearVal != null && yearVal in 2020..2035) {
                // Only consider as OTP if explicitly preceded by "otp" or "pin"
                val preceding50 = text.substring(maxOf(0, startIdx - 30), startIdx).lowercase()
                if (!preceding50.contains("otp") && !preceding50.contains("pin") && !preceding50.contains("code")) {
                    return false
                }
            }
        }

        // Check text immediately before this number for currency or account signs
        val precedingSnippet = text.substring(maxOf(0, startIdx - 20), startIdx).trim()
        if (CURRENCY_PRECEDING_PATTERN.matcher(precedingSnippet).find()) {
            return false
        }
        if (ACCOUNT_PRECEDING_PATTERN.matcher(precedingSnippet).find()) {
            return false
        }

        // Check if immediately followed by decimals (e.g. 5000.00)
        val endIdx = startIdx + code.length
        if (endIdx < text.length && text[endIdx] == '.') {
            val next2 = text.substring(endIdx, minOf(text.length, endIdx + 3))
            if (next2.matches(Regex("\\.\\d{2}"))) {
                return false
            }
        }

        return true
    }

    private fun buildResult(otp: String): OtpResult {
        // Spaced out for prominent display: e.g. "5  2  9  7  5  7"
        val formatted = otp.map { "$it" }.joinToString("  ")
        return OtpResult(
            otp = otp,
            formattedOtp = formatted,
            isOtp = true
        )
    }
}
