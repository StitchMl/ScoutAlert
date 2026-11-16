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
        assertNull(columns.fullName)
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
            "Colonne obbligatorie mancanti: nome/cognome o nominativo, data di nascita",
            error.message
        )
    }

    @Test
    fun `finds scout headers after banner and ignores parent columns`() {
        val header = detectExcelHeader(
            listOf(
                listOf("Elenco Genitori Branca E/G"),
                emptyList(),
                listOf(
                    "Nome del genitore",
                    "Nascita del genitore",
                    "Cognome del censito",
                    "Nome del censito",
                    "Nascita del censito",
                    "Branca"
                )
            )
        )

        assertEquals(2, header.rowIndex)
        assertEquals(2, header.columns.lastName)
        assertEquals(3, header.columns.firstName)
        assertEquals(4, header.columns.birthDate)
        assertEquals(5, header.columns.unit)
    }

    @Test
    fun `supports one combined scout name column`() {
        val columns = detectExcelColumns(
            listOf("Ragazzo", "Data di nascita", "Unità")
        )

        assertNull(columns.firstName)
        assertNull(columns.lastName)
        assertEquals(0, columns.fullName)
        assertEquals("Mario" to "Rossi Bianchi", splitFullName("Mario\tRossi Bianchi"))
    }
}
