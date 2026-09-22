package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

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

    fun encodeCode(code: String): String = "$QR_SCHEME$code"

    /** Parses a scanned QR raw value → pairing code or null if format is wrong */
    fun parseScannedQr(rawValue: String): String? {
        return if (rawValue.startsWith(QR_SCHEME)) {
            rawValue.removePrefix(QR_SCHEME).trim().uppercase().take(6).ifEmpty { null }
        } else {
            // Fallback: maybe the Host scanned a plain 6-char code
            val clean = rawValue.trim().uppercase()
            if (clean.length == 6 && clean.all { it.isLetterOrDigit() }) clean else null
        }
    }
}
