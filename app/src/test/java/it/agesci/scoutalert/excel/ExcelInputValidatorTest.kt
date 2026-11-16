package it.agesci.scoutalert.excel

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertThrows
import org.junit.Test

class ExcelInputValidatorTest {
    @Test
    fun `accepts minimal OOXML archive`() {
        validateXlsxArchive(xlsxArchive())
    }

    @Test
    fun `rejects excessive expansion ratio`() {
        val bomb = xlsxArchive("xl/sharedStrings.xml" to ByteArray(1_000_000))

        assertThrows(IllegalArgumentException::class.java) {
            validateXlsxArchive(bomb)
        }
    }

    @Test
    fun `rejects excessive entry count`() {
        val entries = Array(MAX_XLSX_ENTRIES) { index ->
            "xl/worksheets/sheet$index.xml" to byteArrayOf(1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            validateXlsxArchive(xlsxArchive(*entries))
        }
    }

    @Test
    fun `compressed stream limit rejects bytes beyond cap`() {
        val input = ByteLimitInputStream(ByteArrayInputStream(ByteArray(5)), 4)

        assertThrows(IllegalArgumentException::class.java) {
            input.readBytes()
        }
    }

    private fun xlsxArchive(vararg extraEntries: Pair<String, ByteArray>): ByteArrayInputStream {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            sequenceOf(
                "[Content_Types].xml" to "<Types/>".toByteArray(),
                "xl/workbook.xml" to "<workbook/>".toByteArray()
            ).plus(extraEntries.asSequence()).forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(output.toByteArray())
    }
}
