package com.example.util

import java.util.regex.Pattern

data class OtpResult(
    val otp: String,
    val formattedOtp: String,
    val isOtp: Boolean
)

object OtpExtractor {

    // Regex for checking if text relates to verification / OTP (includes common typos like 'top')
    private val OTP_KEYWORD_PATTERN = Pattern.compile(
        "(?i)\\b(otp|top|o[.-]?t[.-]?p|t[.-]?o[.-]?p|code|kod|verification|verify|verif|pin|password|passcode|pass|secret|one[- ]?time|tac|v-code|auth|2fa|mfa|valid\\s+for)\\b"
    )

    // Pattern 1: Keyword followed by number within up to 35 chars
    // e.g. "OTP is 529757", "your top is this 456789", "code: 123456", "PIN is 8291", "verification code is 492019"
    private val PREFIX_PATTERN = Pattern.compile(
        "(?i)\\b(?:otp|top|o[.-]?t[.-]?p|t[.-]?o[.-]?p|code|kod|pin|verification(?:\\s+code)?|password|passcode|pass|secret|tac|auth|v-code|2fa)\\b[^0-9\\r\\n]{0,35}?([0-9]{4,8})\\b"
    )

    // Pattern 2: Number followed by keyword within up to 35 chars
    // e.g. "529757 is your OTP", "456789 is your top", "492019 is the verification code"
    private val SUFFIX_PATTERN = Pattern.compile(
        "\\b([0-9]{4,8})\\b[^0-9\\r\\n]{0,35}?(?i)\\b(?:otp|top|o[.-]?t[.-]?p|t[.-]?o[.-]?p|code|kod|pin|verification(?:\\s+code)?|password|passcode|secret|one[- ]?time)\\b"
    )

    // Pattern 3: Standard service prefix formats like G-482910 or VK-123456
    private val SERVICE_PREFIX_PATTERN = Pattern.compile(
        "\\b[A-Za-z]-([0-9]{4,8})\\b"
    )

    // Pattern 4: "Use 123456 to verify/login"
    private val USE_CODE_PATTERN = Pattern.compile(
        "(?i)\\b(?:use|enter|with)\\s+([0-9]{4,8})\\b"
    )

    // General 4-8 digit standalone number
    private val STANDALONE_NUMBER_PATTERN = Pattern.compile(
        "\\b([0-9]{4,8})\\b"
    )

    // Disqualification patterns for amounts, accounts, dates, pincodes
    private val CURRENCY_PRECEDING_PATTERN = Pattern.compile(
        "(?i)(?:rs\\.?|inr|₹|\\$|usd|eur|aud|gbp|amount|debited|credited|balance|paid|spent)[\\s:]*$"
    )

    private val ACCOUNT_PRECEDING_PATTERN = Pattern.compile(
        "(?i)(?:a/c|acct|account|card|ending(?:\\s+with)?|ending\\s+in|xx|xx\\*)[\\s:]*$"
    )

    private val PINCODE_PRECEDING_PATTERN = Pattern.compile(
        "(?i)(?:pin\\s*code|pincode|zip\\s*code|zip|postal)[\\s:]*$"
    )

    fun extractOtp(messageBody: String?): OtpResult {
        if (messageBody.isNullOrBlank()) {
            return OtpResult(otp = "", formattedOtp = "", isOtp = false)
        }

        val cleanBody = messageBody.trim()

        // 1. Try Prefix Pattern (e.g. "OTP is 529757", "your top is this 456789")
        val prefixMatcher = PREFIX_PATTERN.matcher(cleanBody)
        if (prefixMatcher.find()) {
            val code = prefixMatcher.group(1)
            if (code != null && isValidOtpCode(code, cleanBody, prefixMatcher.start(1))) {
                return buildResult(code)
            }
        }

        // 2. Try Suffix Pattern (e.g. "529757 is your OTP", "456789 is your top")
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

        // 5. If message contains OTP keywords, find any valid standalone 4-8 digit number
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

        // 6. Fallback for casual / standalone verification numbers (e.g. "hi 456456", "456456")
        val standaloneMatcher = STANDALONE_NUMBER_PATTERN.matcher(cleanBody)
        val candidates = mutableListOf<String>()
        while (standaloneMatcher.find()) {
            val candidate = standaloneMatcher.group(1)
            val start = standaloneMatcher.start(1)
            if (candidate != null && isValidOtpCode(candidate, cleanBody, start)) {
                candidates.add(candidate)
            }
        }

        if (candidates.isNotEmpty()) {
            // Prioritize standard 6-digit OTPs
            val sixDigit = candidates.firstOrNull { it.length == 6 }
            if (sixDigit != null) {
                return buildResult(sixDigit)
            }
            // If message is short (e.g. casual text <= 120 chars) and has exactly 1 candidate
            if (candidates.size == 1 && cleanBody.length <= 120) {
                return buildResult(candidates.first())
            }
        }

        return OtpResult(otp = "", formattedOtp = "", isOtp = false)
    }

    private fun isValidOtpCode(code: String?, text: String, startIdx: Int): Boolean {
        if (code == null || code.length !in 4..8) return false

        // Filter out obvious years (2020..2035) if exactly 4 digits unless explicitly accompanied by otp/pin/code
        if (code.length == 4) {
            val yearVal = code.toIntOrNull()
            if (yearVal != null && yearVal in 2020..2035) {
                val surrounding = text.substring(
                    maxOf(0, startIdx - 30),
                    minOf(text.length, startIdx + code.length + 30)
                ).lowercase()
                if (!surrounding.contains("otp") && !surrounding.contains("top") && !surrounding.contains("pin") && !surrounding.contains("code")) {
                    return false
                }
            }
        }

        // Check text immediately before this number for currency, account, or pincode signs
        val precedingSnippet = text.substring(maxOf(0, startIdx - 20), startIdx).trim()
        if (CURRENCY_PRECEDING_PATTERN.matcher(precedingSnippet).find()) {
            return false
        }
        if (ACCOUNT_PRECEDING_PATTERN.matcher(precedingSnippet).find()) {
            return false
        }
        if (PINCODE_PRECEDING_PATTERN.matcher(precedingSnippet).find()) {
            return false
        }

        // Check if preceded or followed by time/date colon or slashes (e.g. "14:15" or "12/09/2026")
        if (startIdx > 0 && (text[startIdx - 1] == ':' || text[startIdx - 1] == '/')) {
            return false
        }
        val endIdx = startIdx + code.length
        if (endIdx < text.length && (text[endIdx] == ':' || text[endIdx] == '/')) {
            return false
        }

        // Check if followed by decimals (e.g. 5000.00)
        if (endIdx < text.length && text[endIdx] == '.') {
            val next2 = text.substring(endIdx, minOf(text.length, endIdx + 3))
            if (next2.matches(Regex("\\.\\d{2}"))) {
                return false
            }
        }

        // Check if immediately followed by currency symbols (e.g. 500/-, 5000 inr)
        val succeedingSnippet = text.substring(endIdx, minOf(text.length, endIdx + 15)).trim().lowercase()
        if (succeedingSnippet.startsWith("/-") || succeedingSnippet.startsWith("/=") ||
            succeedingSnippet.startsWith("inr") || succeedingSnippet.startsWith("rs") ||
            succeedingSnippet.startsWith("₹") || succeedingSnippet.startsWith("$")) {
            return false
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
