package it.agesci.scoutalert.excel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelRowParserTest {
    @Test
    fun `classifies blank row without counting an error`() {
        assertTrue(parseRowValues("", "", "", "") is RowParseResult.Blank)
    }

    @Test
    fun `skips corrupt date row`() {
        assertTrue(
            parseRowValues("Rossi", "Mario", "31/02/2020", "E/G") is
                RowParseResult.Invalid
        )
    }

    @Test
    fun `normalizes and imports valid row`() {
        val result = parseRowValues("  ROSSI ", " mario ", "16/11/2020", "E/G")

        assertTrue(result is RowParseResult.Valid)
        assertEquals(
            listOf("Rossi", "Mario", "2020-11-16", "E/G"),
            (result as RowParseResult.Valid).row.cells
        )
    }
}
