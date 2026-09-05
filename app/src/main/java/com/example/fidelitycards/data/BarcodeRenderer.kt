package com.example.fidelitycards.data

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object BarcodeRenderer {

    fun formatFor(type: String, oid: String): BarcodeFormat? {
        val t = type.trim().uppercase()
        val id = oid.trim().uppercase()
        return when {
            t == "QR_CODE" -> BarcodeFormat.QR_CODE
            t == "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
            t == "AZTEC" -> BarcodeFormat.AZTEC
            t == "PDF_417" -> BarcodeFormat.PDF_417
            t == "EAN_13" || id.startsWith("EAN-13") -> BarcodeFormat.EAN_13
            t == "EAN_8" || id.startsWith("EAN-8") -> BarcodeFormat.EAN_8
            t == "UPC_A" || id.startsWith("UPC_A") || id.startsWith("UPC-A") -> BarcodeFormat.UPC_A
            t == "UPC_E" || id.startsWith("UPC_E") || id.startsWith("UPC-E") -> BarcodeFormat.UPC_E
            t == "CODE_39" || id.startsWith("Code 39") -> BarcodeFormat.CODE_39
            t == "CODE_93" -> BarcodeFormat.CODE_93
            t == "ITF" || t == "INTERLEAVED_2_OF_5" -> BarcodeFormat.ITF
            t == "CODABAR" -> BarcodeFormat.CODABAR
            // default
            t.isNotEmpty() -> BarcodeFormat.CODE_128
            else -> null
        }
    }

    /** Generate a barcode bitmap with white background and black bars.
     *  width/height in pixels. QR codes are filled to square. */
    fun generate(
        content: String,
        type: String,
        width: Int = 800,
        height: Int = 300
    ): Bitmap? {
        val barcodeFormat = formatFor(type, content) ?: return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.MARGIN] = 1
            val matrix: BitMatrix = when (barcodeFormat) {
                BarcodeFormat.QR_CODE -> {
                    QRCodeWriter().encode(content, barcodeFormat, 0, 0, hints)
                }
                else -> {
                    MultiFormatWriter().encode(content, barcodeFormat, width, height, hints)
                }
            }
            render(matrix)
        } catch (e: Exception) {
            null
        }
    }

    private fun render(matrix: BitMatrix): Bitmap {
        val width = maxOf(matrix.width, 1)
        val height = maxOf(matrix.height, 1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun displayName(type: String): String {
        return when (type.trim().uppercase()) {
            "CODE_128" -> "Code 128"
            "CODE_39" -> "Code 39"
            "CODE_93" -> "Code 93"
            "EAN_13" -> "EAN-13"
            "EAN_8" -> "EAN-8"
            "UPC_A" -> "UPC-A"
            "UPC_E" -> "UPC-E"
            "ITF" -> "ITF"
            "QR_CODE" -> "QR Code"
            "PDF_417" -> "PDF-417"
            "DATA_MATRIX" -> "Data Matrix"
            "AZTEC" -> "Aztec"
            "CODABAR" -> "Codabar"
            else -> type
        }
    }
}
