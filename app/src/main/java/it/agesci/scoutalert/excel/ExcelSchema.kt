package it.agesci.scoutalert.excel

import java.text.Normalizer

internal data class ExcelColumns(
    val lastName: Int,
    val firstName: Int,
    val birthDate: Int,
    val unit: Int?
)

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

    fun find(vararg candidates: String): Int? =
        wordsByColumn.indexOfFirst { words -> candidates.any(words::contains) }
            .takeIf { it >= 0 }

    val missing = buildList {
        if (find("cognome") == null) add("cognome")
        if (find("nome") == null) add("nome")
        if (find("nascita", "data") == null) add("data di nascita")
    }
    require(missing.isEmpty()) {
        "Colonne obbligatorie mancanti: ${missing.joinToString()}"
    }

    return ExcelColumns(
        lastName = requireNotNull(find("cognome")),
        firstName = requireNotNull(find("nome")),
        birthDate = requireNotNull(find("nascita", "data")),
        unit = find("unita", "branca", "reparto")
    )
}
