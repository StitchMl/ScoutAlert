package it.agesci.scoutalert.excel

import android.content.Context
import android.net.Uri
import android.util.Log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private const val TAG = "ExcelParser"

private fun normalizeName(raw: String): String {
    return raw
        .trim()
        .lowercase()
        .split(Regex("\\s+"))
        .joinToString(" ") { part ->
            part.replaceFirstChar { c ->
                if (c.isLetter()) c.titlecase(Locale.ROOT) else c.toString()
            }
        }
}

// ---------------------------------------------------
// EXCEL CELL DATA CONVERSION
// ---------------------------------------------------
private fun parseExcelDate(cell: Cell): LocalDate? {
    return try {
        if (DateUtil.isCellDateFormatted(cell)) {
            cell.dateCellValue.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } else if (cell.cellType == CellType.NUMERIC) {
            val javaDate = DateUtil.getJavaDate(cell.numericCellValue)
            javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun parseStringDate(text: String): LocalDate? {
    val formats = listOf(
        "dd/MM/yyyy", "d/M/yyyy",
        "yyyy-MM-dd", "dd-MM-yyyy",
        "dd.MM.yyyy"
    )
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.getDefault())
            val date = sdf.parse(text) ?: continue
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (_: Exception) { }
    }
    return null
}


// ---------------------------------------------------
// CELL FORMATION
// ---------------------------------------------------
private fun getCellValue(
    cell: Cell,
    evaluator: FormulaEvaluator,
    formatter: DataFormatter
): String {
    return try {
        formatter.formatCellValue(cell, evaluator).trim()
    } catch (_: Exception) {
        Log.w(TAG, "Cell error ${cell.rowIndex},${cell.columnIndex}")
        ""
    }
}


// ---------------------------------------------------
// MAIN PARSER
// ---------------------------------------------------
fun parseExcelFromUri(context: Context, uri: Uri): ExcelTable? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->

            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            val evaluator = workbook.creationHelper.createFormulaEvaluator()
            val formatter = DataFormatter()

            val rowIterator = sheet.iterator()
            if (!rowIterator.hasNext()) {
                workbook.close()
                return null
            }

            // 1) HEADER (first line)
            val headerRow = rowIterator.next()
            val headers = headerRow.map { cell ->
                getCellValue(cell, evaluator, formatter)
            }
            Log.d(TAG, "Original headers: ${headers.joinToString(" | ")}")

            val headerPositions = detectHeaderColumns(headers)

            Log.d(TAG, "---- HEADER POSITIONS (NEW SMART MATCH) ----")
            headerPositions.forEach { (key, idx) ->
                Log.d(TAG, "$key -> index=$idx header='${headers[idx]}'")
            }
            Log.d(TAG, "------------------------------------------------")


            // ---- 2) BETWEEN THE LINES ----
            val rows = mutableListOf<ExcelRow>()

            while (rowIterator.hasNext()) {
                val row = rowIterator.next()

                fun getField(key: String): String {
                    val idx = headerPositions[key] ?: return ""
                    val cell = row.getCell(idx) ?: return ""
                    return getCellValue(cell, evaluator, formatter)
                }

                val cognome = normalizeName(getField("cognome"))
                val nome = normalizeName(getField("nome"))

                var birthDate: LocalDate? = null
                headerPositions["datanascita"]?.let { idx ->
                    val cell = row.getCell(idx)
                    if (cell != null) {
                        birthDate = parseExcelDate(cell)
                            ?: parseStringDate(getCellValue(cell, evaluator, formatter))
                    }
                }

                val unita = getField("unita")

                // skip blank lines
                if (cognome.isBlank() && nome.isBlank() && birthDate == null) continue

                rows.add(
                    ExcelRow(
                        listOf(
                            cognome,
                            nome,
                            birthDate?.toString() ?: "",
                            unita
                        )
                    )
                )
                Log.d(TAG, "Parsed row: cognome='$cognome' nome='$nome' unit='$unita' birth=$birthDate")
            }

            workbook.close()
            Log.d(TAG, "Final lines parsed: ${rows.size}")

            // Removes duplicates of last name+first name+date
            val distinct = rows.distinctBy { it.cells.joinToString("|") }

            ExcelTable(
                headers = listOf("Cognome", "Nome", "Data", "Unità"),
                rows = distinct
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Excel parsing error", e)
        null
    }
}

// ---------------------------------------------------
// NEW HEADER DETECTION (fixes "nome" inside "cognome")
// ---------------------------------------------------
private fun detectHeaderColumns(headers: List<String>): Map<String, Int> {
    fun normalizeForWords(h: String): List<String> =
        h.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

    val wordsList = headers.map { normalizeForWords(it) }

    fun findExactWord(word: String): Int? {
        wordsList.forEachIndexed { idx, words ->
            if (word in words) return idx
        }
        return null
    }

    fun findColumn(vararg candidates: String): Int? {
        for (candidate in candidates) {
            val idx = findExactWord(candidate)
            if (idx != null) return idx
        }
        return null
    }

    val map = mutableMapOf<String, Int>()

    map["cognome"] = findColumn("cognome") as Int
    map["nome"] = findColumn("nome") as Int  // now safe: never matches inside cognome
    map["datanascita"] = findColumn("nascita", "data") as Int
    map["unita"] = findColumn("unita", "unità", "branca", "reparto") as Int

    return map.filterValues { true }.mapValues { it.value }
}