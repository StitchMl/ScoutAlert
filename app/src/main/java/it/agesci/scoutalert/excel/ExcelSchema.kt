package it.agesci.scoutalert.excel

import java.text.Normalizer

internal data class ExcelColumns(
    val lastName: Int?,
    val firstName: Int?,
    val fullName: Int?,
    val birthDate: Int,
    val unit: Int?
)

internal data class ExcelHeader(
    val rowIndex: Int,
    val columns: ExcelColumns
)

internal fun detectExcelHeader(rows: List<List<String>>): ExcelHeader {
    rows.take(10).forEachIndexed { index, headers ->
        runCatching { detectExcelColumns(headers) }
            .getOrNull()
            ?.let { return ExcelHeader(index, it) }
    }
    throw IllegalArgumentException("Intestazione Excel non trovata nelle prime 10 righe")
}

internal fun detectExcelColumns(headers: List<String>): ExcelColumns {
    val wordsByColumn = headers.map { header ->
        Normalizer.normalize(header, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
    }

    fun find(vararg candidates: String): Int? = wordsByColumn
        .withIndex()
        .filter { (_, words) ->
            words.none { it in PARENT_WORDS } && candidates.any(words::contains)
        }
        .maxWithOrNull(
            compareBy<IndexedValue<List<String>>> { (_, words) ->
                words.count { it in SCOUT_WORDS }
            }.thenByDescending { it.index }
        )
        ?.index

    val lastName = find("cognome")
    val firstName = find("nome")
    val fullName = find("ragazzo", "nominativo", "scout", "iscritto")
        ?.takeIf { lastName == null || firstName == null }
    val hasNames = (lastName != null && firstName != null) || fullName != null

    val missing = buildList {
        if (!hasNames) add("nome/cognome o nominativo")
        if (find("nascita", "data") == null) add("data di nascita")
    }
    require(missing.isEmpty()) {
        "Colonne obbligatorie mancanti: ${missing.joinToString()}"
    }

    return ExcelColumns(
        lastName = lastName,
        firstName = firstName,
        fullName = fullName,
        birthDate = requireNotNull(find("nascita", "data")),
        unit = find("unita", "branca", "reparto")
    )
}

private val PARENT_WORDS = setOf("genitore", "madre", "padre")
private val SCOUT_WORDS = setOf("censito", "ragazzo")
