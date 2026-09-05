package com.example.fidelitycards.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object BarcodeRenderer {

    private const val QR_QUIET_ZONE_MODULES = 4
    private const val LINEAR_QUIET_ZONE_PX = 12

    private val blackPaint = Paint().apply { color = Color.BLACK }
    private val whitePaint = Paint().apply { color = Color.WHITE }

    fun formatFor(type: String, content: String): BarcodeFormat? {
        val t = type.trim().uppercase()
        val id = content.trim().uppercase()
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
            t.isNotEmpty() -> BarcodeFormat.CODE_128
            else -> null
        }
    }

    /** Generate a barcode bitmap with clean white background and sharp black marks.
     *  QR codes are rendered square with a proper quiet zone. */
    fun generate(
        content: String,
        type: String,
        width: Int = 800,
        height: Int = 300
    ): Bitmap? {
        val barcodeFormat = formatFor(type, content) ?: return null
        return try {
            if (barcodeFormat == BarcodeFormat.QR_CODE) {
                val target = minOf(maxOf(width, height), 1200).coerceAtLeast(240)
                renderQr(content, target)
            } else {
                renderLinear(content, barcodeFormat, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun hintMap(): EnumMap<EncodeHintType, Any> {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 2
        return hints
    }

    /** Render a QR code as crisp solid modules at integer scale with a quiet zone. */
    private fun renderQr(content: String, targetSize: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 0 // quiet zone added manually below
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)

        val modules = matrix.width
        val scale = maxOf(1, targetSize / modules)
        val body = modules * scale
        val quiet = QR_QUIET_ZONE_MODULES * scale
        val size = body + quiet * 2

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        for (y in 0 until modules) {
            val top = (quiet + y * scale).toFloat()
            val bottom = top + scale
            for (x in 0 until modules) {
                if (matrix.get(x, y)) {
                    val left = (quiet + x * scale).toFloat()
                    canvas.drawRect(left, top, left + scale, bottom, blackPaint)
                }
            }
        }
        return bmp
    }

    /** Render 1D / other barcodes with a clean white background. */
    private fun renderLinear(content: String, format: BarcodeFormat, width: Int, height: Int): Bitmap {
        val matrix = MultiFormatWriter().encode(content, format, width, height, hintMap())
        val w = matrix.width
        val h = matrix.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (matrix.get(x, y)) bmp.setPixel(x, y, Color.BLACK)
            }
        }
        return bmp
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