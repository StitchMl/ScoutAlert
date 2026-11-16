package it.agesci.scoutalert.excel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ExcelSchemaTest {
    @Test
    fun `detects required columns without confusing nome and cognome`() {
        val columns = detectExcelColumns(
            listOf("Cognome", "Nome", "Data di nascita", "Unità")
        )

        assertEquals(0, columns.lastName)
        assertEquals(1, columns.firstName)
        assertEquals(2, columns.birthDate)
        assertEquals(3, columns.unit)
    }

    @Test
    fun `accepts accented unit aliases`() {
        val columns = detectExcelColumns(
            listOf("Nome", "Cognome", "Nascita", "Unità scout")
        )

        assertEquals(3, columns.unit)
    }

    @Test
    fun `keeps unit optional`() {
        val columns = detectExcelColumns(listOf("Nome", "Cognome", "Data"))

        assertNull(columns.unit)
    }

    @Test
    fun `rejects missing required columns`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            detectExcelColumns(listOf("Nome", "Unità"))
        }

        assertEquals(
            "Colonne obbligatorie mancanti: cognome, data di nascita",
            error.message
        )
    }
}
