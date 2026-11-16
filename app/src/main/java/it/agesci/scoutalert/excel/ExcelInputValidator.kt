package it.agesci.scoutalert.excel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

internal const val MAX_XLSX_BYTES = 10L * 1024L * 1024L
internal const val MAX_XLSX_ENTRIES = 1_024
internal const val MAX_XLSX_UNCOMPRESSED_BYTES = 64L * 1024L * 1024L
internal const val MAX_XLSX_ENTRY_BYTES = 32L * 1024L * 1024L
internal const val MAX_XLSX_EXPANSION_RATIO = 100L

internal object ExcelInputValidator {
    private val acceptedMimeTypes = setOf(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/octet-stream",
        "application/zip"
    )

    fun validate(context: Context, uri: Uri) {
        require(uri.scheme == "content") { "Origine file non supportata" }
        val resolver = context.contentResolver

        resolver.getType(uri)?.let { mimeType ->
            require(mimeType.lowercase() in acceptedMimeTypes) {
                "Il file selezionato non è un documento XLSX"
            }
        }

        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                val size = cursor.getLong(0)
                require(size in 1..MAX_XLSX_BYTES) {
                    "Il file XLSX deve avere dimensioni comprese tra 1 byte e 10 MB"
                }
            }
        }

        val input = resolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Impossibile aprire il file selezionato")
        input.use(::validateXlsxArchive)
    }
}

internal fun validateXlsxArchive(input: InputStream) {
    var entryCount = 0
    var totalUncompressedBytes = 0L
    var hasContentTypes = false
    var hasWorkbook = false
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    ZipInputStream(BufferedInputStream(ByteLimitInputStream(input, MAX_XLSX_BYTES))).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            entryCount++
            require(entryCount <= MAX_XLSX_ENTRIES) {
                "Il file XLSX contiene troppi elementi"
            }

            val normalizedName = entry.name.replace('\\', '/').lowercase()
            require(!normalizedName.startsWith("/") && "../" !in normalizedName) {
                "Il file XLSX contiene percorsi non validi"
            }
            hasContentTypes = hasContentTypes || normalizedName == "[content_types].xml"
            hasWorkbook = hasWorkbook || normalizedName == "xl/workbook.xml"

            var entryBytes = 0L
            while (!entry.isDirectory) {
                val read = zip.read(buffer)
                if (read < 0) break
                entryBytes += read
                totalUncompressedBytes += read
                require(entryBytes <= MAX_XLSX_ENTRY_BYTES) {
                    "Un elemento del file XLSX supera 32 MB"
                }
                require(totalUncompressedBytes <= MAX_XLSX_UNCOMPRESSED_BYTES) {
                    "Il contenuto espanso del file XLSX supera 64 MB"
                }
            }

            val compressedBytes = entry.compressedSize
            if (entryBytes > 0L && compressedBytes >= 0L) {
                require(compressedBytes > 0L) {
                    "Il file XLSX contiene un elemento compresso non valido"
                }
                require(entryBytes <= compressedBytes * MAX_XLSX_EXPANSION_RATIO) {
                    "Il file XLSX supera il rapporto massimo di compressione"
                }
            }
            zip.closeEntry()
        }
    }

    require(entryCount > 0 && hasContentTypes && hasWorkbook) {
        "Il file selezionato non contiene una cartella di lavoro XLSX valida"
    }
}

internal class ByteLimitInputStream(
    input: InputStream,
    private val maxBytes: Long
) : FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) recordBytes(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) recordBytes(count)
        return count
    }

    private fun recordBytes(count: Int) {
        bytesRead += count
        require(bytesRead <= maxBytes) {
            "Il file XLSX supera 10 MB"
        }
    }
}
