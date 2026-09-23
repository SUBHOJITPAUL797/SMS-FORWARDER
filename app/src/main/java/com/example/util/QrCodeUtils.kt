package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class QrPairingData(
    val code: String,
    val phoneNumber: String? = null
)

object QrCodeUtils {

    /**
     * Generates a QR code Bitmap for the given content string.
     * @param content The string to encode (e.g. "smsbridge://pair/XXXX")
     * @param size Pixel size of the output bitmap (square)
     * @param fgColor Foreground (module) color — defaults to black
     * @param bgColor Background color — defaults to white
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) fgColor else bgColor)
            }
        }
        return bitmap
    }

    /** Deep-link URI scheme used for QR pairing */
    const val QR_SCHEME = "smsbridge://pair/"

    fun encodeCode(code: String, phoneNumber: String? = null): String {
        val cleanCode = code.trim().uppercase()
        val cleanPhone = phoneNumber?.filter { it.isDigit() || it == '+' }?.trim()
        return if (!cleanPhone.isNullOrBlank()) {
            "$QR_SCHEME$cleanCode?phone=${Uri.encode(cleanPhone)}"
        } else {
            "$QR_SCHEME$cleanCode"
        }
    }

    /** Parses a scanned QR raw value → pairing code or null if format is wrong */
    fun parseScannedQr(rawValue: String): String? {
        return parseScannedQrData(rawValue)?.code
    }

    /** Parses a scanned QR raw value → QrPairingData with code and optional phone number */
    fun parseScannedQrData(rawValue: String): QrPairingData? {
        if (rawValue.startsWith(QR_SCHEME)) {
            val payload = rawValue.removePrefix(QR_SCHEME).trim()
            val parts = payload.split("?")
            val code = parts[0].trim().uppercase().take(6)
            if (code.length != 6 || !code.all { it.isLetterOrDigit() }) return null
            var phone: String? = null
            if (parts.size > 1) {
                val queryParams = parts[1].split("&")
                for (p in queryParams) {
                    val kv = p.split("=")
                    if (kv.size == 2 && kv[0].equals("phone", ignoreCase = true)) {
                        phone = Uri.decode(kv[1]).trim().ifBlank { null }
                    }
                }
            }
            return QrPairingData(code, phone)
        } else {
            // Fallback: maybe plain 6-char code
            val clean = rawValue.trim().uppercase()
            return if (clean.length == 6 && clean.all { it.isLetterOrDigit() }) {
                QrPairingData(clean, null)
            } else null
        }
    }
}
