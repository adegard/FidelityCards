package com.example.fidelitycards.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles Catima backup format: a zip file containing catima.csv plus
 * optional card_N_front.png / card_N_back.png / card_N_icon.png images.
 *
 * CSV header (Catima format):
 * _id,store,note,validfrom,expiry,balance,balancetype,cardid,barcodeid,barcodetype,headercolor,starstatus,lastused,archive
 */
object CatimaCodec {

    val SUPPORTED_BARCODE_TYPES = setOf(
        "CODE_128", "EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_39", "CODE_93",
        "ITF", "QR_CODE", "PDF_417", "DATA_MATRIX", "AZTEC", "CODABAR", "PHARMACODE"
    )

    // ---- Import ----

    fun importFromFile(
        context: Context,
        zipFile: File,
        progress: (String) -> Unit
    ): ImportResult {
        // 1. Extract to a temp dir
        val tempDir = File(context.cacheDir, "catima_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val imageFiles = mutableMapOf<Long, MutableMap<String, File>>() // id -> (role -> file)
        var csvFile: File? = null

        try {
            ZipInputStream(zipFile.inputStream().buffered()).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val baseName = name.substringAfterLast('/')
                    if (!entry.isDirectory && baseName.equals("catima.csv", ignoreCase = true)) {
                        csvFile = File(tempDir, "catima.csv")
                        csvFile!!.outputStream().use { out -> zin.copyTo(out) }
                    } else if (!entry.isDirectory) {
                        // card_N_front/back/icon.png
                        val match = Regex("""card_(\d+)_(front|back|icon)\.png""", RegexOption.IGNORE_CASE)
                            .find(baseName)
                        if (match != null) {
                            val id = match.groupValues[1].toLong()
                            val role = match.groupValues[2].lowercase()
                            val outFile = File(tempDir, baseName)
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out -> zin.copyTo(out) }
                            imageFiles.getOrPut(id) { mutableMapOf() }[role] = outFile
                        }
                    }
                    entry = zin.nextEntry
                }
            }

            val csv = csvFile
            if (csv == null) {
                return ImportResult(0, 0, error = "catima.csv not found in archive")
            }

            val parsed = parseCsv(csv.readText())
            val added = mutableListOf<FidelityCard>()

            for (p in parsed) {
                progress("Importing ${p.store}...")
                val card = FidelityCard(
                    id = p.id,
                    store = p.store,
                    note = p.note,
                    cardId = p.cardId,
                    barcodeType = p.barcodeType,
                    headerColor = p.headerColor,
                    balance = p.balance,
                    lastUsed = p.lastUsed
                )
                // Copy images into persistent app dir
                val imgs = imageFiles[p.id]
                imgs?.get("front")?.let { card.stripeImagePath = copyImage(context, it) }
                imgs?.get("back")?.let { card.extraImagePath = copyImage(context, it) }
                imgs?.get("icon")?.let { card.iconImagePath = copyImage(context, it) }
                added.add(card)
            }

            val count = CardStore.get(context).addAll(added)
            return ImportResult(count, imageFiles.size, null)
        } catch (e: Exception) {
            return ImportResult(0, 0, error = e.message ?: "Import failed")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun copyImage(context: Context, src: File): String {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val dest = File(dir, src.name)
        src.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    private fun parseCsv(text: String): List<ParsedRow> {
        val result = mutableListOf<ParsedRow>()
        // Find header line
        val lines = text.trim().split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        var startIdx = 0
        for ((idx, line) in lines.withIndex()) {
            if (line.startsWith("_id")) { startIdx = idx; break }
        }
        if (startIdx >= lines.size) return result

        for (i in startIdx + 1 until lines.size) {
            val fields = parseLine(lines[i])
            if (fields.size < 8) continue
            val row = ParsedRow(
                id = fields.getOrNull(0)?.toLongOrNull() ?: 0L,
                store = fields.getOrNull(1) ?: "",
                note = fields.getOrNull(2) ?: "",
                cardId = fields.getOrNull(7) ?: "",
                barcodeType = fields.getOrNull(9) ?: "",
                headerColor = fields.getOrNull(10)?.toIntOrNull() ?: -416706,
                balance = fields.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
                lastUsed = fields.getOrNull(12)?.toLongOrNull() ?: 0L
            )
            result.add(row)
        }
        return result
    }

    /** Simple CSV line parser that respects double-quoted fields. */
    private fun parseLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            current.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        current.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    out.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        out.add(current.toString())
        return out
    }

    private data class ParsedRow(
        val id: Long,
        val store: String,
        val note: String,
        val cardId: String,
        val barcodeType: String,
        val headerColor: Int,
        val balance: Double,
        val lastUsed: Long
    )

    data class ImportResult(
        val imported: Int,
        val imagesImported: Int,
        val error: String?
    )

    // ---- Export ----

    fun exportToFile(
        context: Context,
        destinationFile: File,
        includeImages: Boolean,
        progress: (String) -> Unit
    ): String? {
        try {
            val cards = CardStore.get(context).getAll()
            ZipOutputStream(FileOutputStream(destinationFile)).use { zos ->
                // Write catima.csv
                val csv = buildCsv(cards)
                zos.putNextEntry(ZipEntry("catima.csv"))
                zos.write(csv.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write images
                if (includeImages) {
                    for (c in cards) {
                        progress("Exporting ${c.store}...")
                        c.stripeImagePath?.let { path ->
                            val f = File(path)
                            if (f.exists()) writeZipImage(zos, "card_${c.id}_front.png", f)
                        }
                        c.extraImagePath?.let { path ->
                            val f = File(path)
                            if (f.exists()) writeZipImage(zos, "card_${c.id}_back.png", f)
                        }
                        c.iconImagePath?.let { path ->
                            val f = File(path)
                            if (f.exists()) writeZipImage(zos, "card_${c.id}_icon.png", f)
                        }
                    }
                }
            }
            return null
        } catch (e: Exception) {
            return e.message
        }
    }

    private fun writeZipImage(zos: ZipOutputStream, name: String, file: File) {
        zos.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun buildCsv(cards: List<FidelityCard>): String {
        val sb = StringBuilder()
        sb.append(FidelityCard.CSV_HEADER).append('\n')
        for (c in cards) {
            sb
                .append(c.id).append(',')
                .append(escapeCsv(c.store)).append(',')
                .append(escapeCsv(c.note)).append(',')
                .append(',').append(',')                    // validfrom, expiry
                .append(formatBalance(c.balance)).append(',')
                .append(',')                                 // balancetype
                .append(escapeCsv(c.cardId)).append(',')
                .append(',')                                 // barcodeid (unused)
                .append(escapeCsv(c.barcodeType)).append(',')
                .append(c.headerColor).append(',')
                .append('0').append(',')                     // starstatus
                .append(c.lastUsed).append(',')
                .append('0')                                 // archive
                .append('\n')
        }
        return sb.toString()
    }

    private fun formatBalance(b: Double): String {
        val v = b.toLong()
        return if (b == v.toDouble()) v.toString() else b.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
